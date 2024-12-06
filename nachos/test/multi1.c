/*
 * multi1.c
 *
 * Simple program for testing multiple concurrent processes
 * using memory
 */

#include "syscall.h"

int
main (int argc, char *argv[])
{
    char *prog1 = "swap4.coff";
    char *prog2 = "swap5.coff";
    int pid1, status1, j1 = 0;
    int pid2, status2, j2 = 0;

    pid1 = exec (prog1, 0, 0);
    pid2 = exec (prog2, 0, 0);
    j1 = join (pid1, &status1);
    j2 = join (pid2, &status2);
    printf("Swap4 ending with status %d\n", status1);
    printf("Swap5 ending with status %d\n", status2);
    exit (0);
}