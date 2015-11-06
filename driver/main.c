#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>

int main(void) {
	int fd;
	ssize_t count;
	char buf[50];

	/* open device */
	fd = open("/dev/phone_1", O_RDWR|O_TRUNC, 0644);
	if (fd < 0) {
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