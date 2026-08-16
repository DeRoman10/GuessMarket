package com.GuessMarket.data.entities;

import com.GuessMarket.data.XMLhelpers.ComisionXMLhelper;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.Unmarshaller;
import com.GuessMarket.data.enums.CommissionType;
import com.GuessMarket.data.enums.EventStatus;
import com.GuessMarket.data.XMLhelpers.*;
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

    @XmlElement(name = "comision")
    private ComisionXMLhelper xmlComision; // between 0-90

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
    private Account mmAccount;

    private List<Ticket> history = new ArrayList<>();
    private double totalCommissionCollected = 0.0;
    private Option winningOption = null;


    public Event() {
        this.status = EventStatus.ACTIVE;
    }

    public Event(String id, String name, String description, int commissionRate,
                 CommissionType commissionType, EventStatus status,
                 List<Option> options, Account mmAccount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionRate = commissionRate;
        this.commissionType = commissionType;
        this.status = status;
        this.options = options;
        this.mmAccount = mmAccount;
    }

    void afterUnmarshal(Unmarshaller u, Object parent) {
        if (this.xmlComision != null) {
            this.commissionRate = this.xmlComision.getValue();
            this.commissionType = this.xmlComision.getType();
        }
        this.mmAccount = new Account();
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

    public Account getMmAccount() {
        return mmAccount;
    }

    public GMMethodXMLHelper getMethod() {
        return method;
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
}