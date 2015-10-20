#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/proc_fs.h>
#include <linux/string.h>
#include <linux/vmalloc.h>
#include <asm/uaccess.h>
#include <linux/seq_file.h>

MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("USB controller for mouse");
MODULE_AUTHOR("Kislenko Maksim");

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
        ret = -ENOMEM;
        printk(KERN_INFO "usb_module: Couldn't create proc entry\n");
    }
    else {
        printk(KERN_INFO "usb_module: Module loaded.\n");
    }
    return ret;
}

void cleanup_usb_module(void) {
    remove_proc_entry("usb_module", proc_entry);
    printk(KERN_INFO "usb_module: Module cleaned up.\n");
}

module_init(init_usb_module);
module_exit(cleanup_usb_module);