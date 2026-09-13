package com.engine;

import com.data.events.entities.Option;
import java.util.List;

public interface ITradingEngine {

    double calculatePurchaseCost(Option selectedOption, int amountToBuy, List<Option> allOptions);

    double getCurrentPrice(Option selectedOption, List<Option> allOptions);


}
