package nachos.threads;

import nachos.machine.*;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    // Class to represent a sleeping thread
    class SleepThread {
        long wakeTime;
        KThread thread;

        public SleepThread(KThread thread, long wakeTime) {
            this.thread = thread;
            this.wakeTime = wakeTime;
        }
    }

    private PriorityQueue<SleepThread> sleepThreadsQueue = new PriorityQueue<>(Comparator.comparingLong(st -> st.wakeTime));

    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
                timerInterrupt();
            }
        });
    }

	/*private void printQueueContents() { //debugging function 
		for (SleepThread sleepThread : sleepThreadsQueue) {//for each
			System.out.println("Thread: " + sleepThread.thread + ", Wake Time: " + sleepThread.wakeTime);
		}
	}
	private void printThread(SleepThread st){
		System.out.println("Thread to wake : "+ st.thread + ", Wake Time: " + st.wakeTime);
	} */

    // sleep thread for at minimum x ticks
    public void waitUntil(long x) {
        if (x <= 0) return;

        long wakeTime = Machine.timer().getTime() + x;
        SleepThread st = new SleepThread(KThread.currentThread(), wakeTime);
        sleepThreadsQueue.add(st);

        boolean intStatus = Machine.interrupt().disable();
        KThread.sleep(); // Block the current thread
        Machine.interrupt().restore(intStatus);
    }

    // interrupt that is occasionally fired
    public void timerInterrupt() {
        if (!sleepThreadsQueue.isEmpty()) { 
            if (Machine.timer().getTime() >= sleepThreadsQueue.peek().wakeTime) {
                boolean intStatus = Machine.interrupt().disable();
                SleepThread st = sleepThreadsQueue.poll();
            
                st.thread.ready(); 
                Machine.interrupt().restore(intStatus);
            }
        }
        KThread.yield(); // Yield to allow other threads to run
    }

    /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
    public boolean cancel(KThread thread) {
		return false;
	}

    // test individual threads
    public static void alarmTest1() {
        int durations[] = {1000, 10 * 1000, 100 * 1000};
        long t0, t1;

        for (int d : durations) {
            t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil(d);
            t1 = Machine.timer().getTime();
            Lib.assertTrue((t1 - t0) > d);
        }
        System.out.println("alarmTest1 passed!");
    }

    // test multiple concurrent threads
    public static void alarmTest2() {
        KThread thread1 = new KThread(() -> {
            long t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil(2000);
            long t1 = Machine.timer().getTime();
            Lib.assertTrue((t1 - t0) > 2000);
        });

        KThread thread2 = new KThread(() -> {
            long t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil(3000);
            long t1 = Machine.timer().getTime();
            Lib.assertTrue((t1 - t0) > 3000);
        });

        KThread thread3 = new KThread(() -> {
            long t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil(1000);
            long t1 = Machine.timer().getTime();
            Lib.assertTrue((t1 - t0) > 1000);
        });

        thread3.setName("thread3").fork();
        thread1.setName("thread1").fork();
        thread2.setName("thread2").fork();

        // Busy wait to let threads to finish
        for (int i = 0; i < 5; i++) {
            KThread.currentThread().yield();
        }

        thread1.join();
        thread2.join();
        thread3.join();
        System.out.println("alarmTest2 passed!");
    }

    // test bad input
    public static void alarmTest3() {
        long t0 = Machine.timer().getTime();
        ThreadedKernel.alarm.waitUntil(0); // Should return immediately
        long t1 = Machine.timer().getTime();
        Lib.assertTrue((t1 - t0) < 5);
        System.out.println("alarmTest3 passed!");
    }

    // test bad input 2
    public static void alarmTest4() {
        long t0 = Machine.timer().getTime();
        ThreadedKernel.alarm.waitUntil(-500); // Should return immediately
        long t1 = Machine.timer().getTime();
        Lib.assertTrue((t1 - t0) < 5);
        System.out.println("alarmTest4 passed!");
    }

    public static void selfTest() {
        alarmTest1();
        alarmTest2(); 
        alarmTest3();
        alarmTest4(); 
    }
}
