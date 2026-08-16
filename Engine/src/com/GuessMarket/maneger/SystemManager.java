package com.GuessMarket.maneger;

import com.GuessMarket.data.entities.*;
import com.GuessMarket.data.enums.*;
import com.GuessMarket.engine.*;
import com.GuessMarket.validation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class SystemManager {
    private Map<String, Event> events;
    private Map<String, ITradingEngine> trandingEngines; // the same key of events
    private EngineService engineService;

    public SystemManager() {
        this.events = new HashMap<>();
        this.trandingEngines = new HashMap<>();
        this.engineService = new EngineService();
    }

    public void loadXmlData(String xmlPath) throws Exception {
        Market loadedMarket = SystemFileHandler.loadXmlData(xmlPath);
        XmlFileControl.validateMarketData(loadedMarket);
        events.clear();
        trandingEngines.clear();

        for (Event event : loadedMarket.getEvents()) {
            events.put(event.getId(), event);
            if (event.getMethod() != null && event.getMethod().getLmsr() != null) {
                int b = event.getMethod().getLmsr().getB();
                trandingEngines.put(event.getId(), new LmsrEngine(b));
            }
        }
    }

    public void saveXmlData(String xmlPath) throws Exception {
        Market market = new Market();
        market.setEvents(new ArrayList<>(events.values()));
        SystemFileHandler.saveXmlData(market, xmlPath);
    }

    public List<Event> getEvents() {
        return new ArrayList<>(events.values());
    }

    public List<Event> getActiveEvents() {
        List<Event> activeEvents = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getStatus() == EventStatus.ACTIVE) {
                activeEvents.add(event);
            }
        }
        return activeEvents;
    }

    public String getEventBasicInfo(String eventId) throws Exception {
        Event event = getEvent(eventId);
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Event ID: %s%n", event.getId()));
        sb.append(String.format("Name: %s%n", event.getName()));
        sb.append(String.format("Description: %s%n", event.getDescription()));
        sb.append(String.format("Status: %s%n", event.getStatus().name()));
        sb.append(String.format("Commission: %d%% (%s)%n", event.getCommissionRate(), event.getCommissionType().name()));

        if (event.getStatus() == EventStatus.CLOSED) {
            Option winner = event.getWinningOption();
            sb.append(String.format("Winning Option: %s%n", winner != null ? winner.getName() : "None"));
        }

        sb.append(String.format("Options:%n"));

        if (event.getOptions() != null) {
            int optionIndex = 1;
            for (Option option : event.getOptions()) {
                sb.append(String.format("\t%d. %s (Purchased Shares: %.2f)%n",
                        optionIndex++, option.getName(), option.getTotalShares()));
            }
        }
        sb.append(String.format("--------------------------------------------------%n"));

        return sb.toString();
    }

    public Event getEvent(String eventId) throws Exception {
        Event event = events.get(eventId);
        if (event == null) {
            throw new Exception("Current event '" + eventId + "' not in the Market.");
        }
        return event;
    }

    public TradeMonitor executePurchase(String eventId, int optionIndex, int amount) throws Exception {
        Event event = getEvent(eventId);
        ITradingEngine engine = trandingEngines.get(eventId);

        if (engine == null) {
            throw new Exception("Trading engine for event '" + eventId + "' is missing.");
        }
        if (optionIndex < 0 || optionIndex >= event.getOptions().size()) {
            throw new Exception("Invalid option selection.");
        }
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new Exception("Event '" + eventId + "' is no longer active.");
        }

        if (engineService == null) {
            engineService = new EngineService();
        }

        TradeMonitor monitor = new TradeMonitor();

        engineService.submitTask(() -> {
            PurchaseReceipt receipt = null;

            try {
                // ATOMIC LOCK: Synchronize strictly on the target event
                synchronized (event) {
                    if (event.getStatus() != EventStatus.ACTIVE) {
                        return null; // Cancels the transaction
                    }

                    Option selectedOption = event.getOptions().get(optionIndex);

                    double sharesCost = engine.calculatePurchaseCost(selectedOption, amount, event.getOptions());
                    double commissionCost = 0.0;

                    if (event.getCommissionType() == CommissionType.ON_PURCHASE) {
                        commissionCost = sharesCost * (event.getCommissionRate() / 100.0);
                        event.getMmAccount().deposit(commissionCost);
                        event.addCommission(commissionCost);
                    }

                    selectedOption.addPurchasedShares(amount);
                    Ticket ticket = new Ticket(selectedOption, amount, sharesCost);
                    event.addTicketToHistory(ticket);

                    receipt = new PurchaseReceipt(sharesCost, commissionCost, sharesCost + commissionCost);
                }
            } finally {
                monitor.complete(receipt);
            }
            return null;
        });
        return monitor;
    }

    public String closeEvent(String eventId, int winningOptionIndex) throws Exception {
        Event event = getEvent(eventId);

        synchronized (event) {
            if (event.getStatus() != EventStatus.ACTIVE) {
                throw new Exception("Event '" + eventId + "' is not active and cannot be closed.");
            }
            if (winningOptionIndex < 0 || winningOptionIndex >= event.getOptions().size()) {
                throw new Exception("Invalid winning option selection.");
            }

            event.setStatus(EventStatus.CLOSED);
            Option winningOption = event.getOptions().get(winningOptionIndex);
            event.setWinningOption(winningOption);

            double totalPayout = winningOption.getTotalShares();
            double commissionEarned = 0.0;

            if (event.getCommissionType() == CommissionType.ON_CLOSE) {
                commissionEarned = totalPayout * (event.getCommissionRate() / 100.0);
                event.getMmAccount().deposit(commissionEarned);
                event.addCommission(commissionEarned);
            }

            String summary = String.format("Event '%s' has been officially CLOSED.%n", event.getName());
            summary += String.format("Winning Option: %s%n", winningOption.getName());
            summary += String.format("Total winning shares to be paid out: %.2f%n", totalPayout);

            if (event.getCommissionType() == CommissionType.ON_CLOSE) {
                summary += String.format("Commission Earned (ON_CLOSE at %d%%): %.2f",
                        event.getCommissionRate(), commissionEarned);
            }

            return summary;
        }
    }

    public String getEventDetailedStatus(String eventId) throws Exception {
        Event event = getEvent(eventId);
        ITradingEngine engine = trandingEngines.get(eventId);

        StringBuilder sb = new StringBuilder();

        sb.append(buildEventHeader(event));
        sb.append(buildOptionsStatus(event, engine));
        sb.append(buildCommissionStatus(event));
        sb.append(buildTradingHistory(event));
        sb.append(buildClosedStatus(event));

        return sb.toString();
    }

    private String buildEventHeader(Event event) {
        return String.format("%n--- Event Status: %s (%s) ---%n", event.getName(), event.getStatus().name());
    }

    private String buildOptionsStatus(Event event, ITradingEngine engine) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Current Options Status:%n"));
        for (Option opt : event.getOptions()) {
            double currentPrice = (engine != null) ? engine.getCurrentPrice(opt, event.getOptions()) : 0.0;
            sb.append(String.format("\t- %s: Current Value: %.2f | Total Shares: %.2f%n",
                    opt.getName(), currentPrice, opt.getTotalShares()));
        }
        return sb.toString();
    }

    private String buildCommissionStatus(Event event) {
        return String.format("%nTotal Commission Collected: %.2f$%n", event.getTotalCommissionCollected());
    }

    private String buildTradingHistory(Event event) {
        StringBuilder sb = new StringBuilder();
        List<Ticket> history = event.getHistory(); //[cite: 1]
        sb.append(String.format("%nTrading History (Latest first):%n"));

        if (history.isEmpty()) {
            sb.append(String.format("\tNo transactions yet.%n"));
        } else {
            for (int i = history.size() - 1; i >= 0; i--) {
                Ticket t = history.get(i);
                sb.append(String.format("\tOption: %s | Amount: %d | Price Paid: %.2f$%n",
                                        t.getOption().getName(), t.getAmount(), t.getPricePaid()));
            }
        }
        return sb.toString();
    }

    private String buildClosedStatus(Event event) {
        if (event.getStatus() == EventStatus.CLOSED) {
            Option winner = event.getWinningOption(); //[cite: 1]
            return String.format("%nEvent is CLOSED.%nWinning Option: %s%n",
                                winner != null ? winner.getName() : "None");
        }
        return "";
    }

    public String getEventCurrentState(String eventId) throws Exception {
        Event event = getEvent(eventId);
        ITradingEngine engine = trandingEngines.get(eventId);
        return buildOptionsStatus(event, engine);
    }
}