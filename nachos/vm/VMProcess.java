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
		

		// //if( demanded page found ) return super.loadSections();
		// //starting case. check no pages in memory. page fault to laod 1st page into memory
		// //if(freepagelist not empty) return super.loadSections()
		// UserKernel.pageLock.acquire(); 
		// // load sections.keep same?
		// for (int s = 0; s < coff.getNumSections(); s++) {
		// 	CoffSection section = coff.getSection(s);
		// 	Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		// 			+ " section (" + section.getLength() + " pages)");
		// 	boolean isReadOnly = section.isReadOnly();

		// 	for (int i = 0; i < section.getLength(); i++) {
		// 		int vpn = section.getFirstVPN() + i;
		// 		int availPage = UserKernel.freePages.pollLast();
		// 		pageTable[vpn] = new TranslationEntry(vpn, availPage , false, isReadOnly, false, false);//
		// 		section.loadPage( i, availPage ); //loads into physical memory
		// 	}
		// }
		// // allocate stack and argument pages. 0 filled for part 1. ppn=-1 part 2?
		// for (int i = numPages - 1; i >= numPages - 9; i--) {
		// 	//load demanded page into available free page frame
		// 	int freePage = UserKernel.freePages.pop();
		// 	pageTable[i] = new TranslationEntry(i, freePage , true, false, false, false);
		// }

		// //ELSE no free page frame, evict a victim page 
		// //page replacement clockwise algorithm (here or handleException?)
		// 	//use for loop
		// 	//if(used==true) used=false //2nd chance to pg in case it being used
		// 	//else evict this victim page, store location next to evicted pg to start next iteration here
		// //if( victimPage is dirty ) PART 2 swap out; WRITE to swap file. 
		// 	//unset validbit? entry vpn ppn useless. use ppn to store swap pg number in swap file
		// 	//aka vpn to spn mapping in pgtable
		// 	//notify owner process (current process requesting page) when dirty pg evicted
		// 	//update pg table entry of op to mark victim page invalid. usedbit=false
		// 	//access page table of op by ipt? do we make ipt in kernel or process?
		// File.write( 2*pageSize, memory, paddr, pageSize); //add it to swapFileList
		// 	//PART 3 optimized to only when page u wanna evict is dirty
		// //else dirty=false, orig pg in coff already so super.loadSections()?
		// // SWAP IN IF PG TO BE ACCESSED ALREADY RESIDES IN SWAP FILE (SWAPPED OUT EARLIER).
		// File.read( 2*pageSize, memory, paddr, pageSize);
		// //swap using StubFileSystem.java calls
		// ThreadedKernel.fileSystem.open();
		// //read/write
		// //close
		// ThreadedKernel.fileSystem.remove();
		// //close/remove swap file before terminate kernel. call vmkernel or userkernel

		// //invalidate pge table entry of victim
		// UserKernel.pageLock.release(); 
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
				//convert to vpn
				int vpn = virtualAddr / pageSize;
				int freePage = UserKernel.freePages.pop();
							pageTable[vpn].ppn = freePage;
				if( freePages.size() == 0 ){ //NO FREE PAGES. follow psuedocode. p2 still 1 process no lock yet. goal:s swap 1 process
					//pick pg to evict
					//clock alg lecture vid(3 of which physical pg to evict). need 2nd data struct track pages saved on disk
					//filesystemfunctions
				}
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

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';

	//changes here
	// protected TranslationEntry[] invertedPageTable; 
	//which process the evicted pg base on ppn. belonged to invalidate (set valid=false og translation entry) that pg table entry?
} //invalidate the evicted pg (already in the table)
