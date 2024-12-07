package nachos.vm;

import java.util.LinkedList;
import java.util.Set;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		swapFreePages = new LinkedList<Integer>();
		numPhysPages = Machine.processor().getNumPhysPages();
		swapFile = ThreadedKernel.fileSystem.open("swapFile.txt", true);
		invertedTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++) {
    		invertedTable[i] = new TranslationEntry(-1, i, false, false, false, false); // int vpn, int ppn, boolean valid, boolean readOnly, boolean used, boolean dirty
		}
		hand = 0;
		for(int i=0; i<numSwapPages; i++) {
			swapFreePages.add(i);
		}
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
		swapFile.close();
	}

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';

	public static OpenFile swapFile;

	// MAY WANT TO LOCK THIS
	public static LinkedList<Integer> swapFreePages;

	private int numSwapPages = 2048;

	// maps table[ppn] = TranslationEntry
	public static TranslationEntry[] invertedTable;

	public static int hand;

	public static int numPhysPages;
}
