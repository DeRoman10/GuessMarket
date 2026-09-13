package com.ui.core;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import java.util.HashMap;
import java.util.Map;

public class ViewManager {
    private final BorderPane rootLayout;
    private final Scene mainScene;

    private final Map<String, Node> screens;
    private final Map<String, Object> controllers;

    public ViewManager() {
        this.rootLayout = new BorderPane();
        this.screens = new HashMap<>();
        this.controllers = new HashMap<>();
        this.mainScene = new Scene(rootLayout, 1024, 768);
    }

    public Scene getMainScene() {
        return mainScene;
    }

    public void addScreen(String screenName, Node screenNode, Object controller) {
        screens.put(screenName, screenNode);
        controllers.put(screenName, controller);
    }

    public void navigateTo(String screenName) {
        Node nextScreen = screens.get(screenName);
        if (nextScreen != null) {
            rootLayout.setCenter(nextScreen);
        } else {
            System.err.println("Error: Screen '" + screenName + "' not found!");
        }
    }

    public Object getController(String screenName) {
        return controllers.get(screenName);
    }

    public void refreshAllScreens() {
        for (Object controller : controllers.values()) {
            if (controller instanceof IRefreshable) {
                ((IRefreshable) controller).refreshData();
            }
        }
    }

    public void changeTheme(String cssFileName) {
        mainScene.getStylesheets().clear();
        String cssPath = getClass().getResource("/com/ui/css/" + cssFileName).toExternalForm();
        mainScene.getStylesheets().add(cssPath);
    }
}