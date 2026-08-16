package com.GuessMarket.UI.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MenuFolder implements MenuItem {
    private String folderName;
    private List<MenuItem> items;
    private Scanner scanner;

    public MenuFolder(String folderName, Scanner scanner) {
        this.folderName = folderName;
        this.items = new ArrayList<>();
        this.scanner = scanner;
    }
    public void addItem(MenuItem item) {
        this.items.add(item);
    }

    @Override
    public String getName() {
        return this.folderName;
    }

    @Override
    public boolean execute() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== " + folderName + " ===");
            for (int i = 0; i < items.size(); i++) {
                System.out.println((i + 1) + ". " + items.get(i).getName());
            }
            int exitChoice = items.size() + 1;
            System.out.println(exitChoice + ". Exit / Back");
            System.out.print("Please select an option (1-" + exitChoice + "): ");
            try {
                int choice = Integer.parseInt(scanner.nextLine());

                if (choice == exitChoice) {
                    back = true;
                }
                else if (choice > 0 && choice <= items.size()) {
                    items.get(choice - 1).execute();
                }
                else {
                    System.out.println("Invalid option, try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
        return true;
    }
}
