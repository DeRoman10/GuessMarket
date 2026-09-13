package com.maneger;

public class PurchaseReceipt implements IReceipt {
    private double sharesCost;
    private double commission;
    private double totalCost;

    public PurchaseReceipt(double sharesCost, double commission, double totalCost) {
        this.sharesCost = sharesCost;
        this.commission = commission;
        this.totalCost = totalCost;
    }

    public double getSharesCost() { return sharesCost; }
    public double getCommission() { return commission; }
    public double getTotalCost() { return totalCost; }

    public String getReceiptAsString(int commissionRate) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n--- Purchase Receipt ---%n"));
        sb.append(String.format("Cost of Shares: %.2f$%n", this.sharesCost));

        if (this.commission > 0) {
            sb.append(String.format("Commission Charged (%d%%): %.2f$%n", commissionRate, this.commission));
        }

        sb.append(String.format("Total Amount Paid: %.2f$%n", this.totalCost));
        return sb.toString();
    }
}