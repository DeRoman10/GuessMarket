package com.GuessMarket.UI.commands;

import com.GuessMarket.UI.menu.Command;
import com.GuessMarket.maneger.SystemManager;
import java.util.Scanner;

public class SaveXMLCommand implements Command {
    private SystemManager manager;
    private Scanner scanner;

    public SaveXMLCommand(SystemManager manager, Scanner scanner) {
        this.manager = manager;
        this.scanner = scanner;
    }

    @Override
    public String getName() {
        return "Save System State to XML";
    }

    @Override
    public boolean execute() {
        System.out.print("Enter full path to save XML file (e.g., saved_market.xml): ");
        String xmlPath = scanner.nextLine().trim();

        // Strip quotes just in case the user used Windows "Copy as path"
        if (xmlPath.startsWith("\"") && xmlPath.endsWith("\"")) {
            xmlPath = xmlPath.substring(1, xmlPath.length() - 1);
        }

        try {
            manager.saveXmlData(xmlPath);
            System.out.println("Status: System state successfully saved to XML.");
        }
        catch (Exception e) {
            System.out.println("Status: Error saving file - " + e.getMessage());
        }

        return true;
    }
}