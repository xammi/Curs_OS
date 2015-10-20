echo 'Embedding LKM'
make
insmod ./fortune-lkm.ko

echo '>>> /proc/modules (first 10)'
head -n 10 /proc/modules