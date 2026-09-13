package com.handlers;

import com.data.events.entities.Event;
import com.data.files.entities.Market;
import com.data.users.entities.User;

import java.util.*;

public abstract class XmlFileControl {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int MIN_OPTIONS = 2;
    private static final double NOT_POSITIVE_BALANCE = 0;

    public static void validateMarketData(Market market) throws Exception {
        if (market == null || market.getEvents() == null || market.getEvents().isEmpty()) {
            throw new Exception("Validation Error: The XML file is empty or missing events.");
        }

        Set<String> existingIds = new HashSet<>();
        Set<String> existingUserNames = new HashSet<>();
        Map<String, Integer> eventMmCount = new HashMap<>();

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
            eventMmCount.put(event.getId(), 0);
        }

        if (market.getUsers() == null || market.getUsers().isEmpty()) {
            throw new Exception("Validation Error: The XML file is missing user definitions.");
        }

        for (User user : market.getUsers()) {

            if (!existingUserNames.add(user.getName())) {
                throw new Exception(String.format("Validation Error: Duplicate user name found: '%s'.", user.getName()));
            }

            if (user.getBalance() <= NOT_POSITIVE_BALANCE) {
                throw new Exception(String.format("Validation Error: User '%s' must have an initial balance > 0.", user.getName()));
            }

            if (user.getManagedEvents() != null) {
                for (String managedEventId : user.getManagedEvents()) {
                    if (!eventMmCount.containsKey(managedEventId)) {
                        throw new Exception(String.format("Validation Error: User '%s' is assigned as MM for a non-existent event ID: %s.", user.getName(), managedEventId));
                    }
                    eventMmCount.put(managedEventId, eventMmCount.get(managedEventId) + 1);
                }
            }
        }

        for (Event event : market.getEvents()) {
            int mmCount = eventMmCount.get(event.getId());
            if (mmCount != 1) {
                throw new Exception(String.format(
                        "Validation Error: Event '%s' must be assigned exactly one Market Maker (found %d).",
                        event.getName(), mmCount));
            }
        }
    }
}
