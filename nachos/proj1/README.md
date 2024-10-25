# Project 1 README

Group Members: Jordan Phillips, Sarena Pham, Kane Li

**Part 1:** Using a queue, we sought to store threads that need to wait and sleep them, where if the timer interrupt finds that the sleeping thread can then be woken puts it on the ready queue. We tested this using multiple threads, always ensuring that the time after the thread resumes is after the waitUntil.

**Part 2:** Join seeks to use a hashMap to track each child and its parent. By using a lock, we seek to atomically update the datastructure, adding a key on join and removing it on finish. Therefore, join simply has to sleep until the key is removed. This was tested using many threads and asserting that the expecting threads have a STATUSFINISHED set after any join.

**Part 3:** We sought to initiailize a private thread queue for every condition variable. By sleeping and saving the queue, it can then be woken up on a call to wake(). Tests for this mainly involved using the default bounded buffer and comparing our output to the output for Condition.java.

Contributions: Kane Li: Part 1, 2, 3, 4. Mainly writing tests, debugging, and cleaning code.