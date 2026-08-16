package com.GuessMarket.UI.commands;

import com.GuessMarket.UI.menu.Command;
import com.GuessMarket.maneger.SystemManager;
import com.GuessMarket.data.entities.Event;
import java.util.Scanner;

public class CloseEventCommand implements Command {
    private SystemManager manager;
    private Scanner scanner;

    public CloseEventCommand(SystemManager manager, Scanner scanner) {
        this.manager = manager;
        this.scanner = scanner;
    }

    @Override
    public String getName() {
        return "Close an Event";
    }

    @Override
    public boolean execute() {
        try {
            Event selectedEvent = CommandUtils.pickActiveEvent(manager, scanner);
            if (selectedEvent == null) return true;

            String eventId = selectedEvent.getId();

            System.out.println("\n--- Event Details Before Closure ---");
            System.out.println(manager.getEventDetailedStatus(eventId));

            int winningOptionIndex = CommandUtils.pickOption(selectedEvent, scanner);
            if (winningOptionIndex == -1) return true;

            String summary = manager.closeEvent(eventId, winningOptionIndex);
            System.out.println("\n--- Closure Summary ---");
            System.out.println(summary);
            System.out.println("Status: Event closed successfully!");

            System.out.println("\n--- Final Event Status (After Closure) ---");
            System.out.println(manager.getEventDetailedStatus(eventId));

        } catch (NumberFormatException e) {
            System.out.println("Status: Error - Invalid input. Please enter valid numbers only.");
        } catch (Exception e) {
            System.out.println(String.format("Status: Error - %s", e.getMessage()));
        }

        return true;
    }
}