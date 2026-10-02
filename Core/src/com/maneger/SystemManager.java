package com.maneger;

import com.data.XMLhelpers.GMMethodXMLHelper;
import com.data.XMLhelpers.GMlmsrXMLhelper;
import com.data.XMLhelpers.GMOrderBookXMLHelper;
import com.data.events.entities.Event;
import com.data.events.entities.OBOrder;
import com.data.events.entities.Option;
import com.data.events.entities.Ticket;
import com.data.events.enums.CommissionType;
import com.data.events.enums.EventStatus;
import com.data.events.enums.OrderDirection;
import com.data.files.entities.Market;
import com.data.users.entities.User;
import com.engine.ITradingEngine;
import com.engine.LmsrEngine;
import com.engine.OrderBookEngine;
import com.engine.UserManager;
import com.service.EngineService;

import java.util.*;

public class SystemManager {
    // בתרגיל 3 המפתח הוא שם האירוע (name) כי ה-ID בוטל
    private Map<String, Event> events;
    private Map<String, ITradingEngine> tradingEngines;
    private EngineService engineService;
    private UserManager userManager;

    public SystemManager() {
        this.events = new HashMap<>();
        this.tradingEngines = new HashMap<>();
        this.engineService = new EngineService();
        this.userManager = new UserManager();
    }



    public void addUser(User user) {
        if (userManager.getAllUsers() == null || userManager.getAllUsers().stream().noneMatch(u -> u.getName().equalsIgnoreCase(user.getName()))) {
            List<User> currentUsers = new ArrayList<>(userManager.getAllUsers());
            currentUsers.add(user);
            userManager.loadUsers(currentUsers);
        }
    }

    public List<User> getUsers() {
        if (userManager == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(userManager.getAllUsers());
    }


    public synchronized void loadMarketData(Market loadedMarket, User uploader) throws Exception {

        for (Event event : loadedMarket.getEvents()) {
            if (events.containsKey(event.getName())) {
                throw new Exception("An event named '" + event.getName() + "' already exists in the system.");
            }
        }

        for (Event event : loadedMarket.getEvents()) {

            event.setMmAccount(uploader);
            uploader.addManagedEvent(event.getName());

            events.put(event.getName(), event);

            if (event.isLmsr()) {
                int b = event.getMethod().getLmsr().getB();
                ITradingEngine engine = new LmsrEngine(b);
                tradingEngines.put(event.getName(), engine);
            }

            if (event.isOrderBook()) {
                int d = event.getMethod().getOrderBook().getD();
                boolean allowMint = event.getMethod().getOrderBook().isAllowMint();
                ITradingEngine engine = new com.engine.OrderBookEngine(d, allowMint);
                tradingEngines.put(event.getName(), engine);
            }

            fundLmsrSubsidy(event);
            fundObInitialInvestment(event);

            for (User user : userManager.getAllUsers()) {
                if (user.getManagedEvents() != null) {
                    for (String eventId : user.getManagedEvents()) {
                        Event ev = events.get(eventId);
                        if (ev != null) {
                            ev.setMmAccount(user);
                        }
                    }
                }
            }
        }
    }

    private void fundLmsrSubsidy(Event event) {
        if (!event.isLmsr()) return;
        ITradingEngine engine = tradingEngines.get(event.getName());
        User mm = event.getMmAccount();
        if (!(engine instanceof LmsrEngine lmsrEngine) || mm == null) return;

        double subsidy = lmsrEngine.getPotValue(event.getOptions());
        mm.withdraw(subsidy);
        event.depositToContract(subsidy);
    }

    private void fundObInitialInvestment(Event event) {
        if (!event.isOrderBook()) return;
        User mm = event.getMmAccount();
        if (mm == null) return;

        int initialInvestment = event.getMethod().getOrderBook().getInitial();
        int baseValue = event.getMethod().getOrderBook().getD();
        if (initialInvestment <= 0 || baseValue <= 0) return;

        mm.withdraw(initialInvestment);
        event.depositToContract(initialInvestment);
        double pairsToMint = initialInvestment / (double) baseValue;

        com.data.users.entities.Portfolio mmPortfolio =
                mm.getPortfolios().computeIfAbsent(event.getName(), k -> new com.data.users.entities.Portfolio());
        for (Option option : event.getOptions()) {
            mmPortfolio.getHoldings().merge(option.getName(), pairsToMint, Double::sum);
            option.addPurchasedShares(pairsToMint);
        }
    }

    public synchronized Event createLmsrEvent(User creator, String name, String description,
                                              int commissionRate, CommissionType commissionType,
                                              String option1Name, String option2Name,
                                              int liquidityParam) throws Exception {
        validateNewEventInputs(creator, name, description, commissionRate, option1Name, option2Name);
        if (liquidityParam <= 0) {
            throw new Exception("Liquidity index (b) must be a positive whole number.");
        }

        double requiredSubsidy = liquidityParam * Math.log(2.0);
        if (creator.getBalance() < requiredSubsidy) {
            throw new Exception(String.format("Insufficient balance to subsidize market. Required: $%.2f, Available: $%.2f",
                    requiredSubsidy, creator.getBalance()));
        }

        List<Option> options = List.of(new Option(option1Name.trim()), new Option(option2Name.trim()));
        GMMethodXMLHelper method = new GMMethodXMLHelper(new GMlmsrXMLhelper(liquidityParam));

        Event event = new Event(name.trim(), name.trim(), description.trim(), commissionRate,
                commissionType, EventStatus.ACTIVE, options, creator, method);

        events.put(name.trim(), event);
        tradingEngines.put(name.trim(), new LmsrEngine(liquidityParam));
        creator.addManagedEvent(name.trim());
        fundLmsrSubsidy(event);
        return event;
    }


    public synchronized Event createOrderBookEvent(User creator, String name, String description,
                                                   int commissionRate, CommissionType commissionType,
                                                   String option1Name, String option2Name,
                                                   int initialInvestment, boolean allowMint,
                                                   int baseValue) throws Exception {
        validateNewEventInputs(creator, name, description, commissionRate, option1Name, option2Name);
        if (initialInvestment < 0) {
            throw new Exception("Initial investment cannot be negative.");
        }
        if (baseValue <= 0) {
            throw new Exception("Base value (d) must be a positive whole number.");
        }
        if (initialInvestment > creator.getBalance()) {
            throw new Exception("You do not have enough balance to cover the initial investment.");
        }

        List<Option> options = List.of(new Option(option1Name.trim()), new Option(option2Name.trim()));
        GMMethodXMLHelper method = new GMMethodXMLHelper(new GMOrderBookXMLHelper(allowMint, initialInvestment, baseValue));

        Event event = new Event(name.trim(), name.trim(), description.trim(), commissionRate,
                commissionType, EventStatus.ACTIVE, options, creator, method);

        events.put(name.trim(), event);
        tradingEngines.put(name.trim(), new OrderBookEngine(baseValue, allowMint));
        creator.addManagedEvent(name.trim());
        fundObInitialInvestment(event);
        return event;
    }

    private void validateNewEventInputs(User creator, String name, String description,
                                        int commissionRate, String option1Name, String option2Name) throws Exception {
        if (creator == null) throw new Exception("Select a user before creating an event.");
        if (name == null || name.isBlank()) throw new Exception("Event name is required.");
        if (events.containsKey(name.trim())) throw new Exception(String.format("An event named '%s' already exists.", name.trim()));
        if (description == null || description.isBlank()) throw new Exception("Event description is required.");
        if (commissionRate < 0 || commissionRate > 90) throw new Exception("Commission must be a whole number between 0 and 90.");
        if (option1Name == null || option1Name.isBlank() || option2Name == null || option2Name.isBlank()) throw new Exception("Both option names are required.");
        if (option1Name.trim().equalsIgnoreCase(option2Name.trim())) throw new Exception("The two option names must be different.");
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

    public Event getEvent(String eventName) throws Exception {
        Event event = events.get(eventName);
        if (event == null) {
            throw new Exception("Current event '" + eventName + "' not in the Market.");
        }
        return event;
    }

    private void depositToMm(Event event, double amount) {
        User mm = event.getMmAccount();
        if (mm != null && amount != 0) {
            mm.deposit(amount);
        }
    }

    public TradeMonitor<PurchaseReceipt> executePurchase(String eventName, int optionIndex, int amount, User buyer) throws Exception {
        Event event = getEvent(eventName);
        ITradingEngine engine = tradingEngines.get(eventName);

        if (engine == null) throw new Exception("Trading engine for event '" + eventName + "' is missing.");
        if (optionIndex < 0 || optionIndex >= event.getOptions().size()) throw new Exception("Invalid option selection.");
        if (event.getStatus() != EventStatus.ACTIVE) throw new Exception("Event '" + eventName + "' is no longer active.");

        if (engineService == null) engineService = new EngineService();

        TradeMonitor<PurchaseReceipt> monitor = new TradeMonitor<>();

        engineService.submitTask(() -> {
            try {
                synchronized (event) {
                    if (event.getStatus() != EventStatus.ACTIVE) return null;

                    Option selectedOption = event.getOptions().get(optionIndex);
                    double sharesCost = engine.calculatePurchaseCost(selectedOption, amount, event.getOptions());
                    double commissionCost = (event.getCommissionType() == CommissionType.ON_PURCHASE)
                            ? sharesCost * (event.getCommissionRate() / 100.0) : 0.0;

                    double totalCost = sharesCost + commissionCost;

                    if (buyer != null) {
                        if (buyer.isBlocked()) {
                            throw new Exception("Account is locked due to a negative balance. No further actions allowed until balance is positive.");
                        }
                        buyer.withdraw(totalCost);
                        event.depositToContract(sharesCost);

                        com.data.users.entities.Portfolio portfolio = buyer.getPortfolios().get(eventName);
                        if (portfolio == null) {
                            portfolio = new com.data.users.entities.Portfolio();
                            buyer.getPortfolios().put(eventName, portfolio);
                        }

                        double currentHoldings = portfolio.getHoldings().getOrDefault(selectedOption.getName(), 0.0);
                        portfolio.getHoldings().put(selectedOption.getName(), currentHoldings + amount);
                        portfolio.addCommissionPaid(commissionCost);
                        portfolio.setTotalInvestment(portfolio.getTotalInvestment() + totalCost);
                        portfolio.getHistory().add(new Ticket(selectedOption, amount, sharesCost));
                    }

                    if (commissionCost > 0) {
                        depositToMm(event, commissionCost);
                        event.addCommission(commissionCost);
                    }

                    selectedOption.addPurchasedShares(amount);
                    event.addTicketToHistory(new Ticket(selectedOption, amount, sharesCost));

                    PurchaseReceipt receipt = new PurchaseReceipt(sharesCost, commissionCost, totalCost);
                    monitor.complete(receipt);
                }
            } catch (Exception e) {
                monitor.fail(e);
            }
            return null;
        });
        return monitor;
    }

    public TradeMonitor<PurchaseReceipt> executeOBTrade(String eventName, int optionIndex, int amount, double limitPrice, OrderDirection direction, User user) throws Exception {
        Event event = getEvent(eventName);
        ITradingEngine engine = tradingEngines.get(eventName);

        if (!(engine instanceof com.engine.OrderBookEngine obEngine)) {
            throw new Exception("Event is not an Order Book event.");
        }

        if (user.isBlocked()) {
            throw new Exception("Account is locked due to a negative balance.");
        }

        TradeMonitor<PurchaseReceipt> monitor = new TradeMonitor<>();

        engineService.submitTask(() -> {
            try {
                synchronized (event) {
                    if (event.getStatus() != EventStatus.ACTIVE) throw new Exception("Event closed.");

                    Option selectedOption = event.getOptions().get(optionIndex);
                    Option oppositeOption = (event.getOptions().size() == 2) ? event.getOptions().get(1 - optionIndex) : null;

                    com.data.users.entities.Portfolio p = user.getPortfolios().computeIfAbsent(eventName, k -> new com.data.users.entities.Portfolio());

                    if (direction == OrderDirection.SELL) {
                        double sharesOwned = p.getHoldings().getOrDefault(selectedOption.getName(), 0.0);
                        if (sharesOwned < amount) {
                            throw new Exception("Lacking shares. You only own " + sharesOwned + " shares.");
                        }
                        p.getHoldings().put(selectedOption.getName(), sharesOwned - amount);
                    } else {
                        double cost = amount * limitPrice;
                        user.withdraw(cost);
                    }

                    OBOrder newOrder = new OBOrder(limitPrice, amount, user, direction, selectedOption);
                    obEngine.processOrder(newOrder, oppositeOption, event);

                    PurchaseReceipt receipt = new PurchaseReceipt(limitPrice * amount, 0.0, limitPrice * amount);
                    monitor.complete(receipt);
                }
            } catch (Exception e) {
                monitor.fail(e);
            }
            return null;
        });
        return monitor;
    }

    public TradeMonitor<FinalReceipt> closeEvent(String eventName, int winningOptionIndex) throws Exception {
        Event event = getEvent(eventName);

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new Exception("Event is not active.");
        }

        TradeMonitor<FinalReceipt> monitor = new TradeMonitor<>();

        engineService.submitTask(() -> {
            FinalReceipt receipt = null;
            try {
                synchronized (event) {
                    if (event.getStatus() != EventStatus.ACTIVE) {
                        throw new Exception("Event was already closed.");
                    }

                    event.setStatus(EventStatus.CLOSED);
                    Option winningOption = event.getOptions().get(winningOptionIndex);
                    event.setWinningOption(winningOption);

                    double baseValue = event.getBaseValue();
                    double totalMarketNetPayout = 0.0;
                    double totalMarketCommission = 0.0;

                    for (User user : userManager.getAllUsers()) {
                        com.data.users.entities.Portfolio portfolio = user.getPortfolios().get(eventName);
                        if (portfolio != null) {
                            double winningShares = portfolio.getHoldings().getOrDefault(winningOption.getName(), 0.0);
                            if (winningShares > 0) {
                                double grossPayout = winningShares * baseValue;
                                double userCommission = (event.getCommissionType() == CommissionType.ON_CLOSE)
                                        ? grossPayout * (event.getCommissionRate() / 100.0) : 0.0;
                                double netPayout = grossPayout - userCommission;

                                event.withdrawFromContract(grossPayout);
                                user.deposit(netPayout);
                                portfolio.setRevenue(netPayout);

                                totalMarketNetPayout += netPayout;
                                totalMarketCommission += userCommission;
                            }
                        }
                    }

                    if (totalMarketCommission > 0) {
                        depositToMm(event, totalMarketCommission);
                        event.addCommission(totalMarketCommission);
                    }

                    double leftover = event.getContractBalance();
                    if (leftover > 0) {
                        depositToMm(event, leftover);
                        event.withdrawFromContract(leftover);
                    }

                    receipt = new FinalReceipt(event.getName(), winningOption.getName(), totalMarketNetPayout, totalMarketCommission);
                }
            } catch (Exception e) {
                monitor.fail(e);
            } finally {
                if (receipt != null) monitor.complete(receipt);
            }
            return null;
        });

        return monitor;
    }

    public String getEventDetailedStatus(String eventName) throws Exception {
        Event event = getEvent(eventName);
        ITradingEngine engine = tradingEngines.get(eventName);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n--- Event Status: %s (%s) ---%n", event.getName(), event.getStatus().name()));
        sb.append(buildOptionsStatus(event, engine));
        sb.append(String.format("%nTotal Commission Collected: %.2f$%n", event.getTotalCommissionCollected()));
        sb.append(buildTradingHistory(event));

        if (event.getStatus() == EventStatus.CLOSED) {
            Option winner = event.getWinningOption();
            sb.append(String.format("%nEvent is CLOSED.%nWinning Option: %s%n", winner != null ? winner.getName() : "None"));
        }
        return sb.toString();
    }

    public String getEventCurrentState(String eventName) throws Exception {
        Event event = getEvent(eventName);
        ITradingEngine engine = tradingEngines.get(eventName);
        return buildOptionsStatus(event, engine);
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

    private String buildTradingHistory(Event event) {
        StringBuilder sb = new StringBuilder();
        List<Ticket> history = event.getHistory();
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

    public double getOptionCurrentPrice(String eventName, int optionIndex) throws Exception {
        Event event = getEvent(eventName);

        if (event.getStatus() == EventStatus.CLOSED) {
            Option selectedOption = event.getOptions().get(optionIndex);
            return (event.getWinningOption() != null && event.getWinningOption().getName().equals(selectedOption.getName())) ? 1.0 : 0.0;
        }

        ITradingEngine engine = tradingEngines.get(eventName);
        if (engine == null) return 0.0;

        Option selectedOption = event.getOptions().get(optionIndex);
        return engine.getCurrentPrice(selectedOption, event.getOptions());
    }

    public double getPurchaseCostPreview(String eventName, int optionIndex, int amount) throws Exception {
        Event event = getEvent(eventName);
        ITradingEngine engine = tradingEngines.get(eventName);
        if (engine == null) return 0.0;

        Option selectedOption = event.getOptions().get(optionIndex);
        double sharesCost = engine.calculatePurchaseCost(selectedOption, amount, event.getOptions());
        double commissionCost = (event.getCommissionType() == CommissionType.ON_PURCHASE)
                ? sharesCost * (event.getCommissionRate() / 100.0) : 0.0;

        return sharesCost + commissionCost;
    }

    private com.engine.OrderBookEngine getAsOrderBookEngine(String eventName) throws Exception {
        ITradingEngine engine = tradingEngines.get(eventName);
        if (!(engine instanceof com.engine.OrderBookEngine obEngine)) {
            throw new Exception("Event '" + eventName + "' is not an Order Book event.");
        }
        return obEngine;
    }

    public Map<String, Double> getOptionOBStats(String eventName, int optionIndex) throws Exception {
        Event event = getEvent(eventName);
        String optName = event.getOptions().get(optionIndex).getName();
        Map<String, Double> stats = new HashMap<>();

        if (event.getStatus() == EventStatus.CLOSED) {
            boolean isWinner = event.getWinningOption() != null && event.getWinningOption().getName().equals(optName);
            double finalPrice = isWinner ? 1.0 : 0.0;
            stats.put("BID", 0.0); stats.put("ASK", 0.0); stats.put("LAST", finalPrice);
            stats.put("MID", finalPrice); stats.put("SPREAD", 0.0);
            return stats;
        }
        OrderBookEngine obEngine = getAsOrderBookEngine(eventName);

        double bid = obEngine.getBestBid(optName);
        double ask = obEngine.getBestAsk(optName);
        double last = obEngine.getLastPrice(optName);
        double mid = (bid > 0 && ask > 0) ? (bid + ask) / 2.0 : (bid > 0 ? bid : ask);
        double spread = (bid > 0 && ask > 0) ? Math.abs(ask - bid) : 0.0;

        stats.put("BID", bid); stats.put("ASK", ask); stats.put("LAST", last);
        stats.put("MID", mid); stats.put("SPREAD", spread);
        return stats;
    }

    public List<OBOrder> getEventBids(String eventName) throws Exception {
        Event event = getEvent(eventName);
        com.engine.OrderBookEngine obEngine = getAsOrderBookEngine(eventName);
        List<OBOrder> allBids = new ArrayList<>();
        for (Option opt : event.getOptions()) {
            List<OBOrder> bids = obEngine.getAllBids(opt.getName());
            if (bids != null) {
                bids.forEach(b -> b.setOption(opt));
                allBids.addAll(bids);
            }
        }
        return allBids;
    }

    public List<OBOrder> getEventAsks(String eventName) throws Exception {
        Event event = getEvent(eventName);
        com.engine.OrderBookEngine obEngine = getAsOrderBookEngine(eventName);
        List<OBOrder> allAsks = new ArrayList<>();
        for (Option opt : event.getOptions()) {
            List<OBOrder> asks = obEngine.getAllAsks(opt.getName());
            if (asks != null) {
                asks.forEach(a -> a.setOption(opt));
                allAsks.addAll(asks);
            }
        }
        return allAsks;
    }

    public String getOBParticipantsOverview(String eventName) throws Exception {
        Event event = getEvent(eventName);
        List<OBOrder> allBids = getEventBids(eventName);
        List<OBOrder> allAsks = getEventAsks(eventName);
        StringBuilder sb = new StringBuilder();

        for (User user : getUsers()) {
            StringBuilder userSb = new StringBuilder();
            boolean hasHoldings = appendUserHoldings(userSb, user, event);
            boolean hasOrders = appendUserOrders(userSb, user, allBids, allAsks);

            if (hasHoldings || hasOrders) {
                sb.append(user.getName()).append(":\n").append(userSb.toString()).append("\n");
            }
        }
        return sb.length() > 0 ? sb.toString() : "No active participants yet.";
    }

    private boolean appendUserHoldings(StringBuilder userSb, User user, Event event) throws Exception {
        boolean hasHoldings = false;
        com.data.users.entities.Portfolio p = user.getPortfolios().get(event.getName());
        if (p != null && p.getHoldings() != null) {
            for (int i = 0; i < event.getOptions().size(); i++) {
                Option opt = event.getOptions().get(i);
                double shares = p.getHoldings().getOrDefault(opt.getName(), 0.0);
                if (shares > 0) {
                    hasHoldings = true;
                    double currentValue = shares * getOptionCurrentPrice(event.getName(), i);
                    userSb.append(String.format("  - %.0f shares of %s (Est. Value: $%.2f)%n", shares, opt.getName(), currentValue));
                }
            }
        }
        return hasHoldings;
    }

    private boolean appendUserOrders(StringBuilder userSb, User user, List<OBOrder> allBids, List<OBOrder> allAsks) {
        boolean hasOrders = false;
        for (OBOrder bid : allBids) {
            if (bid.getUser().getName().equals(user.getName())) {
                hasOrders = true;
                userSb.append(String.format("  - [BID] %d %s @ $%.2f%n", bid.getAmount(), bid.getOption().getName(), bid.getPrice()));
            }
        }
        for (OBOrder ask : allAsks) {
            if (ask.getUser().getName().equals(user.getName())) {
                hasOrders = true;
                userSb.append(String.format("  - [ASK] %d %s @ $%.2f%n", ask.getAmount(), ask.getOption().getName(), ask.getPrice()));
            }
        }
        return hasOrders;
    }

    public EngineService getEngineService() {
        return this.engineService;
    }
}