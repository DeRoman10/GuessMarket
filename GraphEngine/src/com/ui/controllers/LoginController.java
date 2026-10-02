package com.ui.controllers;

import com.ui.core.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import okhttp3.*;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;

    private ViewManager viewManager;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final String BASE_URL = "http://localhost:8080/GuessMarketServer_Web_exploded";

    public void setManagers(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Error", "Please enter a username.");
            return;
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), "");

        Request request = new Request.Builder()
                .url(BASE_URL + "/login?username=" + username)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the server. Is Tomcat running?"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();

                Platform.runLater(() -> {
                    if (response.isSuccessful()) {
                        if (viewManager != null) {
                            viewManager.navigateTo("USER_DASHBOARD");
                            viewManager.refreshAllScreens(); // Triggers the first data pull
                        }
                    } else if (response.code() == 409) {
                        showAlert(Alert.AlertType.WARNING, "Login Failed", "Username '" + username + "' is already taken. Please choose another.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Server Error", responseData);
                    }
                });
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}