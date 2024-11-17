/*
 * exec1.c
 *
 * Simple program for testing exec.  It does not pass any arguments to
 * the child.
 */

#include "syscall.h"

int
main (int argc, char *argv[])
{
    char *prog = "exit1.coff";
    int pid;

    pid = exec (prog, 0, 0);
    if (pid < 0) {
        printf("ERROR\n");
	    exit (-1);
    }
    printf("SUCCESS WITH PID: %d\n", pid);
    exit (0);
}
