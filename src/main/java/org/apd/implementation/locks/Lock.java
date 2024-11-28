package org.apd.implementation.locks;

import org.apd.executor.LockType;

public abstract class Lock {
	public static Lock createLock(LockType type) {
		if (type == LockType.WriterPreferred1) {
			return new SemaphoreLock();
		}

		return new MonitorLock();
	}

	public abstract void lock() throws InterruptedException;
	public abstract void unlock() throws InterruptedException;
}
