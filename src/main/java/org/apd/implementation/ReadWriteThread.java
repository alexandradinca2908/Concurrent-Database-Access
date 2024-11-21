package org.apd.implementation;

import org.apd.executor.StorageTask;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public abstract class ReadWriteThread implements Runnable {
	private BlockingQueue<StorageTask> tasks;
	protected final SharedDatabase sharedDatabase;
	protected List<EntryResult> entryResults;
	protected Semaphore finishedExecutionSemaphore;

	ReadWriteThread(BlockingQueue<StorageTask> tasks, SharedDatabase sharedDatabase,
					List<EntryResult> entryResults, Semaphore finishedExecutionSemaphore) {
		this.tasks = tasks;
		this.sharedDatabase = sharedDatabase;
		this.entryResults = entryResults;
		this.finishedExecutionSemaphore = finishedExecutionSemaphore;
	}

	@Override
	public void run() {
		while (!tasks.isEmpty()) {
			//  Take a task
			StorageTask task = tasks.poll();

			//  Finish execution if there's nothing left
			//  Release semaphore to alert the main thread
			if (task == null) {
				break;
			}

			//  Assign writing to a thread
			if (task.isWrite()) {
				try {
					writer(task);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			//  Assign reading to a thread
			} else {
				try {
					reader(task);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		//  Finish execution
		finishedExecutionSemaphore.release();
		System.out.println("Gata boss: " + finishedExecutionSemaphore.availablePermits());
	}

	public abstract void reader(StorageTask task) throws InterruptedException;

	public abstract void writer(StorageTask task) throws InterruptedException;
}
