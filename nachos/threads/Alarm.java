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

    //private Queue<SleepThread> sleepThreadsQueue = new LinkedList<>();
    private PriorityQueue<SleepThread> sleepThreadsQueue = new PriorityQueue<>(Comparator.comparingLong(st -> st.wakeTime));

    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
                timerInterrupt();
            }
        });
    }

	private void printQueueContents() { //debugging function 
		for (SleepThread sleepThread : sleepThreadsQueue) {//for each
			System.out.println("Thread: " + sleepThread.thread + ", Wake Time: " + sleepThread.wakeTime);
		}
	}
	private void printThread(SleepThread st){
		System.out.println("Thread to wake : "+ st.thread + ", Wake Time: " + st.wakeTime);
	}

    public void waitUntil(long x) {
        if (x <= 0) return;

        long wakeTime = Machine.timer().getTime() + x;
        SleepThread st = new SleepThread(KThread.currentThread(), wakeTime);
        sleepThreadsQueue.add(st);
		System.out.println("Contents of queue : ");
		printQueueContents();

        boolean intStatus = Machine.interrupt().disable();
        KThread.sleep(); // Block the current thread
        Machine.interrupt().restore(intStatus);
    }

    public void timerInterrupt() {
		//System.out.println("timer interrupt function..."); iterating thru whole thing
        while (!sleepThreadsQueue.isEmpty() ) { 
			boolean intStatus = Machine.interrupt().disable();
            SleepThread st = sleepThreadsQueue.poll(); //remove not poll
        
            printQueueContents();
  			printThread(st);
            st.thread.ready(); // when each alarm is over wake up. 
            Machine.interrupt().restore(intStatus);
        }
        KThread.yield(); // Yield to allow other threads to run
    }

    public static void alarmTest1() {
        int durations[] = {1000, 10 * 1000, 100 * 1000};
        long t0, t1;

        for (int d : durations) {
            t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil(d);
            t1 = Machine.timer().getTime();
            System.out.println("alarmTest1: waited for " + (t1 - t0) + " ticks");
            //t1-t0 <= 500. 
        }
    }
    public static void alarmTest2() {
        //once the alarm is over, within 500 ticks wake it up. my program waits
        //thread 3 should fire 1st since its shortest
        System.out.println("Starting alarmTest2: Multiple threads sleeping, wake them");

        KThread thread1 = new KThread(() -> {
                // long t0 = Machine.timer().getTime();
                // System.out.println("currenttime is :"+ t0);
                ThreadedKernel.alarm.waitUntil(2000);
                System.out.println("Thread 1 woke up after 2000 ticks");
                // long t1 = Machine.timer().getTime();
                // System.out.println("thread1: waited for " + (t1 - t0) + " ticks");
                //t1-t0 <= 500. 
      
        });

        KThread thread2 = new KThread(() -> {
            ThreadedKernel.alarm.waitUntil(3000);
            System.out.println("Thread 2 woke up after 3000 ticks");
            
        });

        KThread thread3 = new KThread(() -> {
            ThreadedKernel.alarm.waitUntil(1000);
            System.out.println("Thread 3 woke up after 1000 ticks");
        });

        thread1.fork();
        thread2.fork();
        thread3.fork();

        // Busy wait to let threads to finish
        for (int i = 0; i < 5; i++) {
            System.out.println("Main thread busy waiting...");
            KThread.currentThread().yield();
        }

        thread1.join();
        thread2.join();
        thread3.join();
        System.out.println("3 threads joined");
    }

    public static void alarmTest3() {
        System.out.println(" alarmTest3: Edge case with zero wait");

        long t0 = Machine.timer().getTime();
        ThreadedKernel.alarm.waitUntil(0); // Should return immediately
        long t1 = Machine.timer().getTime();
        System.out.println("alarmTest3: waited for " + (t1 - t0) + " ticks (expected: 0)");
    }

    public static void alarmTest4() {
        System.out.println(" alarmTest4: Negative wait time");

        long t0 = Machine.timer().getTime();
        ThreadedKernel.alarm.waitUntil(-500); // Should return immediately
        long t1 = Machine.timer().getTime();
        System.out.println("alarmTest4: waited for " + (t1 - t0) + " ticks (expected: 0)");
    }

    public static void selfTest() {
        //alarmTest1(); //pass
        alarmTest2(); //help
        //alarmTest3(); //pass
        //alarmTest4(); //pass
    }

    // Other methods like cancel() can be implemented here...
}
