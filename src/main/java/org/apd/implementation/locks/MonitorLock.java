package org.apd.implementation.locks;

public class MonitorLock extends Lock {
	private int permits;
	final Object monitorLock;

	MonitorLock(int permits) {
		this.permits = permits;
		this.monitorLock = new Object();
	}

	public void lock() throws InterruptedException {
		synchronized (monitorLock) {
			if (permits <= 0) {
				monitorLock.wait();
			}
			permits--;
		}
	}

	public void unlock() throws InterruptedException {
		synchronized (monitorLock) {
			permits++;
			monitorLock.notify();
		}
	}
}
