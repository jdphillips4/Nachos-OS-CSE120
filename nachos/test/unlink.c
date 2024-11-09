#include "syscall.h"
//coff is already made when make
/**
 * If this or another
 * process has the file open, the underlying file system
 * implementation in StubFileSystem will cleanly handle this situation
 * (this process will ask the file system to remove the file, but the
 * file will not actually be deleted by the file system until all
 * other processes are done with the file).
 */
int main (int argc, char *argv[]){
   char *filename1 = "testfile1.txt"; //create new file 
   int fd1;
   fd1 = creat(filename1);
   open(filename1);
   if( unlink(filename1) == -1 ) write(1, "cannot delete testfile1.txt since it's still open. process not done.", 16);
   //close testfile1.txt so we can unlink
   close(fd1);
   if( unlink(filename1) == 0 ) write(1, "successfully unlinked testfile1.txt", 16);

   //make a bunch of processes use the file, like have it open somewhere?
}
