package com.ui.controllers;

import com.ui.core.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import okhttp3.*;
import com.ui.core.ApiClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class MainController {

    @FXML private TextField filePathField;
    @FXML private ComboBox<String> skinCombo;
    @FXML private CheckBox animationToggle;

    private ViewManager viewManager;

    public void setManagers(ViewManager viewManager) {
        this.viewManager = viewManager;
        this.viewManager.changeTheme("GuessMarketTradingTheme.css");
        // polling is owned by ViewManager and starts only after a successful login
    }

    @FXML
    public void initialize() {
        if (skinCombo != null) {
            skinCombo.getItems().addAll("Dark Mode", "Light Mode", "Neon Blue");
            skinCombo.setValue("Dark Mode"); // Default

            skinCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (viewManager != null && newVal != null) {
                    switch (newVal) {
                        case "Dark Mode":
                            viewManager.changeTheme("GuessMarketTradingTheme.css");
                            break;
                        case "Light Mode":
                            viewManager.changeTheme("GuessMarketLightTheme.css");
                            break;
                        case "Neon Blue":
                            viewManager.changeTheme("GuessMarketNeonTheme.css");
                            break;
                    }
                }
            });
        }
        if (animationToggle != null) {
            File animFile = new File("animation_state.txt");

            if (animFile.exists()) {
                try (Scanner scanner = new Scanner(animFile)) {
                    if (scanner.hasNextBoolean()) {
                        animationToggle.setSelected(scanner.nextBoolean());
                    }
                } catch (Exception e) {
                    System.err.println("Could not read animation state.");
                }
            }

            animationToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                try (FileWriter writer = new FileWriter(animFile)) {
                    writer.write(newVal.toString());
                } catch (IOException e) {
                    System.err.println("Error writing animation state to file.");
                }
            });
        }
    }

    @FXML
    private void handleGlobalLoadFile() {
        if (viewManager == null || !viewManager.isLoggedIn()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please log in before uploading a file.");
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }
        File file = chooseXmlFile();
        if (file != null) {
            uploadFileToServer(file);
        }
    }

    private File chooseXmlFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"));
        return fileChooser.showOpenDialog(filePathField.getScene().getWindow());
    }

    private void uploadFileToServer(File file) {
        Dialog<Void> progressDialog = buildProgressDialog();
        progressDialog.show();

        RequestBody fileBody = RequestBody.create(MediaType.parse("application/xml"), file);
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(ApiClient.url("/upload").build())
                .post(requestBody)
                .build();

        ApiClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> {
                    progressDialog.setResult(null);
                    progressDialog.close();
                    if (filePathField != null) filePathField.setText("Error uploading file.");

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Upload Error");
                    alert.setHeaderText("Failed to connect to the server");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager.handleUnauthorized(response)) {
                    return;
                }

                String responseData = ApiClient.message(response.body().string());
                Platform.runLater(() -> {
                    progressDialog.setResult(null);
                    progressDialog.close();

                    if (response.isSuccessful()) {
                        if (filePathField != null) filePathField.setText(file.getAbsolutePath());
                        viewManager.refreshAllScreens();
                        // the task requires an explicit "file is valid" message for the uploader
                        Alert ok = new Alert(Alert.AlertType.INFORMATION);
                        ok.setTitle("Upload Successful");
                        ok.setHeaderText(null);
                        ok.setContentText(responseData);
                        ok.showAndWait();
                    } else {
                        if (filePathField != null) filePathField.setText("Error loading file.");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Server Error");
                        alert.setHeaderText("File validation/upload failed");
                        TextArea area = new TextArea(responseData);
                        area.setEditable(false);
                        area.setWrapText(true);
                        area.setPrefRowCount(10);
                        alert.getDialogPane().setContent(area);
                        alert.setResizable(true);
                        alert.showAndWait();
                    }
                });
            }
        });
    }

    private Dialog<Void> buildProgressDialog() {
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle("Uploading XML");
        progressDialog.setHeaderText("Sending Market Data to Server...");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        VBox vbox = new VBox(15, new Label("Please wait while the server processes the file."), progressIndicator);
        vbox.setAlignment(Pos.CENTER);
        progressDialog.getDialogPane().setContent(vbox);

        progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        progressDialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        return progressDialog;
    }

    @FXML
    private void navigateToEvents() {
        if (viewManager != null && viewManager.isLoggedIn()) {
            viewManager.refreshAllScreens();
            viewManager.navigateTo("MARKET_DASHBOARD");
        }
    }

    @FXML
    private void navigateToUsers() {
        if (viewManager != null && viewManager.isLoggedIn()) {
            viewManager.refreshAllScreens();
            viewManager.navigateTo("USER_DASHBOARD");
        }
    }

    @FXML
    private void navigateToLogin() {
        if (viewManager == null || !viewManager.isLoggedIn()) return;

        Request request = ApiClient.post(ApiClient.url("/logout").build());
        ApiClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                viewManager.performLogout();
            }

            @Override
            public void onResponse(Call call, Response response) {
                response.close();
                viewManager.performLogout();
            }
        });
    }

    @FXML
    private void navigateToChat() {
        if (viewManager != null && viewManager.isLoggedIn()) {
            viewManager.refreshAllScreens();
            viewManager.navigateTo("CHAT_DASHBOARD");
        }
    }
}
