package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

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
		if( numPages > UserKernel.freePages.size() ){
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory"); 
			return false;
		}
		//for loop thru page table not coff sections
		for (int i = 0; i < numPages; i++){
			//int freePage = UserKernel.freePages.pop(); //-1
	 		pageTable[i] = new TranslationEntry(i, -1, false, false, false, false);
			//preallocate pages and set valid bit to false
			//go to handle exception
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
		boolean isCoff = false;
		Processor processor = Machine.processor();
		switch (cause) {
			case Processor.exceptionPageFault:
				// System.out.println("in exception page fault");
				//UserKernel.pageLock.acquire(); 
				//load single pg that caused exception
				int virtualAddr = processor.readRegister(Processor.regBadVAddr);//doesnt match to phys pg#
				int vpn = virtualAddr / pageSize;
				if( freePages.size() == 0 ){ //NO FREE PHYSICAL PAGES. follow psuedocode.
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
				}
				int freePage = UserKernel.freePages.pop();
							pageTable[vpn].ppn = freePage;
				//loop thru all coff section, see if vpn is in that coff section. if so load from that coff section.
				for (int s = 0; s < coff.getNumSections(); s++) {
					CoffSection section = coff.getSection(s);
					for (int i = 0; i < section.getLength(); i++) {

						if( vpn == section.getFirstVPN() + i ){//coff
							
							section.loadPage( i, pageTable[vpn].ppn );
							pageTable[vpn].valid = true;
							isCoff = true;
						}
					}
				}
				if( isCoff == false ){ //it's a stack page, 0 fill that physical page
					pageTable[vpn].valid = true;
					//is something in swap file. if so load page from the disk
					byte[] memory = Machine.processor().getMemory();
					byte[] data = new byte[pageSize];
					int paddr = pageTable[vpn].ppn * pageSize;
					System.arraycopy( memory, paddr, data, 0, pageSize );
					//zero out pg size elements in that array
				}
				break; 
										
		default:
			// System.out.println("in default ");
			super.handleException(cause);
			break;
		}
	}
	/**
	 * add new swap page number to the list.
	 *  returns an available swap page number
	 */
	public int getSPN(){
	if(spnList.size() == 0){
		spnSize++;
		spnList.add(spnSize);
	}
	return spnSize;
}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';

	private static int hand = 0;

	private static LinkedList<TranslationEntry> physicalPages;

	private static OpenFile swapFile;

	private static int spnSize = -1; //increment each time u add new spn. method that does both

	private static LinkedList<Integer> spnList; //if spnList empty add new spn=spnsize

	//changes here
	// protected TranslationEntry[] invertedPageTable; 
	//which process the evicted pg base on ppn. belonged to invalidate (set valid=false og translation entry) that pg table entry?
} //invalidate the evicted pg (already in the table)
