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
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
	final int numThreads;
	final Thread[] threads;
	BlockingQueue<EntryResult> entryResults;
	BlockingQueue<StorageTask> tasks;
	SharedDatabase sharedDatabase;
	AtomicInteger threadsDone;
	ArrayList<Semaphore> dbCellsSemaphores = new ArrayList<>();

	public ThreadPool(int numThreads, List<StorageTask> tasks,
					  SharedDatabase sharedDatabase) {
		this.numThreads = numThreads;
		this.threads = new Thread[numThreads];
		this.entryResults = new LinkedBlockingQueue<>();
		this.tasks = new LinkedBlockingQueue<>(tasks);
		this.sharedDatabase = sharedDatabase;
		this.threadsDone = new AtomicInteger(0);

		//  Set a semaphore for every database cell (index)
		this.dbCellsSemaphores = new ArrayList<>();
		for (int i = 0; i < sharedDatabase.getSize(); i++) {
			dbCellsSemaphores.add(new Semaphore(1));
		}
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
						threads[i] = new Thread(new ReadPriorityThread(this));
				case LockType.WriterPreferred1 ->
						threads[i] = new Thread(new WritePriority1Thread(this));

//				case LockType.WriterPreferred2 ->
				
				default -> {
					return new ArrayList<>();
				}
			}

			this.threads[i].start();
		}

		//  Return result when all tasks are done
		for (Thread thread : this.threads) {
			if (thread != null) {
				thread.join();
			}
		}

		return entryResults.stream().toList();
	}
}
