package org.apd.implementation;

import org.apd.executor.StorageTask;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public abstract class ReadWriteThread implements Runnable {
	private BlockingQueue<StorageTask> tasks;
	protected final SharedDatabase sharedDatabase;
	protected BlockingQueue<EntryResult> entryResults;

	ReadWriteThread(BlockingQueue<StorageTask> tasks, SharedDatabase sharedDatabase,
					BlockingQueue<EntryResult> entryResults) {
		this.tasks = tasks;
		this.sharedDatabase = sharedDatabase;
		this.entryResults = entryResults;
	}

	@Override
	public void run() {
		while (!tasks.isEmpty()) {
			//  Take a task
			StorageTask task = tasks.poll();
			//System.out.println(tasks.size());
			//  Finish execution if there's nothing left
			if (task == null) {
				break;
			}

			//  Assign writing to a thread
			if (task.isWrite()) {
				try {
					writer(task);
					System.out.println("done writing");
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			//  Assign reading to a thread
			} else {
				try {
					reader(task);
					System.out.println("done reading");
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		//  Finish execution; release semaphore to alert the main thread
		ThreadPool.finishedExecutionSemaphore.release();
		System.out.println("Gata boss");
	}

	public abstract void reader(StorageTask task) throws InterruptedException;

	public abstract void writer(StorageTask task) throws InterruptedException;
}
