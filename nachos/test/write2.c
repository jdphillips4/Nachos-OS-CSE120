/*
 * write2.c
 *
 * Can I write from one file to the next?
 *
 * Geoff Voelker
 * 11/9/15
 */

// writing a large amount of bytes
#include "syscall.h"

int
main (int argc, char *argv[])
{
	char *readname = "read2.txt";
    int read_fd = open(readname);
    if (read_fd == -1) {
        printf("Failed to open read file\n");
        return -1;
    }

	char *writename = "write2.txt";
	int write_fd = creat(writename);
	if (write_fd == -1) {
        printf("Failed to create write file\n");
		return -1;
	}

	// what is the point if I am limited by buf size in user process anyway...
	//int file_len = 62571;
	int file_len = 5000;
    char buf[file_len + 1];
    int bytesRead = read(read_fd, buf, file_len);
	printf("bytesRead: %d\n", bytesRead);
	if (bytesRead == -1) return -1;

	int bytesWrote = write(write_fd, buf, file_len);
	printf("bytesWrote: %d\n", bytesWrote);
	if (bytesWrote == -1) return -1;
    return 0;
}
