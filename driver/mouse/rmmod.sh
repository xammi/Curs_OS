echo 'Removing LKM'
rmmod mouse_module

echo '>>> /proc/modules (first 10)'
head -n 10 /proc/modules