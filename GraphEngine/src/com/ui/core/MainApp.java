package com.ui.core;

import com.maneger.SystemManager;
import com.ui.controllers.*;
import javafx.animation.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.Scanner;

public class MainApp extends Application {

    private ViewManager viewManager;
    private SystemManager systemManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize the core managers
        viewManager = new ViewManager();
        systemManager = new SystemManager();

        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/ui/pages/mainLayout.fxml"));
        Parent topBarNode = mainLoader.load();
        MainController mainController = mainLoader.getController();
        mainController.setManagers(systemManager, viewManager);

        FXMLLoader eventLoader = new FXMLLoader(getClass().getResource("/com/ui/pages/eventPage.fxml"));
        Parent marketViewNode = eventLoader.load();
        EventController eventController = eventLoader.getController();
        eventController.setManagers(systemManager, viewManager);

        FXMLLoader userLoader = new FXMLLoader(getClass().getResource("/com/ui/pages/userPage.fxml"));
        Parent userViewNode = userLoader.load();
        UserController userController = userLoader.getController();
        userController.setManagers(systemManager, viewManager);

        viewManager.addScreen("MARKET_DASHBOARD", marketViewNode, eventController);
        viewManager.addScreen("USER_DASHBOARD", userViewNode, userController);

        BorderPane borderPane = (BorderPane) viewManager.getMainScene().getRoot();
        borderPane.setTop(topBarNode);

        viewManager.navigateTo("USER_DASHBOARD");

        // Read animation state from the text file instead of command line arguments
        boolean enableAnimations = false;
        File animFile = new File("animation_state.txt");
        if (animFile.exists()) {
            try (Scanner scanner = new Scanner(animFile)) {
                if (scanner.hasNextBoolean()) {
                    enableAnimations = scanner.nextBoolean();
                }
            } catch (Exception e) {
                System.err.println("Could not read animation state.");
            }
        }

        // Only swap the root structure to StackPane if animations are enabled
        if (enableAnimations) {
            StackPane rootContainer = new StackPane();
            viewManager.getMainScene().setRoot(rootContainer);
            borderPane.setVisible(false);

            javafx.scene.shape.Circle loadingRing = new javafx.scene.shape.Circle(40);
            loadingRing.setFill(javafx.scene.paint.Color.TRANSPARENT);
            loadingRing.setStroke(javafx.scene.paint.Color.web("#38BDF8"));
            loadingRing.setStrokeWidth(6);
            loadingRing.getStrokeDashArray().addAll(35d, 15d);

            rootContainer.getChildren().addAll(borderPane, loadingRing);

            RotateTransition rotate = new RotateTransition(Duration.seconds(1.2), loadingRing);
            rotate.setByAngle(360);

            rotate.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.3), loadingRing);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(ev -> {
                    rootContainer.getChildren().remove(loadingRing);
                    borderPane.setVisible(true);
                    playStartupAnimations(borderPane);
                });
                fadeOut.play();
            });

            rotate.play();
        }

        // Reveal the window at the very end to prevent a blank freeze
        primaryStage.setTitle("Guess Market Trading Platform");
        primaryStage.setScene(viewManager.getMainScene());
        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    private void playStartupAnimations(Parent rootNode) {
        Duration duration = Duration.seconds(1.0);

        FadeTransition fade = new FadeTransition(duration, rootNode);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition translate = new TranslateTransition(duration, rootNode);
        translate.setFromY(30);
        translate.setToY(0);

        ScaleTransition scale = new ScaleTransition(duration, rootNode);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition parallelTransition = new ParallelTransition(fade, translate, scale);
        parallelTransition.play();
    }

}