package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.LinkedList;

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
		pageTable = new TranslationEntry[numPages]; 
		// if( numPages > UserKernel.freePages.size() ){
		// 	coff.close();
		// 	Lib.debug(dbgProcess, "\tinsufficient physical memory"); 
		// 	return false;
		// }
		//for loop thru page table not coff sections
		for (int i = 0; i < numPages; i++){
			// entry.vpn now stores swap page number (suggested by tips)
	 		pageTable[i] = new TranslationEntry(-1, -1, false, false, false, false);
		}
		 return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
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
					while(victimEntry.used) {
						victimEntry.used = false;
						VMKernel.hand = (VMKernel.hand + 1) % VMKernel.numPhysPages;
						victimEntry = VMKernel.invertedTable[VMKernel.hand];
					}
					// evict the victim page to the swap file and mark as invalid
					if (VMKernel.swapFreePages.size() <= 0) {
						Lib.debug(dbgProcess, "\tinsufficient space in our swap file!");
						super.handleException(cause);
					}
					int ppn = victimEntry.ppn;
					int spn = VMKernel.swapFreePages.poll();
					int paddr = ppn * pageSize;
					int saddr = spn * pageSize;
					VMKernel.swapFile.write(saddr, memory, paddr, pageSize);
					victimEntry.vpn = spn;
					victimEntry.ppn = -1;
					victimEntry.valid = false;
					// our faulted page can now use the freed up memory
					UserKernel.freePages.add(ppn);
					VMKernel.hand = (VMKernel.hand + 1) % VMKernel.numPhysPages;
				}
				// fill our faulted page with data
				boolean isCoff = false;
				int freePage = UserKernel.freePages.pop();
				pageTable[vpn].ppn = freePage;
				VMKernel.invertedTable[freePage] = pageTable[vpn];
				// if our page is stored in swap file (already loaded before), load it from there
				int swapPointer = pageTable[vpn].vpn;
				if (swapPointer != -1) {
					byte[] pageData = new byte[pageSize];
					VMKernel.swapFile.read(swapPointer * pageSize, pageData, 0, pageSize);
					System.arraycopy(pageData, 0, memory, freePage * pageSize, pageSize);
					VMKernel.swapFreePages.add(swapPointer);
					pageTable[vpn].vpn = -1;
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
				break; 
										
		default:
			System.out.println("in default ");
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

// else evict a memory page using clk and then put our page there
				/*if( freePages.size() == 0 ){ //NO FREE PHYSICAL PAGES. follow psuedocode.
					//pick pg to evict: 
					for( int i = 0; i < physicalPages.size() - 1; i++ ){//full loop clock algo. track translation entry
					//get part 2 working: set dirty = true anytime u load a page. if below was part 3
						if(physicalPages.get(hand).used == false){ //unused. EVICTION LOGIC
							if(physicalPages.get(hand).dirty == true ){ //only acccess at hand
								int spn = getSPN(); 
								physicalPages.get(hand) = swapFile.write( i*pageSize, mem); //keep data of used page. swap out.
								physicalPages.get(hand).valid = false; //page is BEING USED
								physicalPages.get(hand).ppn = physicalPages.get(hand).getSPN(); //???
								//swap page numbers. spn like ppn method data struct. reallocate spn to pages that return
								//if old page gets freed up, add it to spnList
								if(physical.get(i).valid == true){//page FREED UP. swap in
									physicalPages.get(hand) = swapFile.read( i*pageSize,);
									spnSize = 
								}
								//spn diff for each page. allocate 0,1,2,3. 2 is freed. data struct track availpages 2 free, the rest busy. when no availpages add more
								//data struct to track free spn
								//write page into swap file with that spn*pagesize. save spn so u can read from correct place in swapfile.
							}
							else{ //part 2 assume this never runs
								physicalPages.get(hand).ppn = -1; //page not in swap file
								//ppn tracks if we swapped out earlier. if so swap in (read from swapfile)
							}
							freePages.add(hand);
							break;//found a free page be done
						}
						else{
							physicalPages.get(hand).used = false;//if used dont evict yet. 
						}
						
						hand++;
						if(hand >= freePages.size()) hand = 0;
						//else proceed w replacement since original page already in coff  
					}
				}*/

	/* public int getSPN(){
		if(spnList.size() == 0){
			spnSize++;
			spnList.add(spnSize);
		}
		return spnSize;
	} */