#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/proc_fs.h>
#include <linux/string.h>
#include <linux/vmalloc.h>
#include <asm/uaccess.h>
#include <linux/seq_file.h>
#include <linux/usb.h>

MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("USB controller for mouse");
MODULE_AUTHOR("Kislenko Maksim");

#define MY_DEVICE_NAME    "my_usb_device"
#define PRODUCT_ID 0x1
#define VENDOR_ID 0x1234

static struct usb_device_id my_table [] = {
    { USB_DEVICE(VENDOR_ID, PRODUCT_ID) },
    {}
};

static int my_probe(struct usb_interface *interface, const struct usb_device_id *id) {
    // ...
}

static void my_disconnect(struct usb_interface *interface) {
    // ...
}

static struct usb_driver my_driver = {
  .name = MY_DEVICE_NAME,
  .probe = my_probe,
  .disconnect = my_disconnect,
  .id_table = my_table,
};

static struct proc_dir_entry *proc_entry;

static const struct file_operations proc_file_fops = {
    .owner = THIS_MODULE,
    .open = single_open,
    .read = seq_read,
    .llseek = seq_lseek,
    .release = single_release,
};

int init_usb_module(void) {
    int ret = 0;

    proc_entry = proc_create("usb_module", 0644, NULL, &proc_file_fops);
    if (proc_entry == NULL) {
        printk(KERN_INFO "usb_module: Couldn't create proc entry\n");
        return -ENOMEM;
    }

    ret = usb_register(&my_driver);
    if (ret != 0) {
        printk(KERN_INFO "usb_module: Couldn't register usb driver\n");
        return -ENOMEM;
    }
    return ret;
}

void cleanup_usb_module(void) {
    remove_proc_entry("usb_module", proc_entry);
    usb_deregister(&my_driver);
    printk(KERN_INFO "usb_module: Module cleaned up.\n");
}

module_init(init_usb_module);
module_exit(cleanup_usb_module);