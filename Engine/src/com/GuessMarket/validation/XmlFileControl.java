package com.GuessMarket.validation;

import com.GuessMarket.data.entities.Event;
import com.GuessMarket.data.entities.Market;
import java.util.HashSet;
import java.util.Set;

public abstract class XmlFileControl {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int MIN_OPTIONS = 2;

    public static void validateMarketData(Market market) throws Exception {
        if (market == null || market.getEvents() == null || market.getEvents().isEmpty()) {
            throw new Exception("Validation Error: The XML file is empty or missing events.");
        }

        Set<String> existingIds = new HashSet<>();

        for (Event event : market.getEvents()) {
            if (!existingIds.add(event.getId())) {
                throw new Exception(String.format("Validation Error: Duplicate Event ID found: '%s'. Event IDs must be unique.", event.getId()));
            }

            if (event.getCommissionRate() < MIN_COMMISSION || event.getCommissionRate() > MAX_COMMISSION) {
                throw new Exception(String.format("Validation Error: Commission rate for event '%s' must be between %d and %d.", event.getName(), MIN_COMMISSION, MAX_COMMISSION));
            }

            if (event.getOptions() == null || event.getOptions().size() < MIN_OPTIONS) {
                throw new Exception(String.format("Validation Error: Event '%s' must have at least %d options.", event.getName(), MIN_OPTIONS));
            }
        }
    }
}