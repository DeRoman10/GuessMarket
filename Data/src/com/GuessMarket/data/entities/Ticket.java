package com.GuessMarket.data.entities;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Ticket {
    private Option option;
    private int amount;
    private double pricePaid;

    public Ticket() {} // Required for JAXB

    public Ticket(Option option, int amount, double pricePaid) {
        this.option = option;
        this.amount = amount;
        this.pricePaid = pricePaid;
    }

    public Option getOption() { return option; }
    public int getAmount() { return amount; }
    public double getPricePaid() { return pricePaid; }
}