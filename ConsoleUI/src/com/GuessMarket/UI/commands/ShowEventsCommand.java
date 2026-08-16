package com.GuessMarket.UI.commands;

import com.GuessMarket.UI.menu.Command;
import com.GuessMarket.maneger.SystemManager;
import com.GuessMarket.data.entities.Event;
import java.util.List;

public class ShowEventsCommand implements Command {
    private SystemManager manager;

    public ShowEventsCommand(SystemManager manager) {
        this.manager = manager;
    }

    @Override
    public String getName() {
        return "Show All Events";
    }

    @Override
    public boolean execute() {
        System.out.println(String.format("%n--- All Events ---"));
        try {
            List<Event> allEvents = manager.getEvents();
            if (allEvents.isEmpty()) {
                System.out.println(String.format("Status: No events are currently loaded in the system."));
                return true;
            }

            for (Event event : allEvents) {
                String eventInfo = manager.getEventBasicInfo(event.getId());
                System.out.print(eventInfo);
            }

            System.out.println(String.format("Status: Events displayed successfully."));
        } catch (Exception e) {
            System.out.println(String.format("Status: Error displaying events - %s", e.getMessage()));
        }
        return true;
    }
}