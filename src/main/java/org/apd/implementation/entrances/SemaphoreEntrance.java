package org.apd.implementation.entrances;

import java.util.concurrent.Semaphore;

public class SemaphoreEntrance extends Entrance {
	private final Semaphore semaphore;

	public SemaphoreEntrance() {
		super();
		this.semaphore = new Semaphore(1);
	}

	@Override
	public void lock() throws InterruptedException {
		this.semaphore.acquire();
	}

	@Override
	public void unlock() {
		this.semaphore.release();
	}
}
