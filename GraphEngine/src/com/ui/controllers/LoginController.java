package com.ui.controllers;

import com.ui.core.IRefreshable;
import com.ui.core.ViewManager;
import com.ui.core.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import okhttp3.*;

import java.io.IOException;

public class LoginController implements IRefreshable {

    @FXML private TextField usernameField;

    private ViewManager viewManager;

    public void setManagers(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @Override
    public void refreshData() { /* nothing to pull on the login screen */ }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Error", "Please enter a username.");
            return;
        }

        Request request = ApiClient.post(ApiClient.url("/login").addQueryParameter("username", username).build());

        ApiClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Connection Error",
                        "Could not connect to the server. Is Tomcat running?"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = ApiClient.message(response.body().string());
                int code = response.code();

                Platform.runLater(() -> {
                    if (code == 200) {
                        usernameField.clear();
                        viewManager.onLoggedIn(username);
                    } else if (code == 409) {
                        showAlert(Alert.AlertType.WARNING, "Login Failed", body);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Server Error", body);
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
