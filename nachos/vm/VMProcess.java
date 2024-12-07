package nachos.vm;


import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;


/**
* A <tt>UserProcess</tt> that supports demand-paging.
*/
public class VMProcess extends UserProcess {
   /**
    * Allocate a new process.
    */
    public VMProcess() {
        super();
    }

    /**
    * Save the state of this process in preparation for a context switch.
    * Called by <tt>UThread.saveState()</tt>.
    */
    public void saveState() {
        super.saveState();
    }


    /**
    * Restore the state of this process after a context switch. Called by
    * <tt>UThread.restoreState()</tt>.
    */
    public void restoreState() {
        super.restoreState();
    }

   /**
    * Initializes page tables for this process so that the executable can be
    * demand-paged.
    *
    * @return <tt>true</tt> if successful.
    */
    protected boolean loadSections() {
        super.loadSections();
        //printPageTable();
        return true;
    }

    /**
    * Release any resources allocated by <tt>loadSections()</tt>.
    */
    protected void unloadSections() {
        super.unloadSections();
        for (TranslationEntry entry: pageTable) {
            if (entry.valid) {
                // free allocated physical pages
                if (entry.ppn != -1) {
                    UserKernel.freePages.addFirst(entry.ppn);
                }
                else if (entry.vpn != -1) {
                    // free swap file pages
                    VMKernel.swapFreePages.addFirst(entry.vpn);
                }
            }
        }
    }


    /**
    * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
    * . The <i>cause</i> argument identifies which exception occurred; see the
    * <tt>Processor.exceptionZZZ</tt> constants.
    * try to use page but its invalid.
    * load pages in here on demand ONLY FOR PG EXCEPTIONS. HANDLE NORMAL CASE in loadsections
    * @param cause the user exception that occurred.
    */
    public void handleException(int cause) {
        Processor processor = Machine.processor();
        switch (cause) {
            case Processor.exceptionPageFault:
                //System.out.println("Process " + getProcessID() + " acquiring pageLock in handleException!");
                UserKernel.pageLock.acquire();
                // where we are virtually
                byte[] memory = Machine.processor().getMemory();
                int vaddr = processor.readRegister(Processor.regBadVAddr);
                int vpn = vaddr / pageSize;
                //System.out.println("Handling pageFault for vpn: " + vpn);
                //printPageTable();
                //printInvertedTable();
                // if there's not enough space, swap a physical page to disk and use it
                if (UserKernel.freePages.size() <= 0) {
                    // use NRU clock algorithm to find page to replace
                    TranslationEntry victimEntry = VMKernel.invertedTable[VMKernel.hand];
                    while(victimEntry.used || VMKernel.isPinned(victimEntry.ppn)) {
                        victimEntry.used = false;
                        VMKernel.hand = (VMKernel.hand + 1) % VMKernel.numPhysPages;
                        victimEntry = VMKernel.invertedTable[VMKernel.hand];
                    }
                    int ppn = victimEntry.ppn;
					//only need to use swap file if its dirty
					if (victimEntry.dirty) {
						// evict the victim page to the swap file and mark as invalid
						if (VMKernel.swapFreePages.size() <= 0) {
							Lib.debug(dbgProcess, "\tinsufficient space in our swap file!");
							super.handleException(cause);
						}

                        VMKernel.pinPage(ppn); //dont want it evicted when lock is released
                        UserKernel.pageLock.release(); //release for IO

						int spn = VMKernel.swapFreePages.poll();
						int paddr = ppn * pageSize;
						int saddr = spn * pageSize;
                        //Lib.debug(dbgVM, "Swapping out page " + victimEntry.ppn + " to swapFile at " + saddr);
						VMKernel.swapFile.write(saddr, memory, paddr, pageSize);
                        Machine.incrNumSwapWrites();
						victimEntry.vpn = spn;

                        UserKernel.pageLock.acquire(); //acquire after IO
                        VMKernel.unpinPage(ppn);
                    }
					victimEntry.ppn = -1;
                    victimEntry.valid = false; //set this to false no matter if its dirty or not
                    //victimEntry.vpn = -1;

                    // our faulted page can now use the freed up memory
                    UserKernel.freePages.addFirst(ppn);
                    VMKernel.hand = (VMKernel.hand + 1) % VMKernel.numPhysPages;
                }
                // fill our faulted page with data
                boolean isCoff = false;
                int freePage = UserKernel.freePages.poll();
                pageTable[vpn].ppn = freePage;
                VMKernel.invertedTable[freePage] = pageTable[vpn];
                // if our page is stored in swap file (already loaded before), load it from there
                int swapPointer = pageTable[vpn].vpn;
                if (swapPointer != -1) {
                    byte[] pageData = new byte[pageSize];

                    VMKernel.pinPage(freePage);
                    UserKernel.pageLock.release(); //realease for IO

                    VMKernel.swapFile.read(swapPointer * pageSize, pageData, 0, pageSize);
                    Machine.incrNumSwapReads();
                    System.arraycopy(pageData, 0, memory, freePage * pageSize, pageSize);
                    VMKernel.swapFreePages.addFirst(swapPointer);
                    pageTable[vpn].vpn = -1;

                    UserKernel.pageLock.acquire(); //acquire after IO
                    VMKernel.unpinPage(freePage);
                }
                // first time initializing this page
                else {
                    //if our page matches a coffSection, load it to the coff
                    for (int s = 0; s < coff.getNumSections(); s++) {
                        CoffSection section = coff.getSection(s);
                        for (int i = 0; i < section.getLength(); i++) {
                            if( vpn == section.getFirstVPN() + i ){//coff
                                section.loadPage( i, pageTable[vpn].ppn );
                                isCoff = true;
                                Machine.incrNumCOFFReads();
                            }
                        }
                    }
                    //it's a stack page, 0 fill that physical page
                    if( !isCoff ){
                        byte[] data = new byte[pageSize];
                        int paddr = pageTable[vpn].ppn * pageSize;
                        //zero out the page in physical memory
                        System.arraycopy( data, 0, memory, paddr, pageSize );
                    }
                }
                pageTable[vpn].valid = true;
				//System.out.println("Process " + getProcessID() + " releasing pageLock in handleException!");
                UserKernel.pageLock.release();
                break;

            default:
            //System.out.println("in VM Process default ");
            super.handleException(cause);
            break;
        }
    }


    public void printPageTable(){
        System.out.println("Page Table:");
        for (int i=0; i< pageTable.length; i++) {
            TranslationEntry entry = pageTable[i];
            System.out.print("key: " + i);
            System.out.print(" vpn: " + entry.vpn);
            System.out.print(" ppn: " + entry.ppn);
            System.out.print(" valid: " + entry.valid);
            System.out.print(" readOnly: " + entry.readOnly);
            System.out.print(" used: " + entry.used);
            System.out.print(" dirty: " + entry.dirty);
            System.out.println();
        }
    }


    public void printInvertedTable(){
        System.out.println("Inverted Table:");
        for (int i=0; i< VMKernel.invertedTable.length; i++) {
            TranslationEntry entry = VMKernel.invertedTable[i];
            System.out.print("key: " + i);
            if(entry != null) {
                System.out.print(" vpn: " + entry.vpn);
                System.out.print(" ppn: " + entry.ppn);
                System.out.print(" valid: " + entry.valid);
                System.out.print(" readOnly: " + entry.readOnly);
                System.out.print(" used: " + entry.used);
                System.out.print(" dirty: " + entry.dirty);
            } else {
                System.out.print(" NULL");
            }
            System.out.println();
        }
    }


    private static final int pageSize = Processor.pageSize;


    private static final char dbgProcess = 'a';


    private static final char dbgVM = 'v';
}