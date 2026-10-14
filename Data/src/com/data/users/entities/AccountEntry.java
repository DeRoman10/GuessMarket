package com.data.users.entities;

import java.io.Serializable;

public class AccountEntry implements Serializable { // holds balance changes
    private long timestamp;
    private String description;
    private double amount;
    private double balanceAfter;

    public AccountEntry() {}

    public AccountEntry(String description, double amount, double balanceAfter) {
        this.timestamp = System.currentTimeMillis();
        this.description = description;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public long getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
}