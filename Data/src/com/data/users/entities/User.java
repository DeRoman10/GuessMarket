package com.data.users.entities;

import com.data.XMLhelpers.UserMMHelper;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class User implements Serializable {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "initial-cash")
    private double balance;

    @XmlElementWrapper(name = "GM-market-maker")
    @XmlElement(name = "event")
    private List<UserMMHelper> managedEventsXml;

    @XmlTransient
    private Map<String, Portfolio> portfolios;

    @XmlTransient
    private Set<String> managedEvents;

    @XmlTransient
    private List<Double> balanceHistory;

    private boolean blocked = false;

    public User() {
        this.portfolios = new HashMap<>();
        this.managedEvents = new HashSet<>();
        this.balanceHistory = new ArrayList<>();
    }

    void afterUnmarshal(Unmarshaller u, Object parent) {
        this.portfolios = new HashMap<>();
        this.managedEvents = new HashSet<>();
        this.balanceHistory = new ArrayList<>();
        this.balanceHistory.add(this.balance);

        if (this.managedEventsXml != null) {
            for (UserMMHelper helper : this.managedEventsXml) {
                this.managedEvents.add(helper.getId());
            }
        }
    }

    public String getName() { return name; }

    public double getBalance() { return balance; }

    public void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
            this.balanceHistory.add(this.balance);
            if(this.balance >= 0){
                this.blocked = false;
            }
        }
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean withdraw(double amount) {
        if (amount > 0) {
            this.balance -= amount;
            this.balanceHistory.add(this.balance);
            if (this.balance < 0) {
                this.blocked = true;
            }
            return true;
        }
        return false;
    }

    public Set<String> getManagedEvents() {
        return managedEvents;
    }

    public void addManagedEvent(String eventId) {
        this.managedEvents.add(eventId);
    }


    public Map<String, Portfolio> getPortfolios() {
        return portfolios;
    }

    public List<Double> getBalanceHistory() {
        return balanceHistory;
    }
}