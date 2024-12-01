package org.apd.implementation.threads;

import org.apd.executor.LockType;
import org.apd.executor.StorageTask;
import org.apd.implementation.ThreadPool;
import org.apd.implementation.locks.Lock;

import java.util.ArrayList;

public class WritePriorityThread extends ReadWriteThread {
	private static ArrayList<Integer> readers = null;
	private static ArrayList<Integer> writers = null;
	private static ArrayList<Integer> waitingReaders = null;
	private static ArrayList<Integer> waitingWriters = null;
	private static ArrayList<Lock> readerWaitingQueue = null;
	private static ArrayList<Lock> writerWaitingQueue = null;
	private static ArrayList<Lock> enter = null;
	private static LockType lockType;

	public WritePriorityThread(ThreadPool threadPool, LockType lockType) {
		super(threadPool);

		if (readers == null) {
			WritePriorityThread.lockType = lockType;
			initSynchronizations();
		}
	}

	@Override
	public void initSynchronizations() {
		readers = new ArrayList<>();
		writers = new ArrayList<>();
		waitingReaders = new ArrayList<>();
		waitingWriters = new ArrayList<>();
		readerWaitingQueue = new ArrayList<>();
		writerWaitingQueue = new ArrayList<>();
		enter = new ArrayList<>();

		for (int i = 0; i < this.threadPool.sharedDatabase.getSize(); i++) {
			readers.add(0);
			writers.add(0);
			waitingReaders.add(0);
			waitingWriters.add(0);
			readerWaitingQueue.add(Lock.createLock(lockType, 0));
			writerWaitingQueue.add(Lock.createLock(lockType, 0));
			enter.add(Lock.createLock(lockType, 1));
		}
	}

	@Override
	public void reader(StorageTask task) throws InterruptedException {
		if (task.index() < this.threadPool.sharedDatabase.getSize()) {
			int index = task.index();

			enter.get(index).lock();

			//  Reader waits if writer is in memory area or waits for it
			if (writers.get(index) > 0 || waitingWriters.get(index) > 0) {
				waitingReaders.set(index, waitingReaders.get(index) + 1);
				enter.get(index).unlock();

				//  Acquire monitor and enter waiting state
				readerWaitingQueue.get(index).lock();
			}

			readers.set(index, readers.get(index) + 1);

			//  New reader joins database memory area after waiting
			if (waitingReaders.get(index) > 0) {
				waitingReaders.set(index, waitingReaders.get(index) - 1);

				//  Acquire monitor and release another reader
				readerWaitingQueue.get(index).unlock();

				//  No need to wait as thread is the first in queue
			} else if (waitingReaders.get(index) == 0) {
				enter.get(index).unlock();
			}

			//  Read data
			this.threadPool.entryResults.add(this.threadPool.sharedDatabase.getData(index));

			//  Secure critical area
			enter.get(index).lock();
			readers.set(index, readers.get(index) - 1);

			//  Allow writer to enter if thread is the last EXECUTING reader
			if (readers.get(index) == 0 && waitingWriters.get(index) > 0) {
				waitingWriters.set(index, waitingWriters.get(index) - 1);

				//  Acquire monitor and release a random writer
				writerWaitingQueue.get(index).unlock();

				//  Or simply release critical area semaphore for WAITING readers
			} else if (readers.get(index) > 0 || waitingWriters.get(index) == 0) {
				enter.get(index).unlock();
			}
		}
	}

	@Override
	public void writer(StorageTask task) throws InterruptedException {
		if (task.index() < this.threadPool.sharedDatabase.getSize()) {
			int index = task.index();

			enter.get(index).lock();

			//  Writer waits if readers or writers are in memory area
			if (readers.get(index) > 0 || writers.get(index) > 0) {
				waitingWriters.set(index, waitingWriters.get(index) + 1);
				enter.get(index).unlock();

				//  Acquire monitor and enter waiting state
				writerWaitingQueue.get(index).lock();
			}

			writers.set(index, writers.get(index) + 1);

			enter.get(index).unlock();

			//  Write data
			this.threadPool.entryResults.add(this.threadPool.sharedDatabase.addData(task.index(), task.data()));

			//  Secure critical area
			enter.get(index).lock();
			writers.set(index, writers.get(index) - 1);

			//  If no writer is waiting, let readers in
			if (waitingReaders.get(index) > 0 && waitingWriters.get(index) == 0) {
				waitingReaders.set(index, waitingReaders.get(index) - 1);

				//  Acquire monitor and release a random reader
				readerWaitingQueue.get(index).unlock();

				//  Otherwise, prioritize writer entry
			} else if (waitingWriters.get(index) > 0) {
				waitingWriters.set(index, waitingWriters.get(index) - 1);

				//  Acquire monitor and release another writer
				writerWaitingQueue.get(index).unlock();

				//  When no one is in queue, release entry for any type of thread
			} else if (waitingReaders.get(index) == 0 && waitingWriters.get(index) == 0) {
				enter.get(index).unlock();
			}
		}
	}

	@Override
	public void releaseMemory() {
		readers = null;
		writers = null;
		waitingReaders = null;
		waitingWriters = null;
		readerWaitingQueue = null;
		writerWaitingQueue = null;
		enter = null;
	}
}
