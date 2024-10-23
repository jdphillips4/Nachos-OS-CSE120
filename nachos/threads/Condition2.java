package nachos.threads;

import nachos.machine.*;
import nachos.threads.Alarm.SleepThread;

import java.util.LinkedList;
import nachos.threads.Alarm;

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
	

	private static LinkedList<KThread> waitQueue = new LinkedList<KThread>(); //of threads
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
		conditionLock.release();
		waitQueue.add(KThread.currentThread());
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
		if( ! waitQueue.isEmpty() ){ //do we iterate thru queue
			boolean intStatus = Machine.interrupt().disable();
			waitQueue.removeLast().ready(); // ready 1 thread. does order remove matter
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
            conditionLock.release();
            long wakeTime = Machine.timer().getTime() + timeout;
            // make the thread sleep for specific time
            ThreadedKernel.alarm.waitUntil(wakeTime);
            // Reacquire lock after waking up
            waitQueue.remove(KThread.currentThread());
            conditionLock.acquire();
            }

        private Lock conditionLock;

		//tests
		// Place Condition2 testing code in the Condition2 class.

    // Example of the "interlock" pattern where two threads strictly
    // alternate their execution with each other using a condition
    // variable.  (Also see the slide showing this pattern at the end
    // of Lecture 6.)

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

            // We need to wait for ping to finish, and the proper way
            // to do so is to join on ping.  (Note that, when ping is
            // done, pong is sleeping on the condition variable; if we
            // were also to join on pong, we would block forever.)
            // For this to work, join must be implemented.  If you
            // have not implemented join yet, then comment out the
            // call to join and instead uncomment the loop with
            // yields; the loop has the same effect, but is a kludgy
            // way to do it.
            ping.join();
            // for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
        }
    }

    // Invoke Condition2.selfTest() from ThreadedKernel.selfTest()

	  // Place Condition2 test code inside of the Condition2 class.

    // Test programs should have exactly the same behavior with the
    // Condition and Condition2 classes.  You can first try a test with
    // Condition, which is already provided for you, and then try it
    // with Condition2, which you are implementing, and compare their
    // behavior.

    // Do not use this test program as your first Condition2 test.
    // First test it with more basic test programs to verify specific
    // functionality.

    public static void cvTest5() {
		System.out.println("condition2 cvtest5");
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
                    empty.wake();
                    lock.release();
                }
            });

        consumer.setName("Consumer");
        producer.setName("Producer");
        consumer.fork();
        producer.fork();

        // We need to wait for the consumer and producer to finish,
        // and the proper way to do so is to join on them.  For this
        // to work, join must be implemented.  If you have not
        // implemented join yet, then comment out the calls to join
        // and instead uncomment the loop with yield; the loop has the
        // same effect, but is a kludgy way to do it.
        consumer.join();
        producer.join();
        //for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
    }

	public static void test1(){
		System.out.println("sleep blocks the calling thread");
	}

	public static void test2(){
		System.out.println("wake wakes up ONE thread, even if multiple threads are waiting");
    }

	public static void test3(){
		System.out.println("wakeAll wakes up all waiting threads");
	}
    // public static void test4(){
	// 	System.out.println("if a thread calls any of the synchronization methods without holding the lock, Nachos asserts");
	// }
    public static void sleepFor1(){
		System.out.println("a thread that calls sleepFor will timeout and return after x ticks if no other thread calls wake to wake it up");
        final Lock lock = new Lock();
        final Condition2 c2 = new Condition2(lock);
        KThread thread = new KThread(() -> {
            lock.acquire();
            c2.sleepFor(2000); // Sleep for 2000 ticks
            System.out.println("Thread woke up after timeout");
            lock.release();
        });
        
        thread.fork();
        thread.join(); // Wait for the thread to finish
    
    }
    public static void sleepFor2(){
		System.out.println("a thread that calls sleepFor will wake up and return if another thread calls wake before the timeout expires");
        final Lock lock = new Lock();
        final Condition2 c2 = new Condition2(lock);
        KThread thread1 = new KThread( () -> {
            lock.acquire();
            c2.sleepFor(2000); //should wake after thread2
            System.out.println("thread 1 wakes up");
            lock.release();
        }) ;
        KThread thread2 = new KThread(() -> {
            lock.acquire();
            c2.sleepFor(500);
            c2.wake(); //wait 500 ticks to wake up thread2 1st
            System.out.println("Thread 2 wakes up. should be 1st");
            lock.release();
        });
        thread1.fork();
        thread2.fork();
        thread1.join();
        thread2.join();
    
    }
    public static void sleepFor3(){
		System.out.println("thread that calls sleepFor will timeout and return after x ticks if no other thread calls wake to wake it up");
        final Lock lock = new Lock();
        final Condition2 c2 = new Condition2(lock);
        KThread thread1 = new KThread( () -> {
            lock.acquire();
            c2.sleepFor(2000); //should wake after thread2
            System.out.println("thread 1 wakes up");
            lock.release();
        }) ;
        thread1.fork();
        thread1.join();
    }
    public static void sleepFor4(){
		//System.out.println("sleepFor handles multiple threads correctly (e.g., different timeouts, all are woken up with wakeAll");
        System.out.println("sleepFor removes woken threads from waitQueue");
        final Lock lock = new Lock();
        final Condition2 c2 = new Condition2(lock);
        KThread thread1 = new KThread( () -> {
            lock.acquire();
            c2.sleepFor(2000); //2nd
            c2.wake();
            System.out.println("thread 1 wakes up");
            lock.release();
        }) ;
        printQueueContents();
        KThread thread2 = new KThread( () -> {
            lock.acquire();
            c2.sleepFor(1000); //1st
            c2.wake();
            System.out.println("thread 2 wakes up");
            lock.release();
        }) ;
        printQueueContents();
        KThread thread3 = new KThread( () -> {
            lock.acquire();
            c2.sleepFor(4000); //3rd
            c2.wake();
            System.out.println("thread 3 wakes up");
            lock.release();
        }) ;
        printQueueContents();

        thread1.fork();
        thread2.fork();
        thread3.fork();
        thread1.join();
        thread2.join();
        thread3.join();
        printQueueContents();
    }
    public static void printQueueContents() { //debugging function 
        System.out.println("PRINTING THE WAIT QUEUE");
		for (KThread kt : waitQueue) {//for each
			System.out.println("Thread: " + kt.getName() );
		}
	}
    public static void selfTest() {
        //new InterlockTest();
		//cvTest5();
        //sleepFor2();
        sleepFor4();
    }
}
