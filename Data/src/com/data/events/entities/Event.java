package com.data.events.entities;

import com.data.XMLhelpers.CommissionXMLHelper;
import com.data.XMLhelpers.GMMethodXMLHelper;
import com.data.users.entities.User;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.Unmarshaller;
import com.data.events.enums.CommissionType;
import com.data.events.enums.EventStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Event implements Serializable {


    @XmlElement(name = "id")
    private String id;

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "commission")
    private CommissionXMLHelper xmlCommission; // between 0-90

    @XmlElementWrapper(name = "GM-options")
    @XmlElement(name = "GM-option")
    private List<Option> options;

    @XmlElement(name = "GM-method")
    private GMMethodXMLHelper method;

    @XmlTransient
    private int commissionRate;

    @XmlTransient
    private CommissionType commissionType;

    @XmlAttribute(name = "status")
    private EventStatus status;

    @XmlTransient
    private User mmUser;

    private List<Ticket> history = new ArrayList<>();
    private double totalCommissionCollected = 0.0;
    private Option winningOption = null;
    private double contractBalance = 0.0;


    public Event() {
        this.status = EventStatus.ACTIVE;
    }

    public Event(String id, String name, String description, int commissionRate,
                 CommissionType commissionType, EventStatus status,
                 List<Option> options, User mmUser, GMMethodXMLHelper method) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionRate = commissionRate;
        this.commissionType = commissionType;
        this.status = status;
        this.options = options;
        this.mmUser = mmUser;
        this.method = method;
    }

    void afterUnmarshal(Unmarshaller u, Object parent) {
        if (this.xmlCommission != null) {
            this.commissionRate = this.xmlCommission.getValue();
            this.commissionType = this.xmlCommission.getType();
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionRate() {
        return commissionRate;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public List<Option> getOptions() {
        return options;
    }

    public List<IReadOnlyOptions> getReadOnlyOptions() {
        if (options == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(options));
    }

    public User getMmAccount() {
        return mmUser;
    }

    public GMMethodXMLHelper getMethod() {
        return method;
    }

    public boolean isLmsr() {
        return method != null && method.getLmsr() != null;
    }

    public boolean isOrderBook() {
        return method != null && method.getOrderBook() != null;
    }

    public double getBaseValue() {
        return isOrderBook() ? method.getOrderBook().getD() : 1.0;
    }

    public void setWinningOption(Option winningOption) {
        this.winningOption = winningOption;
    }

    public List<Ticket> getHistory() {
        return history;
    }

    public Option getWinningOption() {
        return winningOption;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public void addTicketToHistory(Ticket ticket) {
        this.history.add(ticket);
    }

    public void addCommission(double amount) {
        this.totalCommissionCollected += amount;
    }

    public void setMmAccount(User mmUser) {
        this.mmUser = mmUser;
    }

    public double getContractBalance() { return contractBalance; }

    public void depositToContract(double amount) {
        if (amount > 0) {
            this.contractBalance += amount;
        }
    }

    public void withdrawFromContract(double amount) {
        if (amount > 0) {
            this.contractBalance -= amount;
        }
    }
}
