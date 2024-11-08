/*
 * read1.c
 * read a specified file and write it to stdout
 */

#include "syscall.h"

int main(int argc, char *argv[]) {
    char *filename = "read1.txt";
    int fd = open(filename);
    if (fd == -1) {
        printf("Failed to open file\n");
        return -1;
    }

    char buf[120];
    int bytesRead = read(fd, buf, 100);
	printf("bytesRead: %d\n", bytesRead);

    buf[bytesRead] = '\0';
    printf("buffer: %s\n", buf);

    return 0;
}
