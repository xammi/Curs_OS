#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>

#define filename "usb-SAMSUNG_SAMSUNG_Android_4dfc1f0c037250fb-if01"

int main(void) {
	int fd;
	ssize_t count;
	char buf[50];

	/* open device */
	if ((fd = open("/dev/serial/by-id/" filename, O_RDWR)) < 0) {
		perror("open()");
		return 1;
	}

	/* write to device */
	memset(buf, 0x00, sizeof(buf));   /* clear buffer */
	strcpy(buf, "Hello World!");
	count = write(fd, buf, sizeof(buf));
	printf("Written %d bytes to device\n", (int) count);

	/* read from device */
	memset(buf, 0x00, sizeof(buf));   /* clear buffer */
	count = read(fd, buf, sizeof(buf));
	printf("Read %d bytes from device: %s\n", (int) count, buf);

	/* close device */
	close(fd);
	return 0;
}