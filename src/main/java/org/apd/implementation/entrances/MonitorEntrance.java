package org.apd.implementation.entrances;

import java.util.concurrent.atomic.AtomicInteger;

public class MonitorEntrance extends Entrance {
	private int enterQueue;
	private final Object enter;

	public MonitorEntrance() {
		this.enterQueue = 1;
		this.enter = new Object();
	}

	@Override
	public void lock() throws InterruptedException {
		synchronized (enter) {
			if (enterQueue <= 0) {
				enter.wait();
			}
			enterQueue--;
		}
	}

	@Override
	public void unlock() {
		synchronized (enter) {
			enterQueue++;
			enter.notify();
		}
	}
}
