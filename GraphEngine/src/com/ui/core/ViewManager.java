
package com.ui.core;

import com.ui.controllers.EventController;
import com.ui.controllers.UserController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class ViewManager {
    private static final Duration PULL_INTERVAL = Duration.seconds(1);

    private final BorderPane rootLayout;
    private final Scene mainScene;

    private final Map<String, Node> screens;
    private final Map<String, Object> controllers;

    private Timeline pullTimeline;
    private boolean loggedIn = false;
    private String username;

    public ViewManager() {
        this.rootLayout = new BorderPane();
        this.screens = new HashMap<>();
        this.controllers = new HashMap<>();
        this.mainScene = new Scene(rootLayout, 1024, 768);
    }

    public Scene getMainScene() { return mainScene; }

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

    public Object getController(String screenName) { return controllers.get(screenName); }


    public boolean isLoggedIn() { return loggedIn; }
    public String getUsername() { return username; }

    public void onLoggedIn(String username) {
        this.username = username;
        this.loggedIn = true;
        resetScreens();
        navigateTo("MARKET_DASHBOARD");
        refreshAllScreens();
        startPolling();
    }

    public void startPolling() {
        if (pullTimeline != null) return;
        pullTimeline = new Timeline(new KeyFrame(PULL_INTERVAL, e -> refreshAllScreens()));
        pullTimeline.setCycleCount(Animation.INDEFINITE);
        pullTimeline.play();
    }

    public void stopPolling() {
        if (pullTimeline != null) {
            pullTimeline.stop();
            pullTimeline = null;
        }
    }

    /** Must run on the FX thread. */
    private void resetScreens() {
        for (Object controller : controllers.values()) {
            if (controller instanceof UserController u) u.resetSession();
            else if (controller instanceof EventController e) e.resetSession();
        }
    }

    /** Every screen re-pulls its data. No-op until the user is logged in (the server would answer 401). */
    public void refreshAllScreens() {
        if (!loggedIn) return;
        for (Object controller : controllers.values()) {
            if (controller instanceof IRefreshable) {
                ((IRefreshable) controller).refreshData();
            }
        }
    }


    public boolean handleUnauthorized(okhttp3.Response response) {
        if (response.code() != 401) return false;
        response.close();                       // release the connection

        if (loggedIn) {
            loggedIn = false;
            stopPolling();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Session Expired");
                alert.setHeaderText(null);
                alert.setContentText("Please log in again.");
                alert.show();
                resetScreens();
                navigateTo("LOGIN_DASHBOARD");
            });
        }
        return true;
    }

    public void performLogout() {
        this.loggedIn = false;
        this.username = null;
        stopPolling();
        ApiClient.clearSession(); // Wipe the cookies
        Platform.runLater(() -> {
            resetScreens();
            navigateTo("LOGIN_DASHBOARD");
        });
    }
}

