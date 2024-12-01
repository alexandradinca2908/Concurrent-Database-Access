package org.apd.implementation.locks;

import org.apd.executor.LockType;

public abstract class Lock {
	public static Lock createLock(LockType type, int permits) {
		if (type == LockType.WriterPreferred1) {
			return new SemaphoreLock(permits);
		}

		return new MonitorLock(permits);
	}

	public abstract void lock() throws InterruptedException;
	public abstract void unlock() throws InterruptedException;
}
