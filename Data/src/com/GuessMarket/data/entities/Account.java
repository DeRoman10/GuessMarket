package com.GuessMarket.data.entities;

public class Account {
    private double balance;

    public Account() {
        this.balance = 0.0;
    }

    public Account(double balance) {
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }
    public void deposit(double amount){
        if (amount > 0)
            this.balance += amount;
    }

    public boolean withdraw(double amount) {
        if (amount > 0) {
            this.balance -= amount;
            return true;
        }
        return false;
    }


}
