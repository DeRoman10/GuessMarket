package com.ui.controllers;

import com.data.events.entities.Event;
import com.data.events.entities.Option;
import com.data.events.enums.CommissionType;
import com.data.users.entities.User;
import com.data.events.enums.EventStatus;
import com.data.events.enums.OrderDirection;
import com.ui.core.IRefreshable;
import com.ui.core.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import okhttp3.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserController implements IRefreshable {

    // --- Left Panel UI Elements ---
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userBalanceCol;
    @FXML private TableColumn<User, String> userMmCol; // Task 3 Requirement

    // --- VIEW 1: Main Dashboard UI Elements ---
    @FXML private SplitPane mainUserView;
    @FXML private Label accountBalanceLabel;
    @FXML private Button loadFundsBtn; // Task 3 Requirement
    @FXML private LineChart<Number, Number> balanceChart;
    @FXML private TilePane userEventsTileContainer;
    @FXML private Label eventInfoLabel;
    @FXML private Button closeEventBtn;
    @FXML private Button openTradeViewBtn;
    @FXML private Button tradeSelectedEventBtn;
    @FXML private Button createEventBtn;

    // --- VIEW 2: Trading View UI Elements ---
    @FXML private HBox tradingView;
    @FXML private VBox tradeEventsTileContainer;
    @FXML private VBox placeOrderPane;
    @FXML private VBox optionsContainer;
    @FXML private Label amountLabel;
    @FXML private TextField tradeAmountField;
    @FXML private Label priceLabel;
    @FXML private TextField tradePriceField;
    @FXML private Label directionLabel;
    @FXML private ComboBox<OrderDirection> tradeDirectionCombo;
    @FXML private Label totalPriceLabel;
    @FXML private Button submitTradeBtn;

    // --- VIEW 3: Create Event UI Elements ---
    @FXML private VBox createEventView;
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

    // --- State Variables & Networking ---
    private ViewManager viewManager;
    private User currentSelectedUser;
    private String currentSelectedEventName;
    private Event tradeTargetEvent;
    private XYChart.Series<Number, Number> balanceSeries;

    private Button currentlySelectedTradeEventBtn = null;
    private Button currentlySelectedOptionBtn = null;
    private Option selectedOption = null;
    private int selectedOptionIndex = -1;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String BASE_URL = "http://localhost:8080/GuessMarketServer_Web_exploded";

    public void setManagers(ViewManager viewManager) {
        this.viewManager = viewManager;
        populateUsersTable();
    }

    @Override
    public void refreshData() {
        populateUsersTable();
    }

    @FXML
    public void initialize() {
        if (userNameCol != null) {
            userNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        }
        if (userBalanceCol != null) {
            userBalanceCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f$", cell.getValue().getBalance())));
        }
        if (userMmCol != null) {
            userMmCol.setCellValueFactory(cell -> {
                boolean isMM = cell.getValue().getManagedEvents() != null && !cell.getValue().getManagedEvents().isEmpty();
                return new SimpleStringProperty(isMM ? "Yes" : "No");
            });
        }

        balanceSeries = new XYChart.Series<>();
        balanceSeries.setName("Balance History");
        if (balanceChart != null) balanceChart.getData().add(balanceSeries);

        if (usersTable != null) {
            usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) selectUser(newSelection);
            });
        }

        if (tradeDirectionCombo != null) {
            tradeDirectionCombo.setItems(FXCollections.observableArrayList(OrderDirection.values()));
            tradeDirectionCombo.setValue(OrderDirection.BUY);
        }

        if (commissionTypeCombo != null) {
            commissionTypeCombo.getItems().addAll(CommissionType.ON_PURCHASE, CommissionType.ON_CLOSE);
        }
        if (methodTypeCombo != null) {
            methodTypeCombo.getItems().addAll("LMSR", "Order Book");
            methodTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                boolean isLmsr = "LMSR".equals(newVal);
                boolean isOB = "Order Book".equals(newVal);
                if(lmsrFieldsBox != null) { lmsrFieldsBox.setVisible(isLmsr); lmsrFieldsBox.setManaged(isLmsr); }
                if(obFieldsBox != null) { obFieldsBox.setVisible(isOB); obFieldsBox.setManaged(isOB); }
            });
        }
    }

    // --- Networking: Fetch Users ---
    private void populateUsersTable() {
        Request request = new Request.Builder().url(BASE_URL + "/users").get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager != null && viewManager.handleUnauthorized(response.code())) return;
                if (!response.isSuccessful()) return;

                String responseData = response.body().string();
                Type userListType = new TypeToken<List<User>>(){}.getType();
                List<User> updatedUsers = gson.fromJson(responseData, userListType);

                Platform.runLater(() -> {
                    if (usersTable != null) {
                        User selected = usersTable.getSelectionModel().getSelectedItem();
                        usersTable.setItems(FXCollections.observableArrayList(updatedUsers));

                        if (selected != null) {
                            for (User u : updatedUsers) {
                                if (u.getName().equals(selected.getName())) {
                                    usersTable.getSelectionModel().select(u);
                                    break;
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    private void selectUser(User user) {
        this.currentSelectedUser = user;
        this.currentSelectedEventName = null;

        if (accountBalanceLabel != null) accountBalanceLabel.setText(String.format("User: %s | Balance: %.2f$", user.getName(), user.getBalance()));
        if (loadFundsBtn != null) loadFundsBtn.setDisable(false);

        drawBalanceGraph(user);
        populateUserEvents(user);

        if (eventInfoLabel != null) eventInfoLabel.setText("Select an event from the list above to view details.");
        if (openTradeViewBtn != null) openTradeViewBtn.setDisable(user.isBlocked());
        if (createEventBtn != null) { createEventBtn.setDisable(false); createEventBtn.setVisible(true); }
        if (tradeSelectedEventBtn != null) tradeSelectedEventBtn.setVisible(false);
        if (closeEventBtn != null) closeEventBtn.setVisible(false);
    }

    private void drawBalanceGraph(User user) {
        if (balanceSeries == null) return;
        balanceSeries.getData().clear();
        List<Double> history = user.getBalanceHistory();
        if (history == null || history.isEmpty()) {
            balanceSeries.getData().add(new XYChart.Data<>(0, user.getBalance()));
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            balanceSeries.getData().add(new XYChart.Data<>(i, history.get(i)));
        }
    }

    private void populateUserEvents(User user) {
        if (userEventsTileContainer == null) return;
        userEventsTileContainer.getChildren().clear();

        Set<String> involvedEventNames = new HashSet<>();
        if (user.getManagedEvents() != null) involvedEventNames.addAll(user.getManagedEvents());
        if (user.getPortfolios() != null) involvedEventNames.addAll(user.getPortfolios().keySet());

        for (String eventName : involvedEventNames) {
            boolean isMM = user.getManagedEvents() != null && user.getManagedEvents().contains(eventName);
            String role = isMM ? "[MM] " : "[Participant] ";
            Button eventBtn = new Button(role + eventName);
            eventBtn.getStyleClass().add("action-button");
            eventBtn.setMaxWidth(Double.MAX_VALUE);
            eventBtn.setOnAction(e -> selectEvent(eventName, isMM));

            userEventsTileContainer.getChildren().add(eventBtn);
        }

        if (involvedEventNames.isEmpty()) {
            Label emptyLabel = new Label("No active participations. Click 'Trade Market' below to start!");
            emptyLabel.getStyleClass().add("sub-text");
            userEventsTileContainer.getChildren().add(emptyLabel);
        }
    }

    private void selectEvent(String eventName, boolean isMM) {
        this.currentSelectedEventName = eventName;

        // Fetch detailed status from server
        Request request = new Request.Builder()
                .url(BASE_URL + "/events?name=" + eventName)
                .get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager != null && viewManager.handleUnauthorized(response.code())) return;
                String detailedStatus = response.isSuccessful() ? gson.fromJson(response.body().string(), String.class) : "Error fetching details.";

                Platform.runLater(() -> {
                    if (eventInfoLabel != null) eventInfoLabel.setText(detailedStatus);
                    boolean isBlocked = currentSelectedUser.isBlocked();

                    if (tradeSelectedEventBtn != null) {
                        tradeSelectedEventBtn.setVisible(true);
                        tradeSelectedEventBtn.setManaged(true);
                        tradeSelectedEventBtn.setDisable(isBlocked);
                    }
                    if (closeEventBtn != null) {
                        closeEventBtn.setVisible(isMM);
                        closeEventBtn.setManaged(isMM);
                    }
                });
            }
        });
    }

    // --- Actions: Deposit & Close Event ---
    @FXML
    private void handleDepositFunds() {
        if (currentSelectedUser == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Load Funds");
        dialog.setHeaderText("Deposit Money to Account");
        dialog.setContentText("Enter amount to deposit ($):");

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) throw new NumberFormatException();

                RequestBody body = RequestBody.create("", MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(BASE_URL + "/deposit?amount=" + amount)
                        .post(body).build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Network Error", "Could not connect to server."));
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (viewManager.handleUnauthorized(response.code())) return;
                        String resBody = response.body().string();
                        Platform.runLater(() -> {
                            if (response.isSuccessful()) {
                                showAlert(Alert.AlertType.INFORMATION, "Success", "Funds loaded successfully.");
                                refreshData();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Deposit Failed", resBody);
                            }
                        });
                    }
                });
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid positive number.");
            }
        });
    }

    @FXML
    private void handleCloseEvent() {
        if (currentSelectedEventName == null) return;

        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Close Event");
        dialog.setHeaderText("Close Event: " + currentSelectedEventName);
        dialog.setContentText("Enter winning option index (0 or 1):");

        dialog.showAndWait().ifPresent(indexStr -> {
            RequestBody body = RequestBody.create("", MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(BASE_URL + "/closeEvent?eventName=" + currentSelectedEventName + "&winningOptionIndex=" + indexStr)
                    .post(body).build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (viewManager.handleUnauthorized(response.code())) return;
                    String resBody = response.body().string();
                    Platform.runLater(() -> {
                        if (response.isSuccessful()) {
                            showAlert(Alert.AlertType.INFORMATION, "Event Closed", "The event has been successfully closed.");
                            refreshData();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Closure Failed", resBody);
                        }
                    });
                }
            });
        });
    }

    @FXML
    private void handleOpenTradeView() {
        if (currentSelectedUser == null) return;
        mainUserView.setVisible(false);
        mainUserView.setManaged(false);
        tradingView.setVisible(true);
        tradingView.setManaged(true);
        placeOrderPane.setVisible(false);
        placeOrderPane.setManaged(false);
        fetchActiveEventsForTrading();
    }

    @FXML
    private void handleTradeSelectedEvent() {
        if (currentSelectedEventName == null) return;
        mainUserView.setVisible(false);
        mainUserView.setManaged(false);
        tradingView.setVisible(true);
        tradingView.setManaged(true);
        fetchActiveEventsForTrading();
    }

    @FXML
    private void handleCloseTradeView() {
        tradingView.setVisible(false);
        tradingView.setManaged(false);
        mainUserView.setVisible(true);
        mainUserView.setManaged(true);

        if (tradeAmountField != null) tradeAmountField.clear();
        if (tradePriceField != null) tradePriceField.clear();
        selectedOption = null;
        selectedOptionIndex = -1;
    }

    private void fetchActiveEventsForTrading() {
        Request request = new Request.Builder().url(BASE_URL + "/events").get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager != null && viewManager.handleUnauthorized(response.code())) return;
                if (!response.isSuccessful()) return;

                Type eventListType = new TypeToken<List<Event>>(){}.getType();
                List<Event> activeEvents = gson.fromJson(response.body().string(), eventListType);

                Platform.runLater(() -> {
                    if (tradeEventsTileContainer != null) tradeEventsTileContainer.getChildren().clear();

                    for (Event event : activeEvents) {
                        Button eventTile = new Button(event.getName());
                        eventTile.getStyleClass().add("action-button");
                        eventTile.setMaxWidth(Double.MAX_VALUE);
                        eventTile.setOnAction(e -> populateOptionsList(event));

                        tradeEventsTileContainer.getChildren().add(eventTile);

                        // Auto-select if we came from "Trade This Event"
                        if (currentSelectedEventName != null && event.getName().equals(currentSelectedEventName)) {
                            populateOptionsList(event);
                        }
                    }
                });
            }
        });
    }

    private void populateOptionsList(Event event) {
        this.tradeTargetEvent = event;
        if (optionsContainer == null) return;

        if (placeOrderPane != null) {
            placeOrderPane.setVisible(true);
            placeOrderPane.setManaged(true);
        }

        boolean isLmsr = event.getMethod() != null && event.getMethod().getLmsr() != null;
        if (priceLabel != null) { priceLabel.setVisible(!isLmsr); priceLabel.setManaged(!isLmsr); }
        if (tradePriceField != null) { tradePriceField.setVisible(!isLmsr); tradePriceField.setManaged(!isLmsr); }
        if (directionLabel != null) { directionLabel.setVisible(!isLmsr); directionLabel.setManaged(!isLmsr); }
        if (tradeDirectionCombo != null) { tradeDirectionCombo.setVisible(!isLmsr); tradeDirectionCombo.setManaged(!isLmsr); }

        if (amountLabel != null) amountLabel.setVisible(false);
        if (tradeAmountField != null) tradeAmountField.setVisible(false);
        if (submitTradeBtn != null) submitTradeBtn.setVisible(false);

        optionsContainer.getChildren().clear();
        selectedOptionIndex = -1;

        List<Option> options = event.getOptions();
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int currentIndex = i;

            Button optBtn = new Button(option.getName());
            optBtn.getStyleClass().add("action-button");
            optBtn.setMaxWidth(Double.MAX_VALUE);
            optBtn.setPrefHeight(40);

            optBtn.setOnAction(e -> {
                if (currentlySelectedOptionBtn != null) {
                    currentlySelectedOptionBtn.getStyleClass().remove("action-button-selected");
                    currentlySelectedOptionBtn.getStyleClass().add("action-button");
                }
                optBtn.getStyleClass().remove("action-button");
                optBtn.getStyleClass().add("action-button-selected");
                currentlySelectedOptionBtn = optBtn;
                selectedOption = option;
                selectedOptionIndex = currentIndex;

                if (amountLabel != null) amountLabel.setVisible(true);
                if (tradeAmountField != null) tradeAmountField.setVisible(true);
                if (submitTradeBtn != null) submitTradeBtn.setVisible(true);
            });
            optionsContainer.getChildren().add(optBtn);
        }
    }

    @FXML
    private void handleSubmitTrade() {
        if (selectedOptionIndex == -1 || tradeAmountField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Form", "Please fill all required fields.");
            return;
        }

        try {
            int amount = Integer.parseInt(tradeAmountField.getText());
            boolean isLmsr = tradeTargetEvent.getMethod() != null && tradeTargetEvent.getMethod().getLmsr() != null;

            HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/trade").newBuilder()
                    .addQueryParameter("eventName", tradeTargetEvent.getName())
                    .addQueryParameter("optionIndex", String.valueOf(selectedOptionIndex))
                    .addQueryParameter("amount", String.valueOf(amount));

            if (!isLmsr) {
                if (tradePriceField.getText().isEmpty() || tradeDirectionCombo.getValue() == null) throw new NumberFormatException();
                urlBuilder.addQueryParameter("limitPrice", tradePriceField.getText());
                urlBuilder.addQueryParameter("direction", tradeDirectionCombo.getValue().name());
            }

            RequestBody body = RequestBody.create("", MediaType.parse("application/json"));
            Request request = new Request.Builder().url(urlBuilder.build()).post(body).build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (viewManager.handleUnauthorized(response.code())) return;
                    String resBody = response.body().string();
                    Platform.runLater(() -> {
                        if (response.isSuccessful()) {
                            handleCloseTradeView();
                            showAlert(Alert.AlertType.INFORMATION, "Trade Successful", "Your transaction was executed.");
                            refreshData();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Transaction Failed", resBody);
                        }
                    });
                }
            });
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers.");
        }
    }

    // --- Bonus 4: Create Event View ---
    @FXML
    private void handleOpenCreateEvent() {
        mainUserView.setVisible(false); mainUserView.setManaged(false);
        tradingView.setVisible(false); tradingView.setManaged(false);
        createEventView.setVisible(true); createEventView.setManaged(true);
    }

    @FXML
    private void handleCancelCreateEvent() {
        createEventView.setVisible(false); createEventView.setManaged(false);
        mainUserView.setVisible(true); mainUserView.setManaged(true);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type); alert.setTitle(title);
        alert.setHeaderText(null); alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleApplyEvent() {
        try {
            String name = eventNameField.getText();
            String desc = descriptionField.getText();
            String commission = commissionRateField.getText();
            String commType = commissionTypeCombo.getValue().name();
            String opt1 = option1Field.getText();
            String opt2 = option2Field.getText();
            String method = methodTypeCombo.getValue();

            HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/createEvent").newBuilder()
                    .addQueryParameter("name", name)
                    .addQueryParameter("description", desc)
                    .addQueryParameter("commissionRate", commission)
                    .addQueryParameter("commissionType", commType)
                    .addQueryParameter("option1", opt1)
                    .addQueryParameter("option2", opt2)
                    .addQueryParameter("method", method);

            if ("LMSR".equals(method)) {
                urlBuilder.addQueryParameter("b", liquidityParamField.getText());
            } else {
                urlBuilder.addQueryParameter("initial", initialInvestmentField.getText());
                urlBuilder.addQueryParameter("d", baseValueField.getText());
                urlBuilder.addQueryParameter("allowMint", String.valueOf(allowMintCheckBox.isSelected()));
            }

            Request request = new Request.Builder().url(urlBuilder.build())
                    .post(RequestBody.create("", MediaType.parse("application/json"))).build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (viewManager.handleUnauthorized(response.code())) return;
                    String resBody = response.body().string();
                    Platform.runLater(() -> {
                        if (response.isSuccessful()) {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Event created successfully!");
                            handleCancelCreateEvent();
                            refreshData();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Creation Failed", resBody);
                        }
                    });
                }
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please check your input fields.");
        }
    }
}