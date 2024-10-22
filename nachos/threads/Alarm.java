package nachos.threads;

import nachos.machine.*;
import java.util.Queue;
import java.util.LinkedList;
import java.util.*;

// this is the class that custom threads are initialized with
class MyRunnable implements Runnable {
    private int count = 0;
    private long wait = 0;
    
    public MyRunnable(int num, long duration) {
		count = num;
		wait = duration;
    };

    public void run() {
		System.out.println("running thread with count: " +  count);
		long start = Machine.timer().getTime();
		ThreadedKernel.alarm.waitUntil(wait);
		long end = Machine.timer().getTime();
        System.out.println("I was run with count: " + count + " after " + (end - start) +  " ticks");
    }
}

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */ 
public class Alarm {
	// Add Alarm testing code to the Alarm class
	public static void alarmTest1() {//make more test cases
		int durations[] = {1000, 10*1000, 100*1000};
		long t0, t1;
	
		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil (d);
			t1 = Machine.timer().getTime();
			System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
	}

	// Test our Alarm code with multiple threads
	public static void threadTest() {
		MyRunnable runnable1 = new MyRunnable(1, 10);
		KThread thread1 = new KThread(runnable1);
		thread1.setName("thread1");
		MyRunnable runnable2 = new MyRunnable(2, 0);
		KThread thread2 = new KThread(runnable2);
		thread2.setName("thread2");

		boolean status = Machine.interrupt().disable();
		thread1.fork();
		thread2.fork();
		Machine.interrupt().restore(status);
		

		// stall so main thread doesn't shut it down
		int count = 0;
		for(int i = 0; i < 1000; i++) {
			KThread.yield();
		}
	}
	
	// Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
	public static void selfTest() {
		alarmTest1();
		threadTest();
	}
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	class sleepThread{ //obj. constructor. 
		//then populate queue w all sleep thread objs
		long wakeTime;
		KThread thread;
		public sleepThread(KThread thread, long wakeTime ){
			this.thread = thread;
			this.wakeTime = wakeTime;
		}
	}

	Queue<sleepThread> sleepThreadsQueue = new LinkedList<sleepThread>();
	// LL is expanded upon queue
	//wake . store k thread into ref. chek if its time to ake them up
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		}); 
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 * multiprocessing. current thread runs may never give up. timerInterrupt forces 
	 * interrupt
	 * r alarms over? ready them
	 * yield is context switch, run next thread
	 */
	public void timerInterrupt() {
		// System.out.println("in the timer interreupt only");
		//track threads ready. add a struct to track ready threads
		//reuuse while loop condition if...<
		//get each thread's time and check w current time
   		/*while ( !sleepThreadsQueue.isEmpty() && sleepThreadsQueue.peek().wakeTime <= Machine.timer().getTime()){
			System.out.println("peeking! " + sleepThreadsQueue.peek());
			//System.out.println("in the timer interreupt while");
			boolean intStatus = Machine.interrupt().disable();
			sleepThreadsQueue.poll().thread.ready();//add the current thread to readylist if it's ready
			//ready kthread in the queue
			Machine.interrupt().restore(intStatus);//pass in
		}*/
		if (!sleepThreadsQueue.isEmpty()) {
			boolean status = Machine.interrupt().disable();
			if (sleepThreadsQueue.peek().wakeTime <= Machine.timer().getTime()) {
				sleepThreadsQueue.poll().thread.ready();
			}
			Machine.interrupt().restore(status);
		}
		KThread.yield(); //add code before 
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 * 1 instance of alarm. not multiple threads. in th os
	 */
	public void waitUntil(long x) {
		// System.out.println("in the wait until");
		// for now, cheat just to get something working (busy waiting is bad)
		if( x <= 0 ) return;
		long wakeTime = Machine.timer().getTime() + x;
		//u wake up every minute to check the time not good
		//put this thread in block  state until the wake up time
		//dont connect them
		//how to append this thread 
		//make obj
		sleepThread st = new sleepThread(KThread.currentThread(), wakeTime);
		//make a function called setWakeTime and getWakeTime
		sleepThreadsQueue.add(st);
		boolean intStatus = Machine.interrupt().disable(); //action
		KThread.sleep(); //must wake up
		//timerInterrupt() auto called
		//yield moves thread to ready
		//restore interrupt
		Machine.interrupt().restore(intStatus);//pass in
		
		// while (wakeTime > Machine.timer().getTime())
		// 	KThread.yield(); //busy waiting
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


}
