package org.apd.implementation;

import org.apd.executor.LockType;
import org.apd.executor.StorageTask;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class ThreadPool {
	private final int numThreads;
	private final Thread[] threads;
	private BlockingQueue<StorageTask> tasks;
	private final SharedDatabase sharedDatabase;
	private List<EntryResult> entryResults;
	private final Semaphore finishedExecutionSemaphore;

	public ThreadPool(int numThreads, List<StorageTask> tasks, SharedDatabase sharedDatabase) {
		this.numThreads = numThreads;
		this.threads = new Thread[numThreads];
		this.tasks = new LinkedBlockingQueue<>(tasks);
		this.sharedDatabase = sharedDatabase;
		this.entryResults = new ArrayList<>();
		this.finishedExecutionSemaphore = new Semaphore(-numThreads + 2);
	}

	public List<EntryResult> execute(LockType lockType) throws InterruptedException {
		//  Thread pool must be initialized
		//  Thread pool must also have at least one thread
		if (this.threads.length == 0) {
			System.out.println("Thread pool not initialized properly!");
			return new ArrayList<>();
		}

		//  Create threads and let them execute
		for (int i = 0; i < this.numThreads; i++) {
			switch (lockType) {
				case LockType.ReaderPreferred ->
						threads[i] = new Thread(new ReadPriorityThread(tasks, sharedDatabase,
								entryResults, finishedExecutionSemaphore));
//				case LockType.WriterPreferred1 -> {
//					return new ArrayList<>();
//				}
//				case LockType.WriterPreferred2 -> {
//					return;
//				}
				default -> {
					return new ArrayList<>();
				}
			}

			this.threads[i].start();
		}

		//  Return result when all tasks are done
		System.out.println("pana aici am dus");
		finishedExecutionSemaphore.acquire();
		return entryResults;
	}
}
