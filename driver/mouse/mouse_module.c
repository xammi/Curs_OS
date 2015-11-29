#include <linux/kernel.h>
#include <linux/slab.h>
#include <linux/module.h>
#include <asm/uaccess.h>
#include <linux/errno.h>

#include <linux/fs.h>
#include <linux/pci.h>
#include <linux/input.h>
#include <linux/platform_device.h>
#include <linux/usb.h>


MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("Virtual mouse driver");
MODULE_AUTHOR("Kislenko Maksim");

#define DEVICE_NAME "virtual_mouse"

#define MOUSE_BTN_LEFT 1
#define MOUSE_BTN_RIGHT 2
#define MOUSE_BTN_MIDDLE 3
#define MOUSE_BTN_EXTRA 4

#define MOUSE_MOVE 5
#define MOUSE_SCROLL 6

#define MOUSE_KEY_PRESSED 8
#define MOUSE_KEY_RELEASED 0

//-------------------------------------------------------------------------------------------------
// Representation of an input device
struct input_dev *vm_input_dev; 

// Device structure
static struct platform_device *vm_dev;

static ssize_t write_vm(struct device *dev, 
                        struct device_attribute *attr, 
                        const char *buffer, 
                        size_t count)
{
    int x = 0, y = 0;
    int command = 0;
    sscanf(buffer, "%d:%d,%d", &command, &x, &y);
    
    printk(KERN_INFO "write %d, %d, %d\n", command, x, y);

    if (command == MOUSE_MOVE) {
        input_report_abs(vm_input_dev, ABS_X, x);
        input_report_abs(vm_input_dev, ABS_Y, y);
    }
    else if (command == MOUSE_SCROLL) {
        input_report_rel(vm_input_dev, REL_X, x);
        input_report_rel(vm_input_dev, REL_Y, y);
    }
    else if (command == MOUSE_BTN_LEFT) {
        input_report_key(vm_input_dev, BTN_LEFT, MOUSE_KEY_PRESSED);
        input_report_key(vm_input_dev, BTN_LEFT, MOUSE_KEY_RELEASED);
    }
    else if (command == MOUSE_BTN_RIGHT) {
        input_report_key(vm_input_dev, BTN_RIGHT, MOUSE_KEY_PRESSED);
        input_report_key(vm_input_dev, BTN_RIGHT, MOUSE_KEY_RELEASED);
    }
    else if (command == MOUSE_BTN_MIDDLE) {
        input_report_key(vm_input_dev, BTN_MIDDLE, MOUSE_KEY_PRESSED);
        input_report_key(vm_input_dev, BTN_MIDDLE, MOUSE_KEY_RELEASED);
    }
    else if (command == MOUSE_BTN_EXTRA) {
        input_report_key(vm_input_dev, BTN_EXTRA, MOUSE_KEY_PRESSED);
        input_report_key(vm_input_dev, BTN_EXTRA, MOUSE_KEY_RELEASED);
    }  

    input_sync(vm_input_dev);
    return count;
}

// Attach the sysfs write method
DEVICE_ATTR(coordinates, 0644, NULL, write_vm);

// Attribute Descriptor
static struct attribute *vm_attrs[] = {
    &dev_attr_coordinates.attr,
    NULL
};

// Attribute group
static struct attribute_group vm_attr_group = {
    .attrs = vm_attrs,
};

//-------------------------------------------------------------------------------------------------

static int __init init_mouse_module(void) {
    int retval = 0;
    
    // Register a platform device
    vm_dev = platform_device_register_simple(DEVICE_NAME, -1, NULL, 0);
    if (IS_ERR(vm_dev)) {
        retval = PTR_ERR(vm_dev);
        printk(KERN_ERR "%s: register error\n", __FUNCTION__);
        return retval;
    }

    // Create a sysfs node to read simulated coordinates
    retval = sysfs_create_group(&vm_dev->dev.kobj, &vm_attr_group);
    if (retval) {
        printk(KERN_ERR "%s: sysfs_create_group() error\n", __FUNCTION__);
        return -ENOMEM;
    }
  
    // Allocate an input device data structure
    vm_input_dev = input_allocate_device();
    if (! vm_input_dev) {
        printk(KERN_ERR "%s: input_alloc_device() error\n", __FUNCTION__);
        return -ENOMEM;
    }

    set_bit(EV_ABS, vm_input_dev->evbit);
    set_bit(ABS_X, vm_input_dev->absbit);
    set_bit(ABS_Y, vm_input_dev->absbit);

    set_bit(EV_KEY, vm_input_dev->evbit);
    set_bit(BTN_LEFT, vm_input_dev->keybit);
    set_bit(BTN_RIGHT, vm_input_dev->keybit);

    vm_input_dev->evbit[0] = BIT_MASK(EV_KEY) | BIT_MASK(EV_ABS) | BIT_MASK(EV_REL);
    vm_input_dev->keybit[BIT_WORD(BTN_MOUSE)] = BIT_MASK(BTN_LEFT) | BIT_MASK(BTN_RIGHT) | BIT_MASK(BTN_MIDDLE) | BIT_MASK(BTN_EXTRA);
    vm_input_dev->relbit[0] = BIT_MASK(REL_X) | BIT_MASK(REL_Y);
    vm_input_dev->absbit[0] = BIT_MASK(ABS_X) | BIT_MASK(ABS_Y);

    input_set_abs_params(vm_input_dev, ABS_X, 0, 0, 0, 0);
    input_set_abs_params(vm_input_dev, ABS_Y, 0, 0, 0, 0);
  
    // Register with the input subsystem
    retval = input_register_device(vm_input_dev);
    if (retval) {
        input_free_device(vm_input_dev);
        printk(KERN_ERR "%s: input_register_device() error\n", __FUNCTION__);
        return retval;
    }

    printk(KERN_INFO "Virtual mouse driver initialized.\n");
    return 0;
}

static void cleanup_mouse_module(void) {
    // Unregister from the input subsystem
    input_unregister_device(vm_input_dev);

    // Cleanup sysfs node
    sysfs_remove_group(&vm_dev->dev.kobj, &vm_attr_group);

    // Unregister driver
    platform_device_unregister(vm_dev);
}

module_init(init_mouse_module);
module_exit(cleanup_mouse_module);