package org.apd.implementation;

import org.apd.executor.LockType;
import org.apd.executor.StorageTask;
import org.apd.implementation.threads.ReadPriorityThread;
import org.apd.implementation.threads.WritePriorityThread;
import org.apd.implementation.threads.separatewriters.WritePriority2Thread;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
	public final int numThreads;
	public final Thread[] threads;
	public BlockingQueue<EntryResult> entryResults;
	public final BlockingQueue<StorageTask> tasks;
	public SharedDatabase sharedDatabase;
	public AtomicInteger threadsDone;
	public AtomicBoolean allTasksSet;

	public ThreadPool(int numThreads, SharedDatabase sharedDatabase,
					  LockType lockType) {
		this.numThreads = numThreads;
		this.threads = new Thread[numThreads];
		this.entryResults = new LinkedBlockingQueue<>();
		this.tasks = new LinkedBlockingQueue<>();
		this.sharedDatabase = sharedDatabase;
		this.threadsDone = new AtomicInteger(0);
		this.allTasksSet = new AtomicBoolean(false);

		//  Create threads and let them run
		for (int i = 0; i < this.numThreads; i++) {
			if (lockType == LockType.ReaderPreferred) {
				threads[i] = new Thread(new ReadPriorityThread(this));
			} else if (lockType == LockType.WriterPreferred2){
				threads[i] = new Thread(new WritePriority2Thread(this));
			}

			this.threads[i].start();
		}
	}

	public List<EntryResult> execute(List<StorageTask> tasks) throws InterruptedException {
		//  Add tasks one by one, simulating a real-time application
		for (StorageTask task: tasks) {
			this.tasks.add(task);
		}

		//  Let threads know they can stop running after they are done
		//  Insert a "poison pill" for each thread, so they terminate gracefully
		StorageTask poisonPill = new StorageTask(-1, "poisonPill");
		for (Thread ignored : this.threads) {
			this.tasks.put(poisonPill);
		}

		//  Return result after all tasks are done
		for (Thread thread : this.threads) {
			if (thread != null) {
				thread.join();
			}
		}

		return entryResults.stream().toList();
	}
}
