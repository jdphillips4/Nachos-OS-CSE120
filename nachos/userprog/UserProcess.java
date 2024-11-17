//Userprocess
package nachos.userprog;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.io.EOFException;
import java.util.List;


import java.util.ArrayList;

//import javax.annotation.processing.Processor;



/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		fileDescriptorTable = new OpenFile[MAX_FILES];
		fileDescriptorTable[0] = UserKernel.console.openForReading();
		fileDescriptorTable[1] = UserKernel.console.openForWriting();
		children = new ArrayList<UserProcess>();//do i need the type UserProcess
		parent = null;
		pid = UserKernel.nextID;
		UserKernel.nextID++;
	 }

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
	        String name = Machine.getProcessClassName ();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals ("nachos.userprog.UserProcess")) {
		    return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
		    return new VMProcess ();
		} else {
		    return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		currentProcess = this; //set current process
		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		// update dirty and used
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
		byte[] memory = Machine.processor().getMemory();
		if (vaddr < 0 || vaddr >= numPages * pageSize) {
			return 0;
		}
		int read = 0;			// how much we have read so far
		while(read < length) {
			int vpage = vaddr / pageSize;
			int voffset = vaddr % pageSize;
			TranslationEntry entry = pageTable[vpage];
			int ppn = entry.ppn;
			if (!entry.valid) return 0;
			int paddr = ppn * pageSize + voffset;
			int amount = Math.min(length - read, pageSize - voffset);
			//Lib.debug(dbgProcess, "vaddr: " + vaddr);
			//Lib.debug(dbgProcess, "paddr: " + paddr);
			try {
				System.arraycopy(memory, paddr, data, offset + read, amount);
			} catch (IndexOutOfBoundsException exception) {
				return read;
			}
			read += amount;
			vaddr += amount;
			pageTable[vpage].used = true;
		}
		return read;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		// update dirty and used
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();
		if (vaddr < 0 || (vaddr + length) >= numPages * pageSize) {
			return 0;
		}

		int written = 0;
		while(written < length) {
			int vpage = vaddr / pageSize;
			int voffset = vaddr % pageSize;
			TranslationEntry entry = pageTable[vpage];
			if (!entry.valid || entry.readOnly) return 0;
			int ppn = entry.ppn;
			int paddr = ppn * pageSize + voffset;
			int amount = Math.min(length - written, pageSize - voffset);
			//Lib.debug(dbgProcess, "write vaddr: " + vaddr);
			//Lib.debug(dbgProcess, "write paddr: " + paddr);
			try {
				System.arraycopy(data, offset + written, memory, paddr, amount);
			} catch (IndexOutOfBoundsException exception) {
				return written;
			}
			written += amount;
			vaddr += amount;
			pageTable[vpage].used = true;
			pageTable[vpage].dirty = true;
		}
		return written;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			Lib.debug(dbgProcess, "found " + section.getName()
					+ " section (" + section.getLength() + " pages)");
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		Lib.debug(dbgProcess, "UserProcess.load: " + numPages + " pages in address space (" + Machine.processor().getNumPhysPages() + " physical pages)");

		/*
		 * Layout of the Nachos user process address space.
		 * The code above calculates the total number of pages
		 * in the address space for this executable.
		 *
		 * +------------------+
		 * | Code and data    |
		 * | pages from       |   size = num pages in COFF file
		 * | executable file  |
		 * | (COFF file)      |
		 * +------------------+
		 * | Stack pages      |   size = stackPages
		 * +------------------+
		 * | Arg page         |   size = 1
		 * +------------------+
		 *
		 * Page 0 is at the top, and the last page at the
		 * bottom is the arg page at numPages-1.
		 */

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		pageTable = new TranslationEntry[numPages]; //initialize pagetable (virtual) program needs. not the whole memory
		//map virtual to physical non continous too
		if( numPages > UserKernel.freePages.size() ){
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");//error if not enough free pages
			return false;
		}

		UserKernel.pageLock.acquire(); 
		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");
			boolean isReadOnly = section.isReadOnly();

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				//make it able to split memory
				// int availPage = UserKernel.freePages.pop();
				int availPage = UserKernel.freePages.pollLast();
				pageTable[vpn] = new TranslationEntry(vpn, availPage , true, isReadOnly, false, false);
				section.loadPage( i, availPage ); //loads into physical memory
			}
		}
		// allocate stack and argument pages
		//for (int i = numPages - 9; i<numPages; i++) {
		for (int i = numPages - 1; i >= numPages - 9; i--) {
			int freePage = UserKernel.freePages.pop();
			pageTable[i] = new TranslationEntry(i, freePage , true, false, false, false);
		}
		UserKernel.pageLock.release();
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		UserKernel.pageLock.acquire();
		for(TranslationEntry entry: pageTable) {
			// for every virtual page, add its physical page back
			UserKernel.freePages.add(entry.ppn);
		}
		UserKernel.pageLock.release();
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 *  only be invoked by the "root" process
	 */
	private int handleHalt() {
		if(currentProcess.getProcessID() != 0) return -1; 
		Lib.debug(dbgProcess, "UserProcess.handleHalt");

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exit() system call.
	 */
	private void handleExit(int status) {
	    // Do not remove this call to the autoGrader...
		System.out.println ("UserProcess.handleExit (" + status + ")");
		Machine.autoGrader().finishingCurrentProcess(status);
		int pid = currentProcess != null ? currentProcess.getProcessID() : -1;
		Lib.debug(dbgProcess, "exit PID: " + pid);

		// close all file descriptors
		for (int i=0; i<16; i++) {
			handleClose(i);
		}
		// any children no longer have parents
		for(UserProcess child: children) {
			child.parent = null;
		}
		// cleanup memory
		unloadSections();
		Lib.debug(dbgProcess, "check current process location: " + currentProcess);
		Lib.debug(dbgProcess, "setting Status of PID " + pid + " to " + status);
		if (currentProcess != null) setExitStatus(status);
		currentProcess = null;
		// last exiting process should invoke halt
		if(pid == 0) {
			Lib.debug(dbgProcess, "Calling terminate!");
			Kernel.kernel.terminate();
		}
		// if parent is waiting on this, allow it to run again
		if (parent != null) {
			KThread.currentThread().yield();
		}
	}

	/**
	 * Handle the open() system call
	 */
	private int handleOpen(int a0) {
		Lib.debug(dbgProcess, "args, a0: " + a0);
		String fileName = readVirtualMemoryString(a0, 256);
		OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);
		if (file == null) return -1;
		int fd = -1;
		for (int i=0; i<fileDescriptorTable.length; i++) {
			if (fileDescriptorTable[i] == null) {
				fd = i;
				break;
			}
		}
		if (fd == -1) {
			file.close();
			return -1;
		}
		fileDescriptorTable[fd] = file;
		return fd;
	}

	/**
	 * Handle the create() system call
	 */
	private int handleCreat(int a0){ //the param is char *name idk
		Lib.debug(dbgProcess, "UserProcess.handleCreat");
		if( ThreadedKernel.fileSystem.getOpenCount() == MAX_FILES ) return -1; //dont create anymore files. 
		String filename = readVirtualMemoryString(a0, 256); 	//whats the max size.
		if( filename == null ) return -1;
	
		//  create the file
		OpenFile file = ThreadedKernel.fileSystem.open(filename, true); // true for create
		if (file == null) return -1; 
	
		int fd = -1;
		for (int i = 0; i < fileDescriptorTable.length; i++) {
			if (fileDescriptorTable[i] == null) {
				fd = i;
				break;
			}
		}

		if( fd == -1) {
			file.close(); //cant store file
			return -1;
		}

		fileDescriptorTable[fd] = file;
		return fd;
	}

	/**
	 * Handle the read() system call
	 */
	private int handleRead(int a0, int a1, int a2) {
		//Lib.debug(dbgProcess, "args, a0: " + a0 + " ,a1: " + a1 + " ,a2: " + a2);
		if (a0 < 0 || a0 > 15) return -1;	// check for a valid file descriptor
		OpenFile file = fileDescriptorTable[a0];
		if (file == null) return -1;		// check for a valid file descriptor
		int read = 0;
		byte[] page = new byte[pageSize];
		while (read < a2) {
			//Lib.debug(dbgProcess, "a1 ptr: " + a1);
			int size = Math.min(pageSize, a2 - read);
			int fileRead = file.read(page, 0, size);
			//Lib.debug(dbgProcess, "fileRead: " + fileRead);
			if (fileRead == -1) return -1; // if error in reading file
			int transferred = writeVirtualMemory(a1, page, 0, fileRead);
			//Lib.debug(dbgProcess, "transferred: " + transferred);
			//Lib.debug(dbgProcess, "data: " + new String(page, 0, fileRead));
			a1 += transferred;		// move our pointer by however much we just added
			read += transferred;
			// stop when nothing more can be read
			if (transferred == 0) break;
		}
		return read;
	}

	/**
	 * Handle the write() system call
	 */
	private int handleWrite(int a0, int a1, int a2) {
		//Lib.debug(dbgProcess, "HANDLEWRITE args, a0: " + a0 + " ,a1: " + a1 + " ,a2: " + a2);
		if(a2 < 0) return -1; // can't write a negative length
		if (a0 < 0 || a0 > 15) return -1;	// check for a valid file descriptor
		OpenFile file = fileDescriptorTable[a0];
		if (file == null) return -1;		// check for a valid file descriptor

		// write in page size amounts over and over
		int written = 0; 
		int size = 0; 
		int transferred = 0;
		int pageWritten = 0;
		byte[] page = new byte[pageSize];
		while (written < a2) {
			size = Math.min(pageSize, a2 - written);
			transferred = readVirtualMemory(a1, page, 0, size);
			//Lib.debug(dbgProcess, "write transferred: " + transferred);
			//Lib.debug(dbgProcess, "page: " + page);
			if (transferred < size) return -1;
			pageWritten = file.write(page, 0, transferred);
			if (pageWritten == -1 || pageWritten < size) return -1;
			written += pageWritten;
			a1 += pageWritten;
		}
		return written;
	}

	/**
	 * Handle the close() system call
	 */
	private int handleClose(int a0) {
		//Lib.debug(dbgProcess, "UserProcess.handleClose");
		if (a0 < 0 || a0 >= fileDescriptorTable.length || fileDescriptorTable[a0] == null) {
			return -1; // Invalid file descriptor
		}
		fileDescriptorTable[a0].close();
		fileDescriptorTable[a0] = null; // Clear the file descriptor entry
		return 0;
	}

	//delete a file
	private int handleUnlink(int a0){
		Lib.debug(dbgProcess, "UserProcess.handleUnlink");
		String filename = readVirtualMemoryString(a0, 256); 
		if (filename == null||filename.length()>256) return -1;

		// Check for open files before unlink
		for (OpenFile file : fileDescriptorTable) {
			if (file != null && file.getName().equals(filename)) {
				return -1; // Cannot delete since it's still open
			}
    	}

    	boolean deleted = ThreadedKernel.fileSystem.remove(filename);
    	if (deleted==false) return -1; // File not deleted.
    	return 0;
	}

	/**
	 * exec system call both creates a new process 
	 * and loads a new program into that process. 
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return returns the child process's process ID, which can be passed to
     * join(). On error, returns -1.
	 */
	private int handleExec( int a0, int a1, int a2 ){
		String executableName = readVirtualMemoryString(a0, 256);
		if (executableName == null){
			Lib.debug(dbgProcess, "handleExec no executable name!");
			return -1;
		}; // Invalid executable name
		//do we check if the string includes .coff ??
		// Step 2: Read the argument count
		int argc = a1;
		if (argc < 0) {
			Lib.debug(dbgProcess, "handleExec bad arg count");
			return -1; // Invalid argument count
		}
		// Step 3: Read the arguments argv is an array of pointers to null-terminated strings
		String[] argv = new String[argc];
		//read virtual mem stuff
		for (int i=0; i< argc; i++) {
			byte[] ptr = new byte[4];
			readVirtualMemory(a2 + i*4, ptr); //reads 4 byte value corresponding to ith argument
			int vaddr = Lib.bytesToInt(ptr,0);
			argv[i] = readVirtualMemoryString(vaddr, 256);
			if (argv[i]==null) {
				Lib.debug(dbgProcess, "handleExec unable to read arg string");
				return -1;
			}
		}
	
		//maybe save child and parent to be used for join
		UserProcess childProcess = UserProcess.newUserProcess();
		if (childProcess == null) {
			Lib.debug(dbgProcess, "handleExec failed to make childProcess");
			return -1; // Failed to create new process
		}
		if (!childProcess.execute(executableName, argv)){
			Lib.debug(dbgProcess, "handleExec failed to run child process!");
			return -1;
		}; // Failed to execute the process
		children.add(childProcess);
		Lib.debug(dbgProcess, "childProcess: " + childProcess.pid);
		Lib.debug(dbgProcess, "currentProcess: " + currentProcess.pid);
		childProcess.parent = currentProcess;
		Lib.debug(dbgProcess, "handleExec child pid: " + childProcess.getProcessID());
		return childProcess.getProcessID(); 
	}
	/**
	 * need to implement this for handleExec
	 * @return
	 */
	private int getProcessID(){
		return pid;
	}
	/*
	 * isAlive process?
	 */
	public boolean isAlive() {
		return alive;
	}
	public void setExitStatus(int status) {
		this.exitStatus = status;
		this.alive = false; // Mark as unalive
	}
	
	public int getExitStatus() {
		return exitStatus;
	}
	/**
	 * 
	 * transfer data between kernel memory and the memory of the user process.
	 * @param a0 process id from handleExec
 	 * @param a1
	 * @return
	 */
	private int handleJoin( int a0, int a1){//int  join(int pid, int *status)
		if( a0 <= 0 ) return -1;//process ID positive integer, assigned to each process when created
		if( a1 < 0 || a1 > numPages * pageSize) return -1;
		UserProcess child = null;
		// only join to your children
		for( UserProcess u : children ){
			if ( u.getProcessID() == a0 ) { //process is done if u can get its id.
				child = u;
				break;
			}
		}
		if(child == null) return -1;
		Lib.debug(dbgProcess, "setting child of " + currentProcess.getProcessID() + " to " + child.getProcessID());
		while (child.isAlive()) {
			KThread.currentThread().yield();
		}
		int exitStatus = child.getExitStatus();
		System.out.println("ChildExitStat: " + exitStatus);
		if(exitStatus == -999) return 0;
		byte[] statusBytes = Lib.bytesFromInt(exitStatus);
		writeVirtualMemory(a1, statusBytes);
		children.remove(child);
		return 1; //  child has exited
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			handleExit(a0);
		case syscallCreate:
			return handleCreat(a0); //doesnt work since we didnt implement close yet
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		case syscallExec:
			return handleExec( a0, a1, a2);
		case syscallJoin:
			return handleJoin( a0, a1);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();
		Lib.debug(dbgProcess, "cause: " + cause);

		switch (cause) {
		case Processor.exceptionSyscall:
			// TODO: HANDLE EXCEPTION
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			handleExit(-999);
		}
	}
	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
        protected UThread thread;
    
	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final int MAX_FILES = 16; // Maximum number of open files
	private OpenFile[] fileDescriptorTable;
	private int pid;
	protected List<UserProcess> children;
	protected UserProcess parent;
	protected boolean alive = true; 
    protected int exitStatus;
	protected UserProcess currentProcess;

}