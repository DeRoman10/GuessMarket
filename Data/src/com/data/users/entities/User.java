package com.data.users.entities;

import java.io.Serializable;
import java.util.*;

public class User implements Serializable {

    private String name;
    private double balance;
    private boolean blocked = false;

    private Map<String, Portfolio> portfolios = new HashMap<>();   // key = event name
    private Set<String> managedEvents = new LinkedHashSet<>();     // names of events this user is MM of
    private List<AccountEntry> ledger = new ArrayList<>();

    public User() {}

    public User(String name) {
        this.name = name;
        this.balance = 0.0;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return blocked; }
    public boolean isMarketMaker() { return !managedEvents.isEmpty(); }

    public void deposit(double amount) { deposit(amount, "Deposit"); }

    public synchronized void deposit(double amount, String reason) {
        if (amount > 0) {
            balance += amount;
            ledger.add(new AccountEntry(reason, amount, balance));
            if (balance >= 0) blocked = false;
        }
    }

    public boolean withdraw(double amount) { return withdraw(amount, "Payment"); }

    public synchronized boolean withdraw(double amount, String reason) {
        if (amount > 0) {
            balance -= amount;
            ledger.add(new AccountEntry(reason, -amount, balance));
            if (balance < 0) blocked = true;   // negative balance is allowed, but the account is locked
            return true;
        }
        return false;
    }

    public Set<String> getManagedEvents() { return managedEvents; }
    public void addManagedEvent(String eventName) { managedEvents.add(eventName); }
    public Map<String, Portfolio> getPortfolios() { return portfolios; }
    public List<AccountEntry> getLedger() { return ledger; }
}