package org.apd.implementation.locks;

public class MonitorLock extends Lock {
	final Object monitorLock;

	MonitorLock() {
		monitorLock = new Object();
	}

	public void lock() throws InterruptedException {
		synchronized (monitorLock) {
			monitorLock.wait();
		}
	}

	public void unlock() throws InterruptedException {
		synchronized (monitorLock) {
			monitorLock.notify();
		}
	}
}
