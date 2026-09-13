package com.engine;
import com.data.events.entities.Option;
import java.util.List;

public class LmsrEngine implements ITradingEngine {
    private double b;

    public LmsrEngine(double b) {
        this.b = b;
    }

    private double calculateCostFunction(List<Option> allOptions){
        double sumC = 0.0;
        for (Option opt : allOptions){
            double exponent = opt.getTotalShares() / this.b;
            sumC += Math.exp(exponent);
        }
        return this.b * Math.log(sumC);
    }

    public double getPotValue(List<Option> allOptions) {
        return calculateCostFunction(allOptions);
    }


    public double calculatePurchaseCost(Option selectedOption, int amountToBuy, List<Option> allOptions) {
        double costBefore = calculateCostFunction(allOptions);
        selectedOption.addPurchasedShares(amountToBuy);
        double costAfter = calculateCostFunction(allOptions);
        selectedOption.removePurchasedShares(amountToBuy);
        return costAfter - costBefore;
    }

    public double getCurrentPrice(Option selectedOption, List<Option> allOptions) {
        double numerator = Math.exp(selectedOption.getTotalShares() / this.b);
        double denominatorSum = 0.0;
        for (Option opt : allOptions) {
            denominatorSum += Math.exp(opt.getTotalShares() / this.b);
        }
        return numerator / denominatorSum;
    }


}
