package com.data.events.entities;

import com.data.events.enums.OrderDirection;
import com.data.users.entities.User;
import java.io.Serializable;

public class OBOrder implements Serializable {

    private double price;
    private int amount;
    private transient User user;
    private String userName;
    private OrderDirection direction;
    private Option option;
    private long sequenceId;

    public OBOrder() {}

    public OBOrder(double price, int amount, User user, OrderDirection direction, Option option) { // Add Option parameter
        this.price = price;
        this.amount = amount;
        this.user = user;
        this.userName = user != null ? user.getName() : null;
        this.direction = direction;
        this.option = option;
    }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; this.userName = user != null ? user.getName() : null; }
    public String getUserName() { return userName; }

    public OrderDirection getDirection() { return direction; }
    public void setDirection(OrderDirection direction) { this.direction = direction; }

    public Option getOption() { return option; }
    public void setOption(Option option) { this.option = option; }

    public long getSequenceId() { return sequenceId; }
    public void setSequenceId(long sequenceId) { this.sequenceId = sequenceId; }
}

