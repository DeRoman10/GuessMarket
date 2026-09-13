package com.engine;

import com.data.events.entities.Event;
import com.data.events.entities.OBOrder;
import com.data.events.entities.Option;
import com.data.events.entities.Ticket;
import com.data.events.enums.OrderDirection;
import com.data.events.enums.TicketAction;
import com.data.users.entities.Portfolio;
import com.data.users.entities.User;
import com.engine.logic.CustomOrderHeap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class OrderBookEngine implements ITradingEngine {
    private int baseValueD;
    private boolean allowMint;

    private Map<String, CustomOrderHeap> bids;
    private Map<String, CustomOrderHeap> asks;
    private AtomicLong sequenceGenerator = new AtomicLong(0);

    private Map<String, Double> lastPrices = new HashMap<>();

    public OrderBookEngine(int baseValueD, boolean allowMint) {
        this.baseValueD = baseValueD;
        this.allowMint = allowMint;
        this.bids = new HashMap<>();
        this.asks = new HashMap<>();
    }

    private void initializeOption(String optionName) {
        if (!bids.containsKey(optionName)) {
            bids.put(optionName, new CustomOrderHeap((o1, o2) -> {
                int priceCompare = Double.compare(o2.getPrice(), o1.getPrice());
                return priceCompare != 0 ? priceCompare : Long.compare(o1.getSequenceId(), o2.getSequenceId());
            }));

            asks.put(optionName, new CustomOrderHeap((o1, o2) -> {
                int priceCompare = Double.compare(o1.getPrice(), o2.getPrice());
                return priceCompare != 0 ? priceCompare : Long.compare(o1.getSequenceId(), o2.getSequenceId());
            }));
        }
    }

    public void processOrder(OBOrder newOrder, Option oppositeOption, Event event) throws Exception {
        String optName = newOrder.getOption().getName();
        initializeOption(optName);

        newOrder.setSequenceId(sequenceGenerator.getAndIncrement());

        if (newOrder.getDirection() == OrderDirection.BUY) {
            matchBuyOrder(newOrder, asks.get(optName), event);

            if (newOrder.getAmount() > 0 && allowMint && oppositeOption != null) {
                tryMint(newOrder, oppositeOption, event);
            }

            if (newOrder.getAmount() > 0) {
                bids.get(optName).insert(newOrder);
            }
        } else {
            matchSellOrder(newOrder, bids.get(optName), event);

            if (newOrder.getAmount() > 0) {
                asks.get(optName).insert(newOrder);
            }
        }
    }

    private void matchBuyOrder(OBOrder buyer, CustomOrderHeap askHeap, Event event) throws Exception {
        while (buyer.getAmount() > 0 && !askHeap.isEmpty()) {
            OBOrder topSeller = askHeap.peek();

            if (buyer.getPrice() >= topSeller.getPrice()) {
                int tradeAmount = Math.min(buyer.getAmount(), topSeller.getAmount());
                double executionPrice = topSeller.getPrice();

                executeTrade(buyer, topSeller, tradeAmount, executionPrice, event);
                buyer.setAmount(buyer.getAmount() - tradeAmount);
                topSeller.setAmount(topSeller.getAmount() - tradeAmount);

                if (topSeller.getAmount() == 0) {
                    askHeap.extractTop();
                }
            } else {
                break;
            }
        }
    }

    private void matchSellOrder(OBOrder seller, CustomOrderHeap bidHeap, Event event) throws Exception {
        while (seller.getAmount() > 0 && !bidHeap.isEmpty()) {
            OBOrder topBuyer = bidHeap.peek();

            if (seller.getPrice() <= topBuyer.getPrice()) {
                int tradeAmount = Math.min(seller.getAmount(), topBuyer.getAmount());
                double executionPrice = topBuyer.getPrice();

                executeTrade(topBuyer, seller, tradeAmount, executionPrice, event);
                seller.setAmount(seller.getAmount() - tradeAmount);
                topBuyer.setAmount(topBuyer.getAmount() - tradeAmount);

                if (topBuyer.getAmount() == 0) {
                    bidHeap.extractTop();
                }
            } else {
                break;
            }
        }
    }

    private void executeTrade(OBOrder buyerOrder, OBOrder sellerOrder, int tradeAmount, double executionPrice, Event event) throws Exception {
        User buyer = buyerOrder.getUser();
        User seller = sellerOrder.getUser();
        Option option = buyerOrder.getOption();
        String eventId = event.getId();

        double grossCost = tradeAmount * executionPrice;
        double commission = 0.0;

        if (event.getCommissionType() == com.data.events.enums.CommissionType.ON_PURCHASE) {
            commission = grossCost * (event.getCommissionRate() / 100.0);
        }

        double diff = (buyerOrder.getPrice() - executionPrice) * tradeAmount;
        if (diff > 0) buyer.deposit(diff);

        if (commission > 0) {
            buyer.withdraw(commission);
            if (event.getMmAccount() != null) {
                event.getMmAccount().deposit(commission);
                event.addCommission(commission);
            }
        }

        seller.deposit(grossCost);


        Portfolio buyerPortfolio = buyer.getPortfolios().computeIfAbsent(eventId, k -> new Portfolio());
        Portfolio sellerPortfolio = seller.getPortfolios().computeIfAbsent(eventId, k -> new Portfolio());

        buyerPortfolio.getHoldings().put(option.getName(), buyerPortfolio.getHoldings().getOrDefault(option.getName(), 0.0) + tradeAmount);
        buyerPortfolio.addCommissionPaid(commission);
        buyerPortfolio.setTotalInvestment(buyerPortfolio.getTotalInvestment() + grossCost + commission);

        Ticket buyerTicket = new Ticket(option, tradeAmount, executionPrice, TicketAction.BUY);
        Ticket sellerTicket = new Ticket(option, tradeAmount, executionPrice, TicketAction.SELL);

        buyerPortfolio.getHistory().add(buyerTicket);
        sellerPortfolio.getHistory().add(sellerTicket);

        TicketAction globalAction = (buyerOrder.getDirection() == OrderDirection.BUY) ? TicketAction.BUY : TicketAction.SELL;
        event.addTicketToHistory(new Ticket(option, tradeAmount, executionPrice, globalAction));

        lastPrices.put(option.getName(), executionPrice);
    }


    @Override
    public double calculatePurchaseCost(Option selectedOption, int amount, List<Option> allOptions) {
        String optName = selectedOption.getName();
        CustomOrderHeap optionAsks = asks.get(optName);

        if (optionAsks == null || optionAsks.isEmpty()) {
            return 0.0;
        }
        java.util.List<OBOrder> tempOrders = new java.util.ArrayList<>();

        double totalCost = 0.0;
        int remainingAmount = amount;

        while (remainingAmount > 0 && !optionAsks.isEmpty()) {
            OBOrder topAsk = optionAsks.extractTop();
            tempOrders.add(topAsk);

            int takeAmount = Math.min(remainingAmount, topAsk.getAmount());
            totalCost += takeAmount * topAsk.getPrice();
            remainingAmount -= takeAmount;
        }
        for (OBOrder order : tempOrders) {
            optionAsks.insert(order);
        }
        if (remainingAmount > 0) {
            return 0.0;
        }
        return totalCost;
    }

    @Override
    public double getCurrentPrice(Option selectedOption, List<Option> allOptions) {
        String optName = selectedOption.getName();

        CustomOrderHeap optionBids = bids.get(optName);
        CustomOrderHeap optionAsks = asks.get(optName);

        double bestBid = (optionBids != null && !optionBids.isEmpty()) ? optionBids.peek().getPrice() : 0.0;
        double bestAsk = (optionAsks != null && !optionAsks.isEmpty()) ? optionAsks.peek().getPrice() : 0.0;

        if (bestBid > 0 && bestAsk > 0) {
            return (bestBid + bestAsk) / 2.0;
        }
        else if (bestBid > 0) {
            return bestBid;
        } else if (bestAsk > 0) {
            return bestAsk;
        }
        return 0.0;
    }

    public double getBestBid(String optionName) {
        CustomOrderHeap optionBids = bids.get(optionName);
        return (optionBids != null && !optionBids.isEmpty()) ? optionBids.peek().getPrice() : 0.0;
    }

    public double getBestAsk(String optionName) {
        CustomOrderHeap optionAsks = asks.get(optionName);
        return (optionAsks != null && !optionAsks.isEmpty()) ? optionAsks.peek().getPrice() : 0.0;
    }

    public double getLastPrice(String optName) {
        return lastPrices.getOrDefault(optName, 0.0);
    }

    public List<OBOrder> getAllBids(String optName) {
        CustomOrderHeap optionBids = bids.get(optName);
        return optionBids != null ? optionBids.getElements() : new java.util.ArrayList<>();
    }

    public List<OBOrder> getAllAsks(String optName) {
        CustomOrderHeap optionAsks = asks.get(optName);
        return optionAsks != null ? optionAsks.getElements() : new java.util.ArrayList<>();
    }

    public double getSpread(String optionName) {
        double bid = getBestBid(optionName);
        double ask = getBestAsk(optionName);
        if (bid > 0 && ask > 0) {
            return ask - bid;
        }
        return 0.0;
    }

    private void tryMint(OBOrder newBuyer, Option oppositeOption, Event event) throws Exception {
        String oppName = oppositeOption.getName();
        initializeOption(oppName);
        CustomOrderHeap oppositeBids = bids.get(oppName);

        while (newBuyer.getAmount() > 0 && !oppositeBids.isEmpty()) {
            OBOrder restingBuyer = oppositeBids.peek();

            if (newBuyer.getPrice() + restingBuyer.getPrice() < baseValueD) {
                break;
            }

            int mintAmount = Math.min(newBuyer.getAmount(), restingBuyer.getAmount());
            double restingPrice = restingBuyer.getPrice();
            double newBuyerPaidPrice = baseValueD - restingPrice;

            double[] commissions = processMintPayments(newBuyer, restingBuyer, mintAmount, newBuyerPaidPrice, restingPrice, event);

            updateUserPortfolio(newBuyer, event, mintAmount, newBuyerPaidPrice * mintAmount, commissions[0]);
            updateUserPortfolio(restingBuyer, event, mintAmount, restingPrice * mintAmount, commissions[1]);

            recordMintHistory(newBuyer, restingBuyer, oppositeOption, mintAmount, newBuyerPaidPrice, restingPrice, event);

            newBuyer.setAmount(newBuyer.getAmount() - mintAmount);
            restingBuyer.setAmount(restingBuyer.getAmount() - mintAmount);

            if (restingBuyer.getAmount() == 0) {
                oppositeBids.extractTop();
            }
        }
    }

    private double[] processMintPayments(OBOrder newBuyer, OBOrder restingBuyer, int mintAmount, double newBuyerPaidPrice, double restingPrice, Event event) {
        event.depositToContract(baseValueD * mintAmount); // CHANGED — was event.getMmAccount().deposit(...)

        double diff = (newBuyer.getPrice() - newBuyerPaidPrice) * mintAmount;
        if (diff > 0) {
            newBuyer.getUser().deposit(diff);
        }

        double newBuyerCost = newBuyerPaidPrice * mintAmount;
        double restingBuyerCost = restingPrice * mintAmount;
        double newBuyerComm = 0.0;
        double restingBuyerComm = 0.0;

        if (event.getCommissionType() == com.data.events.enums.CommissionType.ON_PURCHASE) {
            double rate = event.getCommissionRate() / 100.0;
            newBuyerComm = newBuyerCost * rate;
            restingBuyerComm = restingBuyerCost * rate;

            if (event.getMmAccount() != null) {
                event.getMmAccount().deposit(newBuyerComm + restingBuyerComm);
                event.addCommission(newBuyerComm + restingBuyerComm);
            }

            newBuyer.getUser().withdraw(newBuyerComm);
            restingBuyer.getUser().withdraw(restingBuyerComm);
        }

        return new double[]{newBuyerComm, restingBuyerComm};
    }

    private void updateUserPortfolio(OBOrder buyer, Event event, int mintAmount, double cost, double commission) {
        Portfolio portfolio = buyer.getUser().getPortfolios().computeIfAbsent(event.getId(), k -> new com.data.users.entities.Portfolio());
        String optionName = buyer.getOption().getName();

        double currentHoldings = portfolio.getHoldings().getOrDefault(optionName, 0.0);
        portfolio.getHoldings().put(optionName, currentHoldings + mintAmount);

        portfolio.setTotalInvestment(portfolio.getTotalInvestment() + cost + commission);
        portfolio.addCommissionPaid(commission);
    }

    private void recordMintHistory(OBOrder newBuyer, OBOrder restingBuyer, Option oppositeOption, int mintAmount, double newBuyerPaidPrice, double restingPrice, Event event) {
        newBuyer.getOption().addPurchasedShares(mintAmount);
        oppositeOption.addPurchasedShares(mintAmount);

        event.addTicketToHistory(new Ticket(newBuyer.getOption(), mintAmount, newBuyerPaidPrice));
        event.addTicketToHistory(new Ticket(restingBuyer.getOption(), mintAmount, restingPrice));

        lastPrices.put(newBuyer.getOption().getName(), newBuyerPaidPrice);
        lastPrices.put(oppositeOption.getName(), restingPrice);
    }
}