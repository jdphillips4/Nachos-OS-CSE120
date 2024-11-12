/*
 * creat.c
 * test creat syscall
 */

#include "syscall.h"

int main(int argc, char *argv[]) {
    // creats an existing file and returns a file descriptor for it
    char *filename = "creat.txt";
    int fd1 = creat(filename);
    if(fd1 == -1) {
        printf("error in good creat");
        exit(-1);
    }
    printf("passed test 1!\n");
    //creating the same file multiple times returns different file descriptors for each creat;
    int fd2 = creat(filename);
    if(fd2 == -1 || fd2 == fd1) {
        printf("error in creat same file multiple times");
        exit(-3);
    }
    printf("passed test 3!\n");
    //each process can use 16 file descriptors.
    char *filename2 = "creat2.txt";
    int fd3 = creat(filename2);
    int fd4 = creat(filename2);
    int fd5 = creat(filename2);
    int fd6 = creat(filename2);
    int fd7 = creat(filename2);
    int fd8 = creat(filename2);
    int fd9 = creat(filename2);
    int fd10 = creat(filename2);
    int fd11 = creat(filename2);
    int fd12 = creat(filename2);
    int fd13 = creat(filename2);
    int fd14 = creat(filename2);
    if (fd3 == -1 || fd4 == -1 || fd5 == -1 || fd6 == -1 || fd7 == -1 ||
    fd8 == -1 || fd9 == -1 || fd10 == -1 || fd11 == -1 || fd12 == -1 ||
    fd13 == -1 || fd14 == -1) {
        printf("Error in having 16 file descriptions");
        exit(-4);
    }
    printf("passed test 4!\n");
    int fd15 = creat(filename2);
    if(fd15 != -1) {
        printf("error in having more than 16 file descriptions");
        exit(-5);
    }
    printf("passed test 5!\n");
    //creating files does not interfere with stdin and stdout;
    char buf[50];
    int bytesRead = read(fd1, buf, 50);
	int bytesWrote = write(1, buf, 50);
	if (bytesWrote == -1) {
        printf("error in stdin and stdout with creat");
        exit(-6);
    }

    return 0;
}
