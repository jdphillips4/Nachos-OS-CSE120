package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	

	private LinkedList<KThread> waitQueue = new LinkedList<KThread>(); //of threads
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        waitQueue.add(KThread.currentThread());
		conditionLock.release();
		boolean intStatus = Machine.interrupt().disable();
        KThread.sleep(); // Block the current thread
        Machine.interrupt().restore(intStatus);
		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        long t = Machine.timer().getTime();
		if( !waitQueue.isEmpty() ){ //do we iterate thru queue
			boolean intStatus = Machine.interrupt().disable();
			KThread nextThread = waitQueue.removeFirst(); // ready 1 thread. does order remove matter
            ThreadedKernel.alarm.cancel(nextThread);
            nextThread.ready();
        	Machine.interrupt().restore(intStatus);
		}
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 * iterates thru queue
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		while( ! waitQueue.isEmpty() ){
			wake();
		}
	}

    /**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses.  The current thread must hold the
	 * associated lock.  The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 * PART 4 ONLY
	 * same as before but also a timer. wait until alarm.
	 * //sleep()
		//wake thru arg timeout (waituntil) or wake() function (cancel function in alarm)
		//data struct for either condition.
		//remove from queue by timeout. or cancel is remove by wake
		//compare wakeTime of thread to timeout x
        this code only deals w 1 thread right? not the whole queue?
	 */
    public void sleepFor(long timeout) {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        //System.out.println(KThread.currentThread() + " sleepFor adding to waitQueue");
        waitQueue.add(KThread.currentThread());
        conditionLock.release();
        boolean status = Machine.interrupt().disable();
        // make the thread sleep for specific time
        ThreadedKernel.alarm.waitUntil(timeout);
        // Reacquire lock after waking up
        waitQueue.remove(KThread.currentThread());
        Machine.interrupt().restore(status);
        conditionLock.acquire();
    }

    private Lock conditionLock;

    // alternate executions using a cv
    private static class InterlockTest {
        private static Lock lock;
        private static Condition2 cv;

        private static class Interlocker implements Runnable {
            public void run () {
                lock.acquire();
                for (int i = 0; i < 10; i++) {
                    System.out.println(KThread.currentThread().getName());
                    cv.wake();   // signal
                    cv.sleep();  // wait
                }
                lock.release();
            }
        }

        public InterlockTest () {
            lock = new Lock();
            cv = new Condition2(lock);

            KThread ping = new KThread(new Interlocker());
            ping.setName("ping");
            KThread pong = new KThread(new Interlocker());
            pong.setName("pong");

            ping.fork();
            pong.fork();

            ping.join();
        }
    }

    // Test bounded producers/consumers
    public static void cvTest5() {
        final Lock lock = new Lock();
        // final Condition empty = new Condition(lock);
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread consumer = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    while(list.isEmpty()){
                        empty.sleep();
                    }
                    Lib.assertTrue(list.size() == 5, "List should have 5 values.");
                    while(!list.isEmpty()) {
                        // context swith for the fun of it
                        KThread.currentThread().yield();
                        System.out.println("Removed " + list.removeFirst());
                    }
                    lock.release();
                }
            });

        KThread producer = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    for (int i = 0; i < 5; i++) {
                        list.add(i);
                        System.out.println("Added " + i);
                        // context swith for the fun of it
                        KThread.currentThread().yield();
                    }
                    //printQueueContents();
                    empty.wake();
                    lock.release();
                }
            });

        consumer.setName("Consumer");
        producer.setName("Producer");
        consumer.fork();
        producer.fork();

        consumer.join();
        producer.join();
    }

    // sleep() should block current thread
    public static void cvTest6() {
        final Lock lock = new Lock();
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread run1 = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    list.add(1);
                    lock.release();
                }
            });

        // this thread should sleep and not add to list
        KThread run2 = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    if (list.size() > 0) {
                        empty.sleep();
                        list.add(2);        // this shouldn't run
                    }
                    lock.release();
                }
            });

        run1.setName("run1").fork();
        run1.join();
        run2.setName("run2").fork();
        // let run2 sleep for a bit
        for (int i=0; i<5; i++) {
            KThread.yield();
        }
        Lib.assertTrue(list.size() == 1, "run2 should still be sleeping");
        lock.acquire();
        empty.wake();
        lock.release();
        // wait a bit again
        for (int i=0; i<5; i++) {
            KThread.yield();
        }
        Lib.assertTrue(list.size() == 2, "run2 should have woken");
        System.out.println("cvTest6 passed!");
    }
    
    // wake() wakes at most 1 thread in queue
    public static void cvTest7() {
        final Lock lock = new Lock();
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread run1 = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    list.add(1);
                    lock.release();
                }
            });

        // this thread should sleep
        KThread run2 = new KThread( new Runnable () {
            public void run() {
                lock.acquire();
                if (list.size() > 0) {
                    empty.sleep();
                    // this only runs after being woken
                    list.add(2);
                }
                lock.release();
            }
        });
        
        // this thread should sleep x2
        KThread run3= new KThread( new Runnable () {
            public void run() {
                lock.acquire();
                if (list.size() > 0) {
                    empty.sleep();
                    // this only runs after being woken
                    list.add(3);
                }
                lock.release();
            }
        });

        run1.setName("run1").fork();
        run1.join();
        run2.setName("run2").fork();
        run3.setName("run3").fork();
        // let run2 sleep for a bit
        for (int i=0; i<5; i++) {
            KThread.yield();
        }
        Lib.assertTrue(list.size() == 1, "run2 and run3 should be sleeping");
        Lib.assertTrue(empty.waitQueue.size() == 2, "there should be 2 threads in queue");
        lock.acquire();
        empty.wake();
        lock.release();
        // wait a bit again
        run2.join();
        Lib.assertTrue(list.size() == 2, "only 1 thread should have been woken");
        Lib.assertTrue(empty.waitQueue.size() == 1, "there should be 1 thread in queue");
        lock.acquire();
        empty.wake();
        lock.release();
        // wait a bit again
        run3.join();
        Lib.assertTrue(list.size() == 3, "only 1 thread should have been woken");
        Lib.assertTrue(empty.waitQueue.size() == 0, "there should be no threads in queue");
        System.out.println("cvTest7 passed!");
    }

    // wakeAll wakes up all waiting threads
    public static void cvTest8() {
        final Lock lock = new Lock();
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread run1 = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    list.add(1);
                    lock.release();
                }
            });

        // this thread should sleep
        KThread run2 = new KThread( new Runnable () {
            public void run() {
                lock.acquire();
                if (list.size() > 0) {
                    empty.sleep();
                    // this only runs after being woken
                    list.add(2);
                }
                lock.release();
            }
        });
        
        // this thread should sleep x2
        KThread run3= new KThread( new Runnable () {
            public void run() {
                lock.acquire();
                if (list.size() > 0) {
                    empty.sleep();
                    // this only runs after being woken
                    list.add(3);
                }
                lock.release();
            }
        });

        run1.setName("run1").fork();
        run1.join();
        run2.setName("run2").fork();
        run3.setName("run3").fork();
        // let run2 sleep for a bit
        for (int i=0; i<5; i++) {
            KThread.yield();
        }
        Lib.assertTrue(list.size() == 1, "run2 and run3 should be sleeping");
        Lib.assertTrue(empty.waitQueue.size() == 2, "there should be 2 threads in queue");
        lock.acquire();
        empty.wakeAll();
        lock.release();
        // wait a bit again
        run2.join();
        run3.join();
        Lib.assertTrue(list.size() == 3, "all threads should have been woken");
        Lib.assertTrue(empty.waitQueue.size() == 0, "there should be no threads in queue");
        System.out.println("cvTest8 passed!");
    }

    // if a thread calls any of the synchronization methods without holding the lock, Nachos asserts
    public static void cvTest9() {
        final Lock lock = new Lock();
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        boolean sleepCaught = false;
        try {
            empty.sleep();
        } catch(Error e) {
            sleepCaught = true;
        }
        boolean wakeCaught = false;
        try {
            empty.wake();
        } catch(Error e) {
            wakeCaught = true;
        }
        boolean wakeAllCaught = false;
        try {
            empty.wakeAll();
        } catch(Error e) {
            wakeAllCaught = true;
        }
        Lib.assertTrue(sleepCaught == true, "sleep w/o lock should assert!");
        Lib.assertTrue(wakeCaught == true, "wake w/o lock should assert!");
        Lib.assertTrue(wakeAllCaught == true, "wakeAll w/o lock should assert!");
        System.out.println("cvTest9 passed!");
    }

    // wake and wakeAll with no waiting threads have no effect
    // yet future threads that sleep will still block
    // (i.e., the wake/wakeAll is "lost", which is in contrast to the semantics of semaphores).
     // wake() wakes at most 1 thread in queue
     public static void cvTest10() {
        final Lock lock = new Lock();
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread run1 = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    list.add(1);
                    lock.release();
                }
            });

        // this thread should sleep
        KThread run2 = new KThread( new Runnable () {
            public void run() {
                lock.acquire();
                if (list.size() > 0) {
                    empty.sleep();
                    // this only runs after being woken
                    list.add(2);
                }
                lock.release();
            }
        });
        
        // this thread should sleep x2
        KThread run3= new KThread( new Runnable () {
            public void run() {
                lock.acquire();
                if (list.size() > 0) {
                    empty.sleep();
                    // this only runs after being woken
                    list.add(3);
                }
                lock.release();
            }
        });

        run1.setName("run1").fork();
        run1.join();
        lock.acquire();
        empty.wake();
        lock.release();
        run2.setName("run2").fork();
        run3.setName("run3").fork();
        // stall for a bit
        for (int i=0; i<5; i++) {
            KThread.yield();
        }
        Lib.assertTrue(list.size() == 1, "the previous wake was lost");
        Lib.assertTrue(empty.waitQueue.size() == 2, "there should be 2 threads in queue");
        lock.acquire();
        empty.wakeAll();
        lock.release();
        // wait a bit again
        run2.join();
        run3.join();
        Lib.assertTrue(list.size() == 3, "all threads should have been woken");
        Lib.assertTrue(empty.waitQueue.size() == 0, "there should be no threads in queue");
        System.out.println("cvTest10 passed!");
    }

    // a thread that calls sleepFor will timeout and return after x ticks even if no other threads wake it up
    public static void sleepFor1(){
        final Lock lock = new Lock();
        final Condition2 c2 = new Condition2(lock);
        KThread thread = new KThread(() -> {
            lock.acquire();
            long t0 = Machine.timer().getTime();
            c2.sleepFor(2000); // Sleep for 2000 ticks
            long t1 = Machine.timer().getTime();
            lock.release();
            Lib.assertTrue((t1 - t0) > 2000, "a sleepFor should start on its own if no wake is called");
        });
        
        thread.fork();
        thread.join();
        System.out.println("sleepFor1 passed!");
    }

    // similar to sleepFor1
    private static void sleepForTest1 () {
        Lock lock = new Lock();
        Condition2 cv = new Condition2(lock);
    
        lock.acquire();
        long t0 = Machine.timer().getTime();
        // no other thread will wake us up, so we should time out
        cv.sleepFor(2000);
        long t1 = Machine.timer().getTime();
        lock.release();
        Lib.assertTrue((t1 - t0) > 2000, "a sleepFor should start on its own if no wake is called");
        System.out.println("sleepForTest1 passed!");
    }
    
    // threads can still be woken even before sleepFor expires
    public static void sleepFor2(){
        final Lock lock = new Lock();
        final Condition2 c2 = new Condition2(lock);
        KThread thread1 = new KThread( () -> {
            lock.acquire();
            long t0 = Machine.timer().getTime();
            c2.sleepFor(10000000);
            long t1 = Machine.timer().getTime();
            lock.release();
            Lib.assertTrue((t1 - t0) < 10000000, "wake should terminate sleep for early!");
        }) ;
        KThread thread2 = new KThread(() -> {
            long t0 = Machine.timer().getTime();
            lock.acquire();
            c2.wake();
            lock.release();
        });
        thread1.setName("thread1").fork();
        thread2.setName("thread2").fork();
        thread2.join();
        thread1.join();
        System.out.println("sleepFor2 passed!");
    }

    // sleepFor handles multiple threads correctly (e.g., different timeouts, all are woken up with wakeAll
    public static void sleepFor3(){
        final Lock lock = new Lock();
        final Condition2 c2 = new Condition2(lock);
        KThread thread1 = new KThread( () -> {
            lock.acquire();
            long t0 = Machine.timer().getTime();
            c2.sleepFor(2000); //2nd
            long t1 = Machine.timer().getTime();
            Lib.assertTrue((t1-t0) < 2000, "wakeAll terminates early");
            lock.release();
        }) ;
        //printQueueContents();
        KThread thread2 = new KThread( () -> {
            lock.acquire();
            long t0 = Machine.timer().getTime();
            c2.sleepFor(4000);
            long t1 = Machine.timer().getTime();
            Lib.assertTrue((t1-t0) < 4000, "wakeAll terminates early");
            lock.release();
        }) ;
        KThread thread3 = new KThread( () -> {
            lock.acquire();
            long t0 = Machine.timer().getTime();
            c2.sleepFor(200000); //2nd
            long t1 = Machine.timer().getTime();
            Lib.assertTrue((t1-t0) < 200000, "wakeAll terminates early");
            lock.release();
        }) ;

        thread1.setName("thread1").fork();
        thread2.setName("thread2").fork();
        thread3.setName("thread3").fork();
        // busy stall so that the wait queue can be filled
        for (int i=0; i<5; i++) {
            KThread.yield();
        }
        lock.acquire();
        long t = Machine.timer().getTime();
        c2.wakeAll();
        lock.release();
        thread1.join();
        thread2.join();
        thread3.join();
    }
    public void printQueueContents() { 
        System.out.println("PRINTING THE WAIT QUEUE");
		for (KThread kt : waitQueue) {//for each
			System.out.println("Thread: " + kt);
		}
	} 
    public static void selfTest() {
        new InterlockTest();
		cvTest5();
        cvTest6();
        cvTest7();
        cvTest8();
        cvTest9();
        cvTest10();
        sleepFor1();
        sleepForTest1();
        sleepFor2();
        sleepFor3();
    }
}
