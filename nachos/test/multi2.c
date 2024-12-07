/*
 * multi2.c
 *
 * program for testing multiple concurrent processes
 * using memory. Includes IO now
 */

#include "syscall.h"

int
main (int argc, char *argv[])
{
    char *prog1 = "swap5.coff";
    char *prog2 = "write1.coff";
    char *prog3 = "read1.coff";
    int pid1, status1, j1 = 0;
    int pid2, status2, j2 = 0;
    int pid3, status3, j3 = 0;

    pid1 = exec (prog1, 0, 0);
    pid2 = exec (prog2, 0, 0);
    pid3 = exec (prog3, 0, 0);
    j1 = join (pid1, &status1);
    j2 = join (pid2, &status2);
    j3 = join (pid3, &status3);
    printf("Swap5 ending with status %d\n", status1);
    printf("write1 ending with status %d\n", status2);
    printf("read1 ending with status %d\n", status3);
    exit (0);
}