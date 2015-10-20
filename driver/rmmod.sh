echo 'Removing LKM'
rmmod usb_module

echo '>>> /proc/modules (first 10)'
head -n 10 /proc/modules