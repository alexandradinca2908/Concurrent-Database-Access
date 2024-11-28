package org.apd.executor;

import org.apd.implementation.ThreadPool;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.ArrayList;
import java.util.List;

/* DO NOT MODIFY THE METHODS SIGNATURES */
public class TaskExecutor {
    private final SharedDatabase sharedDatabase;

    public TaskExecutor(int storageSize, int blockSize, long readDuration, long writeDuration) {
        sharedDatabase = new SharedDatabase(storageSize, blockSize, readDuration, writeDuration);
    }

    public List<EntryResult> ExecuteWork(int numberOfThreads, List<StorageTask> tasks, LockType lockType)
            throws InterruptedException {
        /* IMPLEMENT HERE THE THREAD POOL, ASSIGN THE TASKS AND RETURN THE RESULTS */
        //  Create thread pool
        ThreadPool threadPool = new ThreadPool(numberOfThreads, sharedDatabase, lockType);

        //  Thread pool must be initialized
        //  Thread pool must also have at least one thread
        if (threadPool.threads.length == 0) {
            System.out.println("Thread pool not initialized properly!");
            return new ArrayList<>();
        }

        //  Return results
        return threadPool.execute(tasks);
    }

    public List<EntryResult> ExecuteWorkSerial(List<StorageTask> tasks) {
        var results = tasks.stream().map(task -> {
            try {
                if (task.isWrite()) {
                    return sharedDatabase.addData(task.index(), task.data());
                } else {
                    return sharedDatabase.getData(task.index());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        return results.stream().toList();
    }
}
