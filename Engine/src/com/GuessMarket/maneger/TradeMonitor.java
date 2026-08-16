package com.GuessMarket.maneger; // or engine package depending on your setup

public class TradeMonitor {
    private PurchaseReceipt receipt = null;
    private boolean isDone = false;

    public synchronized PurchaseReceipt waitForResult() throws InterruptedException {
        while (!isDone) {
            this.wait();
        }
        return receipt;
    }

    public synchronized void complete(PurchaseReceipt receipt) {
        this.receipt = receipt;
        this.isDone = true;
        this.notifyAll();
    }
}