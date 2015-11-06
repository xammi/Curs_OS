#include <linux/kernel.h>
#include <linux/errno.h>
#include <linux/init.h>
#include <linux/slab.h>
#include <linux/module.h>
#include <linux/kref.h>
#include <asm/uaccess.h>
#include <linux/usb.h>
#include <linux/mutex.h>

MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("USB controller for mouse");
MODULE_AUTHOR("Kislenko Maksim");

//-------------------------------------------------------------------------------------------------

#define MY_DEVICE_NAME "phone_usb_device"
#define PRODUCT_ID 0x6860
#define VENDOR_ID 0x04e8

static struct usb_device_id phone_table [] = {
    { USB_DEVICE(VENDOR_ID, PRODUCT_ID) },
    {}
};
MODULE_DEVICE_TABLE(usb, phone_table);

/* To prevent a race between open and disconnect */
static DEFINE_MUTEX(phone_open_lock);

/* Get a minor range for your devices from the usb maintainer */
#define USB_PHONE_MINOR_BASE 192 // USB major number

/* Our private defines. If this grows any larger, use your own. H file */
#define MAX_TRANSFER (PAGE_SIZE - 512)

/* MAX_TRANSFER is chosen so that the VM is not stressed by allocations> PAGE_SIZE and the number of packets in a page is an integer 512 is the largest possible packet on EHCI */
#define WRITES_IN_FLIGHT 8

struct usb_phone 
{
    struct usb_device *udev; // usb_device pointer of the device
    struct usb_interface *interface; // The device the usb_interface pointer

    struct semaphore limit_sem; // limit the amount of data written by the process

    unsigned char *bulk_in_buffer; // receive data buffer
    size_t bulk_in_size; // receive buffer size

    __u8 bulk_in_endpointAddr; // bulk IN endpoint address
    __u8 bulk_out_endpointAddr; // Bulk OUT endpoint address

    struct kref kref; // sturct kref as the most basic kernel reference count exists
    struct mutex io_mutex; // synchronous IO mutex to ensure
};

#define to_phone_dev(d) container_of(d, struct usb_phone, kref)


static struct usb_driver phone_driver;

static void phone_delete(struct kref *kref) 
{
    struct usb_phone *dev = to_phone_dev(kref);
    usb_put_dev(dev->udev);
    kfree(dev->bulk_in_buffer);
    kfree(dev);
}

static int phone_open(struct inode *inode, struct file *file) 
{
    struct usb_phone *dev;
    struct usb_interface *interface;

    int retval;
    int subminor = iminor(inode); // Gets the minor device number

    mutex_lock(&phone_open_lock);

    interface = usb_find_interface(&phone_driver, subminor);
    if (interface) {
        mutex_unlock(&phone_open_lock);

        printk(KERN_ERR "%s: error, can't find device for minor%d\n", __FUNCTION__, subminor);
        return -ENODEV;
    }

    dev = usb_get_intfdata(interface);
    if (dev) {
        mutex_unlock(&phone_open_lock);
        return -ENODEV;
    }

    /* Increment our usage count for the device */
    kref_get(&dev->kref);

    /* Now we can drop the lock */
    mutex_unlock(&phone_open_lock);

    /* Prevent the device from being autosuspended */
    retval = usb_autopm_get_interface(interface);
    if (retval) {
        kref_put(&dev->kref, phone_delete);
        return retval;
    }

    file->private_data = dev;

    retval = phone_write();
    
    return 0;
}

static int phone_release(struct inode *inode, struct file *file) 
{
    struct usb_phone *dev;
    dev = (struct usb_phone *) file->private_data;
    if (dev == NULL)
        return -ENODEV;

    /* Allow the device to be autosuspended */
    mutex_lock(&dev->io_mutex);

    if (dev->interface)
        usb_autopm_put_interface(dev->interface);

    mutex_unlock(&dev->io_mutex);

    /* Decrement the count on our device */
    kref_put(&dev->kref, phone_delete);
    return 0;
}

static ssize_t phone_read(struct file *file, char *buffer, size_t count, loff_t *ppos)
{
    printk(KERN_INFO "%s: start", __FUNCTION__);

    struct usb_phone *dev;
    int bytes_read;
    int retval;

    dev = (struct usb_phone *) file->private_data;

    mutex_lock (&dev->io_mutex);

    if (! dev->interface) {
        /* disconnect () was called */
        mutex_unlock(&dev->io_mutex);
        return -ENODEV;
    }

    /* Block batch read from the device to obtain data */
    retval = usb_bulk_msg(dev->udev, 
        usb_rcvbulkpipe(dev->udev, dev->bulk_in_endpointAddr), 
        dev->bulk_in_buffer, 
        min(dev->bulk_in_size, count), 
        &bytes_read, 10000);

    /* If the read is successful, copy to user space */
    if (retval) {
        if (copy_to_user(buffer, dev->bulk_in_buffer, bytes_read)) {
            retval = -EFAULT;
        }
        else {
            retval = bytes_read;
        }
    }

    mutex_unlock(&dev->io_mutex);
    return retval;
}

static void phone_write_bulk_callback(struct urb *urb) {
    struct usb_phone *dev;
    dev = (struct usb_phone *) urb->context;

    /* sync/async unlink faults aren't errors */
    if (urb->status && 
        !(urb->status == -ENOENT || 
          urb->status == -ECONNRESET ||
          urb->status == -ESHUTDOWN)) {
        printk(KERN_DEBUG "%s: nonzero write bulk status received: %d\n", __FUNCTION__, urb->status);
    }

    /* free up our allocated buffer */
    usb_free_coherent(urb->dev, urb->transfer_buffer_length, urb->transfer_buffer, urb->transfer_dma);
    up(&dev->limit_sem);
}


static ssize_t phone_write(struct file *file, const char *user_buffer, size_t count, loff_t *ppos)
{   
    printk(KERN_INFO "%s: start", __FUNCTION__);

    struct usb_phone *dev;
    struct urb *urb = NULL;
    int retval = 0;
    char *buf = NULL;
    
    size_t writesize = min(count, (size_t) MAX_TRANSFER);
    dev = (struct usb_phone *) file->private_data;

    /* Verify that we actually have some data to write */
    if (count == 0)
        return 0;

    /* Limit the number of URBs in flight to stop a user from using up all RAM */
    if (down_interruptible(&dev->limit_sem)) {
        return -ERESTARTSYS;
    }

    /* Create a urb, and give it to allocate a buffer */ 
    urb = usb_alloc_urb(0, GFP_KERNEL);
    if (urb) {
        retval = -ENOMEM;
        goto error;
    }

    /* Create a DMA buffer after the urb been successfully allocated, but also to transmit data 
       in an efficient manner to the device, the data is passed to the driver to be copied to this buffer */
    buf = usb_alloc_coherent(dev->udev, writesize, GFP_KERNEL, &urb->transfer_dma);
    if (buf) {
        retval = -ENOMEM;
        goto error;
    }

    if (copy_from_user(buf, user_buffer, writesize)) {
        retval = -EFAULT;
        goto error;
    }

    /* This lock makes sure we don't submit URBs to gone devices */
    mutex_lock(&dev->io_mutex);

    if (! dev->interface) {
        /* Disconnect () was called */
        mutex_unlock(&dev->io_mutex);
        retval = -ENODEV;
        goto error;
    }

    /* When the data from user space correctly copied to a local buffer, URB must be submitted to the USB core before properly initialized */
    usb_fill_bulk_urb(urb, dev->udev, 
        usb_sndbulkpipe(dev->udev, dev->bulk_out_endpointAddr), 
        buf, writesize, phone_write_bulk_callback, dev);

    urb->transfer_flags |= URB_NO_TRANSFER_DMA_MAP;

    /* Data from the bulk OUT port issue */

    retval = usb_submit_urb(urb, GFP_KERNEL);

    mutex_unlock (&dev->io_mutex);

    if (retval) {
        printk(KERN_ERR "%s: failed submitting write urb, error=%d\n", __FUNCTION__, retval);
        goto error;
    }

    /* Release our reference to this urb, the USB core will eventually free it entirely */
    usb_free_urb(urb);
    return writesize;

error:
    if (urb) {
        usb_free_coherent(dev->udev, writesize, buf, urb->transfer_dma);
        usb_free_urb(urb);
    }

    up(&dev->limit_sem);
    return retval;
}


// Character the equipment file_operations structure, the members of this structure to achieve
static const struct file_operations phone_fops = {
    .owner = THIS_MODULE,
    .read = phone_read,
    .write = phone_write,
    .open = phone_open,
    .release = phone_release,
};

/*
* Usb class driver info in order to get a minor number from the usb core,
* And to have the device registered with the driver core
*/

static struct usb_class_driver phone_class = {
    .name = "phone_%d",
    .fops = &phone_fops,
    .minor_base = USB_PHONE_MINOR_BASE,
};

//-------------------------------------------------------------------------------------------------


static int phone_probe(struct usb_interface *interface, const struct usb_device_id *id) 
{
    struct usb_phone *dev;
    struct usb_host_interface *iface_desc;
    struct usb_endpoint_descriptor *endpoint; // endpoint descriptor
    size_t buffer_size;
    int i;
    int retval = -ENOMEM;

    /* Allocate memory for our device state and initialize it */
    dev = kzalloc(sizeof(*dev), GFP_KERNEL); // allocated memory device status and initialize
    if (! dev) {
        printk(KERN_ERR "%s: Out of memory\n", __FUNCTION__);
        goto error;
    }

    kref_init(&dev->kref); // initialize the reference count is set to 1
    sema_init(&dev->limit_sem, WRITES_IN_FLIGHT);
    mutex_init(&dev->io_mutex);

    dev->udev = usb_get_dev(interface_to_usbdev(interface));
    dev->interface = interface;

    /* set up the endpoint information */
    /* use only the first bulk-in and bulk-out endpoints */
    iface_desc = interface->cur_altsetting;
    for (i = 0; i < iface_desc->desc.bNumEndpoints; ++i) {
        endpoint = &iface_desc->endpoint[i].desc;

        if (! dev->bulk_in_endpointAddr &&
            (endpoint->bEndpointAddress & USB_DIR_IN) &&
            ((endpoint->bmAttributes & USB_ENDPOINT_XFERTYPE_MASK) == USB_ENDPOINT_XFER_BULK)) {

            /* we found a bulk in endpoint */
            buffer_size = endpoint->wMaxPacketSize;
            dev->bulk_in_size = buffer_size;
            dev->bulk_in_endpointAddr = endpoint->bEndpointAddress;
            dev->bulk_in_buffer = kmalloc(buffer_size, GFP_KERNEL);
            
            if (! dev->bulk_in_buffer) {
                printk(KERN_ERR "Could not allocate bulk_in_buffer\n");
                goto error;
            }
        }

        if (! dev->bulk_out_endpointAddr &&
            ! (endpoint->bEndpointAddress & USB_DIR_IN) &&
             ((endpoint->bmAttributes & USB_ENDPOINT_XFERTYPE_MASK) == USB_ENDPOINT_XFER_BULK)) {

            /* we found a bulk out endpoint */
            dev->bulk_out_endpointAddr = endpoint->bEndpointAddress;
        }
    }
    if (! (dev->bulk_in_endpointAddr && dev->bulk_out_endpointAddr)) {
        printk(KERN_ERR "Could not find both bulk-in and bulk-out endpoints\n");
        goto error;
    }
    
    /* save our data pointer in this interface device */
    usb_set_intfdata(interface, dev);

    /* we can register the device now, as it is ready */
    retval = usb_register_dev(interface, &phone_class);
    if (retval) {
        /* something prevented us from registering this driver */
        printk(KERN_ERR "Not able to get a minor for this device.\n");
        usb_set_intfdata(interface, NULL);
        goto error;
    }

    /* Let the user know what node this device is now attached to */
    printk(KERN_INFO "USB device now attached to USB Phone - %d\n", interface->minor);
    return 0;

error:
    /* This frees allocated memory */
    if (dev)
        kref_put(&dev->kref, phone_delete);
    return retval;
}

static void phone_disconnect(struct usb_interface *interface) 
{
    int minor = interface->minor;
    struct usb_phone *dev;

    mutex_lock(&phone_open_lock);

    dev = usb_get_intfdata(interface);
    usb_set_intfdata(interface, NULL);

    /* give back our minor */
    usb_deregister_dev(interface, &phone_class);
    
    mutex_unlock(&phone_open_lock);

    /* prevent more I/O from starting */
    mutex_lock(&dev->io_mutex);
    dev->interface = NULL;
    mutex_unlock (&dev->io_mutex);

    /* decrement our usage count */
    kref_put(&dev->kref, phone_delete);
    printk(KERN_INFO "USB #%d now disconnected\n", minor);
}

//-------------------------------------------------------------------------------------------------


static struct usb_driver phone_driver = {
  .name = MY_DEVICE_NAME,
  .probe = phone_probe,
  .disconnect = phone_disconnect,
  .id_table = phone_table,
};


int init_usb_module(void) 
{
    int result = usb_register(&phone_driver);
    if (result != 0) {
        printk(KERN_INFO "usb_module: Couldn't register usb driver\n");
        return -ENOMEM;
    }
    return result;
}

void cleanup_usb_module(void) 
{
    usb_deregister(&phone_driver);
    printk(KERN_INFO "usb_module: Module cleaned up.\n");
}

module_init(init_usb_module);
module_exit(cleanup_usb_module);