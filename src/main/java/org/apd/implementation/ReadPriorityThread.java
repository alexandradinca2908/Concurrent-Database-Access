package org.apd.implementation;

import org.apd.executor.StorageTask;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public class ReadPriorityThread extends ReadWriteThread {
	private static int readers = 0;
	private static final Semaphore readWrite = new Semaphore(1);

	ReadPriorityThread(BlockingQueue<StorageTask> tasks, SharedDatabase sharedDatabase,
					   BlockingQueue<EntryResult> entryResults) {
		super(tasks, sharedDatabase, entryResults);
	}

	@Override
	public void reader(StorageTask task) throws InterruptedException {
		//  Increase the number of readers
		synchronized (ReadPriorityThread.class) {
			readers++;

			//  First reader acquires memory so that writers can't enter
			if (readers == 1) {
				readWrite.acquire();
			}
		}

		//  Read from database
		entryResults.add(sharedDatabase.getData(task.index()));

		//  Finish reading process
		synchronized (ReadPriorityThread.class) {
			readers--;

			//  Last reader releases memory for writers to enter
			if (readers == 0) {
				readWrite.release();
			}
		}
	}

	@Override
	public void writer(StorageTask task) throws InterruptedException {
		//  Enter database
		//  Only one writer at a time can enter
		readWrite.acquire();

		//  Write in database
		entryResults.add(sharedDatabase.addData(task.index(), task.data()));

		//  Writer releases memory for others to enter
		readWrite.release();
	}
}
