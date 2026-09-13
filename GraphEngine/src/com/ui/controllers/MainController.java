package com.ui.controllers;

import com.maneger.SystemManager;
import com.ui.core.ViewManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class MainController {

    @FXML private TextField filePathField;
    @FXML private ComboBox<String> skinCombo;
    @FXML private CheckBox animationToggle;

    private SystemManager systemManager;
    private ViewManager viewManager;

    public void setManagers(SystemManager systemManager, ViewManager viewManager) {
        this.systemManager = systemManager;
        this.viewManager = viewManager;
        this.viewManager.changeTheme("GuessMarketTradingTheme.css");
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

            // 1. Read the initial state from the text file on load
            if (animFile.exists()) {
                try (Scanner scanner = new Scanner(animFile)) {
                    if (scanner.hasNextBoolean()) {
                        animationToggle.setSelected(scanner.nextBoolean());
                    }
                } catch (Exception e) {
                    System.err.println("Could not read animation state.");
                }
            }

            // 2. Write to the text file instantly whenever the checkbox state changes
            animationToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                try (FileWriter writer = new FileWriter(animFile)) {
                    writer.write(newVal.toString()); // Writes "true" or "false"
                } catch (IOException e) {
                    System.err.println("Error writing animation state to file.");
                }
            });
        }
    }

    @FXML
    private void handleGlobalLoadFile() {
        File file = chooseXmlFile();
        if (file != null && systemManager != null) {
            processFileLoad(file);
        }
    }

    private File chooseXmlFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"));
        return fileChooser.showOpenDialog(filePathField.getScene().getWindow());
    }

    private void processFileLoad(File file) {
        Dialog<Void> progressDialog = buildProgressDialog();
        Task<Void> loadTask = createLoadTask(file);

        setupTaskCallbacks(loadTask, progressDialog, file);

        new Thread(loadTask).start();
        progressDialog.showAndWait();
    }

    private Dialog<Void> buildProgressDialog() {
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle("Loading XML");
        progressDialog.setHeaderText("Parsing Market Data...");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        VBox vbox = new VBox(15, new Label("Please wait while the engine processes the file."), progressIndicator);
        vbox.setAlignment(Pos.CENTER);
        progressDialog.getDialogPane().setContent(vbox);

        // JavaFX Dialogs require at least one ButtonType to function, but we hide it
        progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        progressDialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        return progressDialog;
    }

    private Task<Void> createLoadTask(File file) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(1500); // Simulated delay as required by Task 2
                systemManager.loadXmlData(file.getAbsolutePath());
                return null;
            }
        };
    }

    private void setupTaskCallbacks(Task<Void> loadTask, Dialog<Void> progressDialog, File file) {
        loadTask.setOnSucceeded(e -> {
            progressDialog.setResult(null); // Unblocks the dialog
            progressDialog.close();

            if (filePathField != null) {
                filePathField.setText(file.getAbsolutePath());
            }

            // Broadcast refresh to ALL registered controllers simultaneously
            viewManager.refreshAllScreens();
        });

        loadTask.setOnFailed(e -> {
            progressDialog.setResult(null); // Unblocks the dialog
            progressDialog.close();

            Throwable ex = loadTask.getException();
            if (filePathField != null) {
                filePathField.setText("Error loading file.");
            }

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File Load Error");
            alert.setHeaderText(null);
            alert.setContentText(ex != null ? ex.getMessage() : "Unknown error occurred.");
            alert.showAndWait();
        });
    }

    @FXML
    private void navigateToEvents() {
        if (viewManager != null) {
            viewManager.refreshAllScreens();
            viewManager.navigateTo("MARKET_DASHBOARD");
        }
    }

    @FXML
    private void navigateToUsers() {
        if (viewManager != null) {
            viewManager.refreshAllScreens();
            viewManager.navigateTo("USER_DASHBOARD");
        }
    }
}