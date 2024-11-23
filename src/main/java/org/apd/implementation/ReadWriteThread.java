package org.apd.implementation;

import org.apd.executor.StorageTask;

public abstract class ReadWriteThread implements Runnable {
	protected ThreadPool threadPool;

	ReadWriteThread(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}

	@Override
	public void run() {
		while (!threadPool.tasks.isEmpty()) {
			//  Take a task
			StorageTask task = threadPool.tasks.poll();

			//  Finish execution if there's nothing left
			if (task == null) {
				break;
			}

			//  Assign reading to a thread
			if (!task.isWrite()) {
				try {
					reader(task);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			//  Assign writing to a thread
			} else {
				try {
					writer(task);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		//  Last thread must free memory by unbinding "lock type" variables
		if (threadPool.threadsDone.incrementAndGet() == threadPool.numThreads) {
			releaseMemory();
		}
	}

	public abstract void reader(StorageTask task) throws InterruptedException;

	public abstract void writer(StorageTask task) throws InterruptedException;

	public abstract void releaseMemory();
}
