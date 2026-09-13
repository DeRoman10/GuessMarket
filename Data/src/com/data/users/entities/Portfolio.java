package com.data.users.entities;

import com.data.events.entities.Ticket;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolio implements Serializable {

    private Map<String, Double> holdings;
    private double totalCommissionPaid;
    private double revenue;
    private List<Ticket> history;
    private double totalInvestment;

    public Portfolio() {
        this.holdings = new HashMap<>();
        this.totalCommissionPaid = 0.0;
        this.history = new ArrayList<>();
        this.totalInvestment = 0.0;
    }

    public Map<String, Double> getHoldings() { return holdings; }
    public double getTotalCommissionPaid() {
        return totalCommissionPaid;
    }
    public void addCommissionPaid(double amount) { this.totalCommissionPaid += amount; }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    public List<Ticket> getHistory() {
        return history;
    }

    public void setHistory(List<Ticket> history) {
        this.history = history;
    }

    public double getTotalInvestment() {
        return totalInvestment;
    }

    public void setTotalInvestment(double totalInvestment) {
        this.totalInvestment = totalInvestment;
    }
}