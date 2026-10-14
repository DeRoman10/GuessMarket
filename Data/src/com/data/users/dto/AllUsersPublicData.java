package com.data.users.dto;

import com.data.users.entities.User;
import java.io.Serializable;

public class AllUsersPublicData implements Serializable {
    private String name;
    private double balance;
    private boolean marketMaker;

    public AllUsersPublicData() {}

    public static AllUsersPublicData of(User u) {
        AllUsersPublicData s = new AllUsersPublicData();
        s.name = u.getName();
        s.balance = u.getBalance();
        s.marketMaker = u.isMarketMaker();
        return s;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isMarketMaker() { return marketMaker; }
}
