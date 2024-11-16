#include "syscall.h"

int main (int argc, char *argv[]) {
    char *filename1 = "testfile1.txt"; // create new file 
    int fd1;
    
    // Create the file
    fd1 = creat(filename1);
    if (fd1 == -1) {
        write(1, "Failed to create testfile1.txt\n", 34);
        return -1;
    }

    // Open the file
    int fd2 = open(filename1);
    if (fd2 == -1) {
        write(1, "Failed to open testfile1.txt\n", 30);
        close(fd1); // Close the file descriptor
        return -1;
    }

    // Attempt to unlink the file while it's open
    if (unlink(filename1) == -1) {
        write(1, "Cannot delete testfile1.txt since it's still open.\n", 54);
    }

    // Close the file to allow unlinking
    write(1,"fd 2 (file descriptor for opening file1 = "+ fd2, 54);
    close(fd2);
    if (unlink(filename1) == 0) {
        write(1, "Successfully unlinked testfile1.txt\n", 37);
    } else {
        write(1, "Failed to unlink testfile1.txt\n", 33); //error
    }

    //  create multiple processes here to test concurrent access
    // For example, using exec to run another program that opens the same file

   char *filename2 = "testfile2.txt"; // create new file but keep open
   int fd3 = creat(filename2);
    if (fd3 == -1) {
        write(1, "Failed to create testfile2.txt\n", 34);
        return -1;
    }

   //make multiple processes access testfile2. attempt to unlink should fail

   // before making more processes, Open the second file
    int fd4 = open(filename2);
    if (fd4 == -1) {
        write(1, "Failed to open testfile2.txt\n", 30); //error
        close(fd4); // Close the file descriptor
        return -1;
    }

    // Execute a new process that tries to open the same file
    int exec_result = exec("test_exec", 0, 0); // Execute the program 
    if (exec_result == -1) {
        write(1, "Failed to execute test_exec\n", 30);
        close(fd4); // Close the file descriptor
        return -1;
    }

    // Close the second file descriptor
    close(fd4);
    return 0;
}
