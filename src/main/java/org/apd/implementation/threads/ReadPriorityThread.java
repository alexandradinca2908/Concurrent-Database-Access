package org.apd.implementation.threads;

import org.apd.executor.StorageTask;
import org.apd.implementation.ThreadPool;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class ReadPriorityThread extends ReadWriteThread {
	private static ArrayList<Integer> readers = null;
	private static ArrayList<Object> readersMutex = null;
	private static ArrayList<Semaphore> readWrite = null;

	public ReadPriorityThread(ThreadPool threadPool) {
		super(threadPool);

		if (readers == null) {
			initSynchronizations();
		}
	}

	@Override
	public void initSynchronizations() {
		readers = new ArrayList<>();
		readersMutex = new ArrayList<>();
		readWrite = new ArrayList<>();

		for (int i = 0; i < this.threadPool.sharedDatabase.getSize(); i++) {
			readers.add(0);
			readersMutex.add(new Object());
			readWrite.add(new Semaphore(1));
		}
	}

	@Override
	public void reader(StorageTask task) throws InterruptedException {
		if (task.index() < this.threadPool.sharedDatabase.getSize()) {
			//  Increase the number of readers
			synchronized (readersMutex.get(task.index())) {
				int nrReaders = readers.get(task.index());
				readers.set(task.index(), ++nrReaders);

				//  First reader acquires memory so that writers can't enter this cell
				if (nrReaders == 1) {
					readWrite.get(task.index()).acquire();
				}
			}

			//  Read from database
			this.threadPool.entryResults.add(this.threadPool.sharedDatabase.getData(task.index()));

			//  Finish reading process
			synchronized (readersMutex.get(task.index())) {
				int nrReaders = readers.get(task.index());
				readers.set(task.index(), --nrReaders);

				//  First reader acquires memory so that writers can't enter
				if (nrReaders == 0) {
					readWrite.get(task.index()).release();
				}
			}
		}
	}

	@Override
	public void writer(StorageTask task) throws InterruptedException {
		if (task.index() < this.threadPool.sharedDatabase.getSize()) {
			//  Enter database
			//  Only one writer at a time can enter a cell
			readWrite.get(task.index()).acquire();

			//  Write in database
			this.threadPool.entryResults.add(this.threadPool.sharedDatabase.addData(task.index(), task.data()));

			//  Writer releases memory for others to enter
			readWrite.get(task.index()).release();
		}
	}

	@Override
	public void releaseMemory() {
		readers = null;
		readersMutex = null;
		readWrite = null;
	}
}
