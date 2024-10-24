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
    private Condition condition;
    private boolean waiting;
    private static int storedValue;
    HashMap<Integer, Condition> theHashMap = new HashMap<Integer, Condition>();
    //key=tag, value=condition
    /**
     * Allocate a new Rendezvous. initialize ds 
     */

    private class exchangeState { //
        
        int switchValue; //switching
        Condition condition;

        public exchangeState(int switchValue, Condition condition){
            this.switchValue = switchValue;
            this.condition = condition;
        }
    }

    public Rendezvous () {
        lock = new Lock();
        condition = new Condition(lock);
        theHashMap.put(null, condition); // where do i put 
        waiting = false;
        storedValue = 0;
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
        int exchange = 0; //new
	    lock.acquire();
        if( theHashMap.isEmpty() ){ //nothing in hashmap. no one in wait room
            System.out.print("enter hashmap empty condtion");
            Condition c = new Condition(lock);
            exchange = value;
            storedValue = value; //t1 puts val1 in mailbox
            System.out.print("thread1 exchange is: "+exchange);
            System.out.print("thread 1 puts "+storedValue+" in mailbox");
            theHashMap.put(tag ,c ); //populate waiting room
            c.sleep();
        }
        if ( ! theHashMap.isEmpty() ) { //there r patients in the wait room
            Condition c = theHashMap.get(tag); //is this dr. mario's patient? c is not null if matching tag
            if( c != null ) waiting = true; // yes it's his patient
            else{ //not his patient, populate waiting room
                Condition c2 = new Condition(lock);
                exchange = value;
                theHashMap.put(tag ,c2 ); 
                c2.sleep();
            }
            if( waiting == true ){ //matching tag. treat the patient
            exchangeState e = new exchangeState( value , c); //access switchvalue from condition
            System.out.println("e "+e.switchValue);
            exchange = storedValue; //t2 gets val1 from mailbox. 
            storedValue = e.switchValue; //t2 puts val2 in mailbox
            e.condition.wake();//?
            theHashMap.remove(tag);
        } //t1 tag=0 value=-1    t2 tag=0 value=1    var 
    }
        lock.release();
        return exchange;
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
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
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
