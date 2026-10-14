
package com.handlers;

import com.data.events.entities.Event;
import com.data.events.entities.Option;
import com.data.files.entities.Market;

import java.util.HashSet;
import java.util.Set;

public abstract class XmlFileControl {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int MIN_OPTIONS = 2;

    public static void validateFileName(String fileName) throws Exception {
        if (fileName == null || !fileName.trim().toLowerCase().endsWith(".xml")) {
            throw new Exception("Validation Error: the file must be an XML file (.xml).");
        }
    }

    public static void validateMarketData(Market market) throws Exception {
        if (market == null || market.getEvents() == null || market.getEvents().isEmpty()) {
            throw new Exception("Validation Error: The XML file is empty or missing events.");
        }

        Set<String> existingNames = new HashSet<>();
        for (Event event : market.getEvents()) {
            String name = event.getName();
            if (name == null || name.isBlank()) {
                throw new Exception("Validation Error: an event without a name was found.");
            }

            if (!existingNames.add(name)) {
                throw new Exception(String.format("Validation Error: Duplicate Event Name found: '%s'. Event names must be unique.", name));
            }

            if (event.getCommissionRate() < MIN_COMMISSION || event.getCommissionRate() > MAX_COMMISSION) {
                throw new Exception(String.format("Validation Error: Commission rate for event '%s' must be between %d and %d.", name, MIN_COMMISSION, MAX_COMMISSION));
            }

            if (event.getOptions() == null || event.getOptions().size() < MIN_OPTIONS) {
                throw new Exception(String.format("Validation Error: Event '%s' must have at least %d options.", name, MIN_OPTIONS));
            }
            Set<String> optionNames = new HashSet<>();
            for (Option o : event.getOptions()) {
                if (o.getName() == null || o.getName().isBlank() || !optionNames.add(o.getName().trim().toLowerCase())) {
                    throw new Exception(String.format("Validation Error: Event '%s' has an empty or duplicate option name.", name));
                }
            }

            if (!event.isLmsr() && !event.isOrderBook()) {
                throw new Exception(String.format("Validation Error: Event '%s' has no trading method.", name));
            }
            if (event.isLmsr() && event.getMethod().getLmsr().getB() <= 0) {
                throw new Exception(String.format("Validation Error: Event '%s': liquidity (b) must be a positive number.", name));
            }
            if (event.isOrderBook()) {
                if (event.getMethod().getOrderBook().getD() <= 0) {
                    throw new Exception(String.format("Validation Error: Event '%s': base value (d) must be a positive number.", name));
                }
                if (event.getMethod().getOrderBook().getInitial() < 0) {
                    throw new Exception(String.format("Validation Error: Event '%s': initial investment cannot be negative.", name));
                }
            }
        }
    }
}

