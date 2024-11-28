package org.apd.implementation.locks;

import java.util.concurrent.Semaphore;

public class SemaphoreLock extends Lock {
	Semaphore semaphore;

	public SemaphoreLock() {
		super();
		semaphore = new Semaphore(0);
	}

	@Override
	public void lock() throws InterruptedException {
		semaphore.acquire();
	}

	@Override
	public void unlock() throws InterruptedException {
		semaphore.release();
	}
}
