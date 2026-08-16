package com.GuessMarket.UI.commands;

import com.GuessMarket.UI.menu.Command;
import com.GuessMarket.maneger.SystemManager;
import com.GuessMarket.data.entities.Event;
import java.util.Scanner;

public class ShowEventInfoCommand implements Command {
    private SystemManager manager;
    private Scanner scanner;

    public ShowEventInfoCommand(SystemManager manager, Scanner scanner) {
        this.manager = manager;
        this.scanner = scanner;
    }

    @Override
    public String getName() {
        return "Show Event Trading Status";
    }

    @Override
    public boolean execute() {
        try {
            Event selectedEvent = CommandUtils.pickActiveEvent(manager, scanner);
            if (selectedEvent == null) return true;

            String detailedStatus = manager.getEventDetailedStatus(selectedEvent.getId());
            System.out.println(detailedStatus);

        } catch (NumberFormatException e) {
            System.out.println("Status: Error - Please enter a valid number.");
        } catch (Exception e) {
            System.out.println(String.format("Status: Error - %s", e.getMessage()));
        }
        return true;
    }
}