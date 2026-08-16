package com.GuessMarket.UI.commands;

import com.GuessMarket.UI.menu.Command;
import com.GuessMarket.maneger.SystemManager;
import java.util.Scanner;

public class LoadXmlCommand implements Command {
    private SystemManager manager;
    private Scanner scanner;

    public LoadXmlCommand(SystemManager manager, Scanner scanner) {
        this.manager = manager;
        this.scanner = scanner;
    }

    @Override
    public String getName() {
        return "Load XML File";
    }

    @Override
    public boolean execute() {
        System.out.print("Enter XML file path: ");
        String xmlPath = scanner.nextLine();

        try {
            manager.loadXmlData(xmlPath);
            System.out.println("Status: File loaded successfully.");
        }
        catch (Exception e) {
            System.out.println("Status: Error loading - " + e.toString());
        }

        return true;
    }

}
