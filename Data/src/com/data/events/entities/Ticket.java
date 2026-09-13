package com.data.events.entities;

import com.data.events.enums.TicketAction;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class Ticket implements Serializable {
    private Option option;
    private int amount;
    private double pricePaid;
    private TicketAction action;

    public Ticket() {} // Required for JAXB

    public Ticket(Option option, int amount, double pricePaid , TicketAction action) {
        this.option = option;
        this.amount = amount;
        this.pricePaid = pricePaid;
        this.action = action;
    }
    public Ticket(Option option, int amount, double pricePaid) {
        this(option, amount, pricePaid, TicketAction.BUY);
    }

    public Option getOption() { return option; }
    public int getAmount() { return amount; }
    public double getPricePaid() { return pricePaid; }
    public TicketAction getAction() { return action; }
}