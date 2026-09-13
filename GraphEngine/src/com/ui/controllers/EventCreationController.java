package com.ui.controllers;

import com.data.events.enums.CommissionType;
import com.data.users.entities.User;
import com.maneger.SystemManager;
import com.ui.core.IGuiController;
import com.ui.core.ViewManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EventCreationController implements IGuiController {

    @FXML private TextField eventNameField;
    @FXML private TextField descriptionField;
    @FXML private TextField commissionRateField;
    @FXML private ComboBox<CommissionType> commissionTypeCombo;
    @FXML private TextField option1Field;
    @FXML private TextField option2Field;
    @FXML private ComboBox<String> methodTypeCombo;

    @FXML private VBox lmsrFieldsBox;
    @FXML private TextField liquidityParamField;

    @FXML private VBox obFieldsBox;
    @FXML private TextField initialInvestmentField;
    @FXML private TextField baseValueField;
    @FXML private CheckBox allowMintCheckBox;

    private SystemManager systemManager;
    private ViewManager viewManager;
    private User creator;

    @Override
    public void setManagers(SystemManager systemManager, ViewManager viewManager) {
        this.systemManager = systemManager;
        this.viewManager = viewManager;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    @Override
    public void refreshData() {}

    @FXML
    public void initialize() {
        commissionTypeCombo.getItems().addAll(CommissionType.ON_PURCHASE, CommissionType.ON_CLOSE);
        methodTypeCombo.getItems().addAll("LMSR", "Order Book");

        // Toggle visibility based on selected method
        methodTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isLmsr = "LMSR".equals(newVal);
            boolean isOB = "Order Book".equals(newVal);

            lmsrFieldsBox.setVisible(isLmsr);
            lmsrFieldsBox.setManaged(isLmsr);

            obFieldsBox.setVisible(isOB);
            obFieldsBox.setManaged(isOB);
        });
    }

    @FXML
    private void handleApplyEvent(ActionEvent event) {
        if (creator == null) return;

        try {
            String name = eventNameField.getText();
            String desc = descriptionField.getText();
            int commission = Integer.parseInt(commissionRateField.getText());
            CommissionType commType = commissionTypeCombo.getValue();
            String opt1 = option1Field.getText();
            String opt2 = option2Field.getText();
            String method = methodTypeCombo.getValue();

            if (method == null || commType == null) {
                throw new Exception("Please select a Commission Type and Market Method.");
            }

            if ("LMSR".equals(method)) {
                int b = Integer.parseInt(liquidityParamField.getText());
                systemManager.createLmsrEvent(creator, name, desc, commission, commType, opt1, opt2, b);
            } else {
                int initial = Integer.parseInt(initialInvestmentField.getText());
                int d = Integer.parseInt(baseValueField.getText());
                boolean allowMint = allowMintCheckBox.isSelected();
                systemManager.createOrderBookEvent(creator, name, desc, commission, commType, opt1, opt2, initial, allowMint, d);
            }

            handleGoBack(event);

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please ensure all numeric fields contain valid numbers.");
        } catch (Exception e) {
            showAlert("Creation Error", e.getMessage());
        }
    }

    @FXML
    private void handleGoBack(ActionEvent event) {
        Node source = (Node) event.getSource();
        if (source.getScene() != null && source.getScene().getWindow() instanceof Stage) {
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
        }

        if (viewManager != null) {
            UserController uc = (UserController) viewManager.getController("USER_DASHBOARD");
            if (uc != null) uc.refreshData();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}