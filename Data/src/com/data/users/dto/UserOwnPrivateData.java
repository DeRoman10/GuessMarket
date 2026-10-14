package com.data.users.dto;

import com.data.users.entities.AccountEntry;
import com.data.users.entities.Portfolio;
import com.data.users.entities.User;
import java.io.Serializable;
import java.util.*;

public class UserOwnPrivateData implements Serializable {
    private String name;
    private double balance;
    private boolean blocked;
    private List<String> managedEvents;
    private Map<String, Portfolio> portfolios;
    private List<AccountEntry> ledger;

    public UserOwnPrivateData() {}

    public static UserOwnPrivateData of(User u) {
        UserOwnPrivateData p = new UserOwnPrivateData();
        p.name = u.getName();
        p.balance = u.getBalance();
        p.blocked = u.isBlocked();
        p.managedEvents = new ArrayList<>(u.getManagedEvents());
        p.portfolios = new LinkedHashMap<>(u.getPortfolios());
        p.ledger = new ArrayList<>(u.getLedger());
        return p;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return blocked; }
    public List<String> getManagedEvents() { return managedEvents; }
    public Map<String, Portfolio> getPortfolios() { return portfolios; }
    public List<AccountEntry> getLedger() { return ledger; }
}
