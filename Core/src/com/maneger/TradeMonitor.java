package com.maneger;

public class TradeMonitor<T extends IReceipt> {
    private T receipt = null;
    private boolean isDone = false;
    private Exception error = null;

    public synchronized T waitForResult() throws Exception {
        while (!isDone) wait();
        if (error != null) throw error;
        return receipt;
    }

    public synchronized void complete(T receipt) {
        this.receipt = receipt;
        this.isDone = true;
        notifyAll();
    }

    public synchronized void fail(Exception e) {
        this.error = e;
        this.isDone = true;
        notifyAll();
    }
}