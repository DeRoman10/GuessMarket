package com.service;

import java.util.concurrent.*;

public class EngineService {
    private final ExecutorService threadPool;

    public EngineService() {
        this.threadPool = Executors.newCachedThreadPool();
    }

    public <T> Future<T> submitTask(Callable<T> task) {
        return threadPool.submit(task);
    }

    public void shutdown() {
        threadPool.shutdown();
    }
}