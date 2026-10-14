package com.ui.core;

import com.ui.controllers.*;
import javafx.animation.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainApp extends Application {

    private ViewManager viewManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        viewManager = new ViewManager();

        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/ui/pages/mainLayout.fxml"));
        Parent topBarNode = mainLoader.load();
        MainController mainController = mainLoader.getController();
        mainController.setManagers(viewManager);

        FXMLLoader eventLoader = new FXMLLoader(getClass().getResource("/com/ui/pages/eventPage.fxml"));
        Parent marketViewNode = eventLoader.load();
        EventController eventController = eventLoader.getController();
        eventController.setManagers(viewManager);          // was never called: 401s on this screen were silently ignored

        FXMLLoader userLoader = new FXMLLoader(getClass().getResource("/com/ui/pages/userPage.fxml"));
        Parent userViewNode = userLoader.load();
        UserController userController = userLoader.getController();
        userController.setManagers(viewManager);           // no longer fetches anything (we are not logged in yet)

        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/com/ui/pages/loginPage.fxml"));
        Parent loginViewNode = loginLoader.load();
        LoginController loginController = loginLoader.getController();
        loginController.setManagers(viewManager);

        FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/com/ui/pages/chatPage.fxml"));
        Parent chatViewNode = chatLoader.load();
        ChatController chatController = chatLoader.getController();
        chatController.setManagers(viewManager);

        viewManager.addScreen("MARKET_DASHBOARD", marketViewNode, eventController);
        viewManager.addScreen("USER_DASHBOARD", userViewNode, userController);
        viewManager.addScreen("LOGIN_DASHBOARD", loginViewNode, loginController);
        viewManager.addScreen("CHAT_DASHBOARD", chatViewNode, chatController);

        BorderPane borderPane = (BorderPane) viewManager.getMainScene().getRoot();
        borderPane.setTop(topBarNode);

        viewManager.navigateTo("LOGIN_DASHBOARD");

        StackPane rootContainer = new StackPane();
        viewManager.getMainScene().setRoot(rootContainer);

        primaryStage.setTitle("Guess Market Trading Platform");
        primaryStage.setScene(viewManager.getMainScene());
        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(700);
        primaryStage.show();

        // startup animation is always on
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

    @Override
    public void stop() {
        if (viewManager != null) viewManager.stopPolling();
        ApiClient.shutdown();
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

        new ParallelTransition(fade, translate, scale).play();
    }

}