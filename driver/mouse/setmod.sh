echo 'Embedding LKM'
make
insmod ./mouse_module.ko

echo '>>> /proc/modules (first 10)'
head -n 10 /proc/modules