package com.ui.core;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import okhttp3.Call;
import okhttp3.Response;

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

    public void changeTheme(String cssFileName) {
        mainScene.getStylesheets().clear();
        String cssPath = getClass().getResource("/com/ui/css/" + cssFileName).toExternalForm();
        mainScene.getStylesheets().add(cssPath);
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

    public boolean handleUnauthorized(int statusCode) {
        if (statusCode == 401) {
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("Session Expired");
                alert.setHeaderText(null);
                alert.setContentText("Please log in again.");
                alert.showAndWait();

                navigateTo("LOGIN_DASHBOARD");
            });
            return true; // Signals to the caller that the request was intercepted
        }
        return false;
    }

    private void startAutoRefresh() {
        Timeline pullTimeline = new Timeline(new KeyFrame(Duration.seconds(1.5), event -> {
            refreshAllScreens();
        }));
        pullTimeline.setCycleCount(Animation.INDEFINITE);
        pullTimeline.play();
    }
}