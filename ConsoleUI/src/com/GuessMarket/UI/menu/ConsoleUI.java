package com.GuessMarket.UI.menu;

import com.GuessMarket.maneger.SystemManager;
import com.GuessMarket.UI.commands.*;

import java.util.Scanner;

public class ConsoleUI {
    private SystemManager manager;

    public ConsoleUI() {
        this.manager = new SystemManager();
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        MenuFolder mainMenu = new MenuFolder("Main Menu", scanner);
        mainMenu.addItem(new LoadXmlCommand(manager, scanner));
        mainMenu.addItem(new SaveXMLCommand(manager, scanner));
        mainMenu.addItem(new ShowEventsCommand(manager));
        mainMenu.addItem(new ShowEventInfoCommand(manager, scanner));
        mainMenu.addItem(new BuySharesCommand(manager, scanner));
        mainMenu.addItem(new CloseEventCommand(manager, scanner));

        System.out.println("Welcome to Guess Market");
        mainMenu.execute();

        System.out.println("Goodbye");
        scanner.close();
    }
}

