package com.GuessMarket.data.entities;

import jakarta.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class Option implements IReadOnlyOptions {

    @XmlValue
    private String name;

    @XmlAttribute
    private double totalShares;

    public Option() {
        this.totalShares = 0.0;
    }

    public Option(String name) {
        this.name = name;
        this.totalShares = 0.0;
    }
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public double getTotalShares() {
        return this.totalShares;
    }

    public void addPurchasedShares(double amount) {
        if (amount > 0) {
            this.totalShares += amount;
        }
    }

    public void removePurchasedShares(double amount) {
        if (amount > 0 && this.totalShares >= amount) {
            this.totalShares -= amount;
        }
    }
}
