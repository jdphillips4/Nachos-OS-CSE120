#include "syscall.h"
//coff is already made when make
int main (int argc, char *argv[])
{
   char *filename1 = "testfile1.txt"; //create new file
   char *filename2 = "testfile2.txt"; //open existing file
   char *filename3 = "notexist.txt"; //doesnt exist
   int fd1, fd2, fd3;

   //test 1: create new file
    fd1 = creat(filename1);
    if(fd1 == -1) write(1, "Failed to create testfile1.txt", 16);
    else{
        write( 1, "Successfully created testfile1.txt", 16);
        close(fd1);
    }

    //test 2: open existing file
    fd2 = open(filename1);
    if(fd2==-1) write(1, "Failed to open testfile1.txt", 16);
    else{
        write(1, "Successfully opened testfile1.txt", 16);
        close(fd2);
    }

    //test 3:
    fd3 = open(filename3);
    if(fd3==-1) write(1, "Failed to open notexist.txt", 16);
    else{
        write(1, "this shouldnt run or u have an issue", 16);
        close(fd3);
    }

    return 0;
}
