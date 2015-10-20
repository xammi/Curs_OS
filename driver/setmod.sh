echo 'Embedding LKM'
make
insmod ./usb_module.ko

echo '>>> /proc/modules (first 10)'
head -n 10 /proc/modules