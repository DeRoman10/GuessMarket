package com.GuessMarket.UI.commands;

import com.GuessMarket.maneger.SystemManager;
import com.GuessMarket.data.entities.Event;
import com.GuessMarket.data.entities.IReadOnlyOptions;
import java.util.List;
import java.util.Scanner;

public abstract class CommandUtils {

    public static Event pickActiveEvent(SystemManager manager, Scanner scanner) throws Exception {
        List<Event> activeEvents = manager.getActiveEvents();
        return pickEventFromList(activeEvents, "an Active Event", scanner);
    }

    public static Event pickAnyEvent(SystemManager manager, Scanner scanner) throws Exception {
        List<Event> allEvents = manager.getEvents();
        return pickEventFromList(allEvents, "an Event", scanner);
    }

    private static Event pickEventFromList(List<Event> events, String title, Scanner scanner) throws Exception {
        if (events.isEmpty()) {
            throw new Exception("No events are currently available.");
        }

        System.out.println(String.format("%n--- Select %s ---", title));
        int index = 1;
        for (Event ev : events) {
            System.out.printf("%d. %s (ID: %s)%n", index++, ev.getName(), ev.getId());
        }

        int cancelOption = events.size() + 1;
        System.out.println(String.format("%d. Cancel / Go Back", cancelOption));
        System.out.print(String.format("Select Event by its Serial Number (1-%d): ", cancelOption));

        int choice = Integer.parseInt(scanner.nextLine());
        if (choice == cancelOption)
            return null;

        if (choice < 1 || choice > events.size()) {
            throw new Exception("Invalid event selection.");
        }
        return events.get(choice - 1);
    }

    public static int pickOption(Event event, Scanner scanner) throws Exception {
        List<IReadOnlyOptions> readOnlyOptions = event.getReadOnlyOptions();

        System.out.println(String.format("%n--- Select Option ---"));
        int index = 1;
        for (IReadOnlyOptions opt : readOnlyOptions) {
            System.out.printf("%d. %s%n", index++, opt.getName());
        }

        int cancelOption = readOnlyOptions.size() + 1;
        System.out.println(String.format("%d. Cancel / Go Back", cancelOption));
        System.out.print(String.format("Select Option by Serial Number (1-%d): ", cancelOption));

        int choice = Integer.parseInt(scanner.nextLine());
        if (choice == cancelOption)
            return -1;

        if (choice < 1 || choice > readOnlyOptions.size()) {
            throw new Exception("Invalid option selection.");
        }
        return choice - 1;
    }
}