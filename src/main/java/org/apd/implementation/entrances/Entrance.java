package org.apd.implementation.entrances;

import org.apd.executor.LockType;
import org.apd.implementation.locks.Lock;
import org.apd.implementation.locks.MonitorLock;
import org.apd.implementation.locks.SemaphoreLock;

public abstract class Entrance {
	public static Entrance createEntrance(LockType type) {
		if (type == LockType.WriterPreferred1) {
			return new SemaphoreEntrance();
		}

		return new MonitorEntrance();
	}

	public abstract void lock() throws InterruptedException;
	public abstract void unlock();
}
