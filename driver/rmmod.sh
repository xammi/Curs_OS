echo 'Removing LKM'
rmmod fortune-lkm

echo '>>> /proc/modules (first 10)'
head -n 10 /proc/modules