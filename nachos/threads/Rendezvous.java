package nachos.threads;
import nachos.machine.*;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 * mechanism for threads to exchange values, using locks and condition variables to manage concurrency
 */
public class Rendezvous {
    //single case 1st then multiple
    private Lock lock;
    //private Condition condition;
    //private boolean waiting;
    //private int storedValue;//mailbox
    HashMap<Integer, ExchangeState> theHashMap;
    //key=tag, value=condition
    /**
     * Allocate a new Rendezvous. initialize ds 
     */

    private class ExchangeState { //
        
        int value;
        Condition condition;

        public ExchangeState(int value, Condition condition){
            this.value = value;
            this.condition = condition;
        }
    }

    public Rendezvous () {
        lock = new Lock();
        //condition = new Condition(lock);
        //waiting = false;
        //storedValue = 0;
        theHashMap = new HashMap<Integer, ExchangeState>();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallelUCS
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange. the value u end w
     */
    public int exchange (int tag, int value) { 
        //int exchange = 0; //new
	    lock.acquire();
        //insert new entry
        if (!theHashMap.containsKey(tag)) { //first of the tag need to wait for next one
            Condition condition = new Condition(lock);
            ExchangeState toBeSwitched = new ExchangeState(value, condition);
            theHashMap.put(tag, toBeSwitched);

            condition.sleep(); //Block until another thread B arrives
            lock.release();
            theHashMap.remove(tag);
            return toBeSwitched.value; //will be modified to whatever thread Bs value was
        }
        else { //tag already exists, need to swap
            ExchangeState otherThread = theHashMap.get(tag);
            int otherValue = otherThread.value;

            otherThread.value = value; // waiting thread gets value of current thread
            otherThread.condition.wake();

            

            lock.release();
            return otherValue;
        }



        // if( theHashMap.get(tag) == null ){  //wait=f
        //     System.out.println("enter hashmap no matching tag");
        //     Condition c = new Condition(lock);
        //     theHashMap.put(tag ,c ); //populate waiting room
        //     waiting = true; //waiting for matching thread to wake it up
        //     exchange = value;
        //     storedValue = value;
        //     System.out.println("thread1 exchange is: "+exchange);
        //     System.out.println("thread 1 puts "+storedValue+" in mailbox");
        //     c.sleep(); //does NOT do EXCHANGE
        // }
        // Condition c = theHashMap.get(tag); //this thread calls exchange. thread2
        // if( waiting == true ){ //matching thread will wake up sleeping thread. EXCHANGE. make sure thread doing exchange is NOT SLEEEP()
        //     System.out.println("thread 2 want to wake thread 1");
        //     waiting = false;
        //     exchangeState e = new exchangeState( value , c); //access switchvalue from condition
        //     exchange = storedValue; //t2 gets val1 from mailbox. 
        //     storedValue = e.switchValue; //t2 puts val2 in mailbox
        //     System.out.println("thread2 exchange is: "+exchange);
        //     System.out.println("THread 2 put "+e.switchValue+" into mailbox");
        //     Condition wakeThread = theHashMap.get(tag); //
        //     System.out.println("thread to wake is "+wakeThread.toString());
        //     wakeThread.wake();//wake thread w matching thread THATS ASLEEP
        //     theHashMap.remove(tag);
        // }
        // //new thread: i didnt exchange yet so w=f. drop in mailbox, then w=t.
        // else{ //not his patient, populate waiting room
        //     System.out.print("the extra else. assume thread 1 is awake and should get its mail ");
        //     Condition c2 = new Condition(lock);
        //     exchange = storedValue;
        //     //somehow access thread1's old value and change it. i forgot to make an exchange class for therad 1
        //     c2.sleep();
        // }
      
        // lock.release();
        // return value;
    }

    public static void test2(){
        System.out.println("threads exchanging values on different instances of Rendezvous operate independently of each other. multiple exchanges w correcttags too");
        System.out.println("r instance 1");
        final Rendezvous r = new Rendezvous();
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1; //value to switch
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv +" yeee");
            }
            });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv +"yay");
            }
            });
        t2.setName("t2"); //t1 and t2 exchange
        KThread t3 = new KThread( new Runnable () {
            public void run() {
                int tag = 4;
                int send = -200; //value to switch
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t3.setName("t3");
        KThread t4 = new KThread( new Runnable () {
            public void run() {
                int tag = 99;
                int send = -111; //value to switch
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t4.setName("t4"); //t4 doesnt exchange since no matching tag
        KThread t5 = new KThread( new Runnable () { //t5 exchange w t3
            public void run() {
                int tag = 4;
                int send = 200; //value to switch
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t5.setName("t5");
        t1.fork(); t2.fork(); t3.fork(); t4.fork(); t5.fork();
        t1.join(); t2.join(); t3.join(); t4.join(); t5.join();
//why doesn't instance 2 work?
        System.out.println("r instance 2 !!!");
        final Rendezvous r2 = new Rendezvous();
    
        KThread t6 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -10000; //value to switch
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r2.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t6.setName("t6");
        KThread t8 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 124343545;
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r2.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t8.setName("t8");
        t6.fork(); t8.fork();
        t6.join(); t8.join();
    }

    // Place Rendezvous test code inside of the Rendezvous class.

    public static void rendezTest1() {
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1; //value to switch
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                //Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t2.setName("t2");
    
        t1.fork(); t2.fork();
        // assumes join is implemented correctly
        t1.join(); t2.join();
        }
    
        // Invoke Rendezvous.selfTest() from ThreadedKernel.selfTest()
    
        public static void selfTest() {
        // place calls to your Rendezvous tests that you implement here
        rendezTest1();
        //test2();
        }

  
}
