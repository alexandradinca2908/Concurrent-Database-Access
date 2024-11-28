package org.apd.implementation.threads;

import org.apd.executor.StorageTask;
import org.apd.implementation.ThreadPool;

public abstract class ReadWriteThread implements Runnable {
	public ThreadPool threadPool;

	protected ReadWriteThread(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}

	@Override
	public void run() {
		while (true) {
			//  Take a task
			StorageTask task;
			try {
				task = threadPool.tasks.take();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			//  Finish execution if poison pill is encountered
			if (task.data() != null && task.data().equals("poisonPill")) {
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

		//  Last thread must free memory by unbinding static thread variables
		if (threadPool.threadsDone.incrementAndGet() == threadPool.numThreads) {
			releaseMemory();
		}
	}

	public abstract void initSynchronizations();

	public abstract void reader(StorageTask task) throws InterruptedException;

	public abstract void writer(StorageTask task) throws InterruptedException;

	public abstract void releaseMemory();
}
