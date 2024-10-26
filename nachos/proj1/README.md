# Project 1 README

Group Members: Jordan Phillips, Sarena Pham, Kane Li

**Part 1:** Using a queue, we sought to store threads that need to wait and sleep them, where if the timer interrupt finds that the sleeping thread can then be woken puts it on the ready queue. We tested this using multiple threads, always ensuring that the time after the thread resumes is after the waitUntil.

**Part 2:** Join seeks to use a hashMap to track each child and its parent. By using a lock, we seek to atomically update the datastructure, adding a key on join and removing it on finish. Therefore, join simply has to sleep until the key is removed. This was tested using many threads and asserting that the expecting threads have a STATUSFINISHED set after any join.

**Part 3:** We sought to initiailize a private thread queue for every condition variable. By sleeping and saving the queue, it can then be woken up on a call to wake(). Tests for this mainly involved using the default bounded buffer and comparing our custom test case outputs to the outputs if we were to use Condition.java.

**Part 4:** Our goal mainly had 2 key sections. In sleepFor, it adds a thread to the waitQueue. If the timer ends first, the thread is removed from the queue and moved to the ready queue. If wake() is called first, the thread cancels its alarm and is moved to the ready queue. Testing this mainly involved following the general recommendations on the assignment.

**Part 5:** Here, we mainly utilized a hashmap and a lock for updating the hashmap. Whenever we updated the hashmap, we'd make sure to use the lock, and the hashmap itself would keep track of a special exchange class for each tab. Our tests are mainly built off the default ones given.

### Overview
Overall, the code works for the default cases and the test cases that we created, even with varying context switch configurations. Though part 5 rendevouz could use more testing, most parts seem to hold on fine. Sarena worked a lot in initial coding, Kane worked in writing test cases + cleaning, and Jordan worked on much of the initial parts as well as 3 and 5.

Contributions: Kane Li: Part 1, 2, 3, 4. Mainly writing tests, debugging, and cleaning code.