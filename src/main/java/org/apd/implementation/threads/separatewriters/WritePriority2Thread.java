package org.apd.implementation.threads.separatewriters;

import org.apd.executor.StorageTask;
import org.apd.implementation.ThreadPool;
import org.apd.implementation.threads.ReadWriteThread;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class WritePriority2Thread extends ReadWriteThread {
	private static ArrayList<Integer> readers = null;
	private static ArrayList<Integer> writers = null;
	private static ArrayList<Integer> waitingReaders = null;
	private static ArrayList<Integer> waitingWriters = null;
	private static ArrayList<Object> readerWaitingQueue = null;
	private static ArrayList<Object> writerWaitingQueue = null;
	private static ArrayList<Semaphore> enter = null;

	public WritePriority2Thread(ThreadPool threadPool) {
		super(threadPool);

		if (readers == null) {
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
			readerWaitingQueue.add(new Object());
			writerWaitingQueue.add(new Object());
			enter.add(new Semaphore(1));
		}
	}

	@Override
	public void reader(StorageTask task) throws InterruptedException {
		if (task.index() < this.threadPool.sharedDatabase.getSize()) {
			int index = task.index();

			enter.get(index).acquire();

			//  Reader waits if writer is in memory area or waits for it
			if (writers.get(index) > 0 || waitingWriters.get(index) > 0) {
				waitingReaders.set(index, waitingReaders.get(index) + 1);
				enter.get(index).release();

				//  Acquire monitor and enter waiting state
				Object lock = readerWaitingQueue.get(index);
				synchronized (lock) {
					lock.wait();
				}
			}

			readers.set(index, readers.get(index) + 1);

			//  New reader joins database memory area after waiting
			if (waitingReaders.get(index) > 0) {
				waitingReaders.set(index, waitingReaders.get(index) - 1);

				//  Acquire monitor and release another reader
				Object lock = readerWaitingQueue.get(index);
				synchronized (lock) {
					lock.notify();
				}


			//  No need to wait as thread is the first in queue
			} else if (waitingReaders.get(index) == 0) {
				enter.get(index).release();
			}

			//  Read data
			this.threadPool.entryResults.add(this.threadPool.sharedDatabase.getData(index));

			//  Secure critical area
			enter.get(index).acquire();
			readers.set(index, readers.get(index) - 1);

			//  Allow writer to enter if thread is the last EXECUTING reader
			if (readers.get(index) == 0 && waitingWriters.get(index) > 0) {
				waitingWriters.set(index, waitingWriters.get(index) - 1);

				//  Acquire monitor and release a random writer
				Object lock = writerWaitingQueue.get(index);
				synchronized (lock) {
					lock.notify();
				}

				//  Or simply release critical area semaphore for WAITING readers
			} else if (readers.get(index) > 0 || waitingWriters.get(index) == 0) {
				enter.get(index).release();
			}
		}
	}

	@Override
	public void writer(StorageTask task) throws InterruptedException {
		if (task.index() < this.threadPool.sharedDatabase.getSize()) {
			int index = task.index();

			enter.get(index).acquire();

			//  Writer waits if readers or writers are in memory area
			if (readers.get(index) > 0 || writers.get(index) > 0) {
				waitingWriters.set(index, waitingWriters.get(index) + 1);
				enter.get(index).release();

				//  Acquire monitor and enter waiting state
				Object lock = writerWaitingQueue.get(index);
				synchronized (lock) {
					writerWaitingQueue.get(index).wait();
				}

			}

			writers.set(index, writers.get(index) + 1);

			enter.get(index).release();

			//  Write data
			this.threadPool.entryResults.add(this.threadPool.sharedDatabase.addData(task.index(), task.data()));

			//  Secure critical area
			enter.get(index).acquire();
			writers.set(index, writers.get(index) - 1);

			//  If no writer is waiting, let readers in
			if (waitingReaders.get(index) > 0 && waitingWriters.get(index) == 0) {
				waitingReaders.set(index, waitingReaders.get(index) - 1);

				//  Acquire monitor and release a random reader
				Object lock = readerWaitingQueue.get(index);
				synchronized (lock) {
					lock.notify();
				}

			//  Otherwise, prioritize writer entry
			} else if (waitingWriters.get(index) > 0) {
				waitingWriters.set(index, waitingWriters.get(index) - 1);

				//  Acquire monitor and release another writer
				Object lock = writerWaitingQueue.get(index);
				synchronized (lock) {
					lock.notify();
				}

				//  When no one is in queue, release entry for any type of thread
			} else if (waitingReaders.get(index) == 0 && waitingWriters.get(index) == 0) {
				enter.get(index).release();
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
