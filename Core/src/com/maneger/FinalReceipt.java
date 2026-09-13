package com.maneger;

public class FinalReceipt implements IReceipt {
    private String eventName;
    private String winningOption;
    private double totalPayout;
    private double totalCommission;

    public FinalReceipt(String eventName, String winningOption, double totalPayout, double totalCommission) {
        this.eventName = eventName;
        this.winningOption = winningOption;
        this.totalPayout = totalPayout;
        this.totalCommission = totalCommission;
    }

    @Override
    public String getReceiptAsString(int commissionRate) {
        return String.format(
                "Event '%s' is officially CLOSED.%nWinning Option: %s%nTotal Net Payout Distributed: %.2f$%nTotal Commission Earned by MM: %.2f$",
                eventName, winningOption, totalPayout, totalCommission
        );
    }
}