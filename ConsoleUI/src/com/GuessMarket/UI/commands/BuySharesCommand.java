package com.GuessMarket.UI.commands;

import com.GuessMarket.UI.menu.Command;
import com.GuessMarket.maneger.*;
import com.GuessMarket.data.entities.Event;
import java.util.Scanner;

public class BuySharesCommand implements Command {
    private SystemManager manager;
    private Scanner scanner;

    public BuySharesCommand(SystemManager manager, Scanner scanner) {
        this.manager = manager;
        this.scanner = scanner;
    }

    @Override
    public String getName() {
        return "Participate in an Event (Buy Shares)";
    }

    @Override
    public boolean execute() {
        try {
            Event selectedEvent = CommandUtils.pickActiveEvent(manager, scanner);
            if (selectedEvent == null) return true;

            System.out.println("%n--- Event Current State ---");
            System.out.print(manager.getEventCurrentState(selectedEvent.getId()));

            int optionIndex = CommandUtils.pickOption(selectedEvent, scanner);
            if (optionIndex == -1) return true;

            System.out.print("Enter amount of shares to buy: ");
            int amount = Integer.parseInt(scanner.nextLine());

            if (amount <= 0) {
                System.out.println("Status: Error - Amount must be greater than zero.");
                return true;
            }

            TradeMonitor monitor = manager.executePurchase(selectedEvent.getId(), optionIndex, amount);

            PurchaseReceipt receipt = monitor.waitForResult();
            System.out.print(receipt.getReceiptAsString(selectedEvent.getCommissionRate()));

            System.out.println("%--- Event Current State (After Purchase) ---");
            System.out.print(manager.getEventCurrentState(selectedEvent.getId()));
            System.out.println("Status: Purchase executed successfully.");

        } catch (NumberFormatException e) {
            System.out.println("Status: Error - Invalid input. Please enter a valid number.");
        } catch (Exception e) {
            System.out.println(String.format("Status: Error during purchase - %s", e.getMessage()));
        }
        return true;
    }
}