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
        if ( ! theHashMap.isEmpty() ) waiting = true; //
        if( waiting == true ){ 
            Condition c = theHashMap.get(tag); //use tag to get matching tag's condition
            exchangeState e = new exchangeState( value , c); //access switchvalue from condition
            System.out.println("e "+e.switchValue);
            exchange = e.switchValue;
            e.condition.wake();//null
            theHashMap.remove(tag);
        }
        else{ //matching tag not exist yet. putting into the hashmap
            // thread calling exchange waits until there's a matching tag in the hashmap
            Condition c = new Condition(lock);
            exchange = value;
            theHashMap.put(value ,c ); //putting tag, condition. 
            c.sleep();
            
        }
        
        lock.release();
        return exchange;
        
    }

    //refs to thread since i cant assign tag into a thread.
    public static void test1tag(){
        System.out.println("test if a and b exchange values. 1 tag only");
       
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
        }

  
}
