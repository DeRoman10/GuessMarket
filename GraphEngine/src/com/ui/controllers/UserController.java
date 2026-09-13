package com.ui.controllers;

import com.data.events.entities.Event;
import com.data.events.entities.Option;
import com.data.events.enums.CommissionType;
import com.data.users.entities.User;
import com.data.users.enums.EventToUserStatus;
import com.data.events.enums.EventStatus;
import com.data.events.enums.OrderDirection;
import com.maneger.FinalReceipt;
import com.maneger.PurchaseReceipt;
import com.maneger.SystemManager;
import com.maneger.TradeMonitor;
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

import java.util.ArrayList;
import java.util.List;

public class UserController implements IRefreshable {

    // --- Left Panel UI Elements ---
    @FXML private VBox leftPanel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userBalanceCol;

    // --- VIEW 1: Main Dashboard UI Elements ---
    @FXML private SplitPane mainUserView;
    @FXML private Label accountBalanceLabel;
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

    // --- State Variables ---
    private Button currentlySelectedTradeEventBtn = null;
    private SystemManager systemManager;
    private ViewManager viewManager;
    private User currentSelectedUser;
    private Event currentSelectedEvent;
    private XYChart.Series<Number, Number> balanceSeries;


    private Button currentlySelectedOptionBtn = null;
    private Option selectedOption = null;
    private int selectedOptionIndex = -1;

    public void setManagers(SystemManager systemManager, ViewManager viewManager) {
        this.systemManager = systemManager;
        this.viewManager = viewManager;
        populateUsersTable();
    }

    public void refreshData() {
        populateUsersTable();

        if (currentSelectedUser != null) {
            for (User u : usersTable.getItems()) {
                if (u.getName().equals(currentSelectedUser.getName())) {
                    usersTable.getSelectionModel().select(u);
                    selectUser(u);
                    break;
                }
            }
        }
    }

    @FXML
    public void initialize() {
        if (userNameCol != null) {
            userNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        }
        if (userBalanceCol != null) {
            userBalanceCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f$", cell.getValue().getBalance())));
        }

        balanceSeries = new XYChart.Series<>();
        balanceSeries.setName("Balance History");
        if (balanceChart != null) balanceChart.getData().add(balanceSeries);

        if (usersTable != null) {
            usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) selectUser(newSelection);
            });
        }

        if (tradeAmountField != null) tradeAmountField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        if (tradePriceField != null) tradePriceField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());

        if (tradeDirectionCombo != null) {
            tradeDirectionCombo.setItems(FXCollections.observableArrayList(OrderDirection.values()));
            tradeDirectionCombo.setValue(OrderDirection.BUY);
            tradeDirectionCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        }

        if (commissionTypeCombo != null) {
            commissionTypeCombo.getItems().addAll(CommissionType.ON_PURCHASE, CommissionType.ON_CLOSE);
        }
        if (methodTypeCombo != null) {
            methodTypeCombo.getItems().addAll("LMSR", "Order Book");
            methodTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                boolean isLmsr = "LMSR".equals(newVal);
                boolean isOB = "Order Book".equals(newVal);
                lmsrFieldsBox.setVisible(isLmsr);
                lmsrFieldsBox.setManaged(isLmsr);
                obFieldsBox.setVisible(isOB);
                obFieldsBox.setManaged(isOB);
            });
        }
    }

    private void populateUsersTable() {
        if (systemManager == null || usersTable == null) return;
        usersTable.setItems(FXCollections.observableArrayList(systemManager.getUsers()));
        usersTable.refresh();
    }

    private void selectUser(User user) {
        this.currentSelectedUser = user;
        this.currentSelectedEvent = null;

        if (accountBalanceLabel != null) {
            accountBalanceLabel.setText(String.format("User: %s | Balance: %.2f$", user.getName(), user.getBalance()));
        }

        drawBalanceGraph(user);
        populateUserEvents(user);

        if (eventInfoLabel != null) eventInfoLabel.setText("Select an event from the list above to view details.");

        if (openTradeViewBtn != null) openTradeViewBtn.setDisable(user.isBlocked());
        if (createEventBtn != null) {
            createEventBtn.setDisable(false);
            createEventBtn.setVisible(true);
        }
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

    private EventToUserStatus getEventToUserStatus(Event event, User user) {
        boolean isMM = event.getMmAccount() != null && event.getMmAccount().getName() != null && event.getMmAccount().getName().equals(user.getName());
        boolean isParticipant = user.getPortfolios() != null && user.getPortfolios().containsKey(event.getId());

        if (isMM) return EventToUserStatus.MM;
        if (isParticipant) return EventToUserStatus.PARTICIPANT;
        return EventToUserStatus.NONE;
    }

    private void populateUserEvents(User user) {
        if (userEventsTileContainer == null) return;
        userEventsTileContainer.getChildren().clear();
        currentlySelectedTradeEventBtn = null;

        boolean hasEvents = false;
        for (Event ev : systemManager.getEvents()) {
            EventToUserStatus status = getEventToUserStatus(ev, user);

            if (status != EventToUserStatus.NONE) {
                hasEvents = true;
                String role = (status == EventToUserStatus.MM) ? "[MM] " : "[Participant] ";
                Button eventBtn = new Button(role + ev.getName());
                eventBtn.getStyleClass().add("action-button");
                eventBtn.setMaxWidth(Double.MAX_VALUE);

                // Highlight if it's already the selected event
                if (currentSelectedEvent != null && currentSelectedEvent.getId().equals(ev.getId())) {
                    eventBtn.getStyleClass().remove("action-button");
                    eventBtn.getStyleClass().add("action-button-selected");
                    currentlySelectedTradeEventBtn = eventBtn;
                }

                eventBtn.setOnAction(e -> {
                    // Swap CSS classes to highlight the active button
                    if (currentlySelectedTradeEventBtn != null) {
                        currentlySelectedTradeEventBtn.getStyleClass().remove("action-button-selected");
                        currentlySelectedTradeEventBtn.getStyleClass().add("action-button");
                    }
                    eventBtn.getStyleClass().remove("action-button");
                    eventBtn.getStyleClass().add("action-button-selected");
                    currentlySelectedTradeEventBtn = eventBtn;

                    selectEvent(ev);
                });

                userEventsTileContainer.getChildren().add(eventBtn);
            }
        }

        if (!hasEvents) {
            Label emptyLabel = new Label("No active participations. Click 'Trade Market' below to start!");
            emptyLabel.getStyleClass().add("sub-text");
            emptyLabel.setWrapText(true);
            userEventsTileContainer.getChildren().add(emptyLabel);
        }
    }

    private void selectEvent(Event event) {
        this.currentSelectedEvent = event;

        if (eventInfoLabel != null) {
            try {
                String eventDetails = buildUserEventOverview(event, currentSelectedUser);
                eventInfoLabel.setText(eventDetails);
            } catch (Exception e) {
                eventInfoLabel.setText("Error loading event details: " + e.getMessage());
            }
        }

        boolean isMM = currentSelectedEvent.getMmAccount() != null && currentSelectedEvent.getMmAccount().getName() != null && currentSelectedEvent.getMmAccount().getName().equals(currentSelectedUser.getName());
        boolean isActive = event.getStatus() == EventStatus.ACTIVE;
        boolean isBlocked = currentSelectedUser.isBlocked();

        if (openTradeViewBtn != null) openTradeViewBtn.setDisable(!isActive || isBlocked);

        if (tradeSelectedEventBtn != null) {
            tradeSelectedEventBtn.setVisible(isActive);
            tradeSelectedEventBtn.setManaged(isActive);
            tradeSelectedEventBtn.setDisable(!isActive || isBlocked);
        }

        if (closeEventBtn != null) {
            closeEventBtn.setVisible(isMM && isActive);
            closeEventBtn.setManaged(isMM && isActive);
        }
    }

    private String buildUserEventOverview(Event event, User user) {
        boolean isLmsr = event.getMethod() != null && event.getMethod().getLmsr() != null;
        EventToUserStatus status = getEventToUserStatus(event, user);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("--- Event: %s (%s) ---\n", event.getName(), event.getStatus()));

        if (status == EventToUserStatus.MM) {
            try {
                sb.append("\n[Global Market Status]\n");
                sb.append(systemManager.getEventCurrentState(event.getId()));
                sb.append(String.format("Total Commission Collected: %.2f$\n", event.getTotalCommissionCollected()));
                sb.append(String.format("Event Account Balance: %.2f$\n", event.getContractBalance())); // NEW
            } catch (Exception e) {
                sb.append("Error loading global stats.\n");
            }
        }

        com.data.users.entities.Portfolio portfolio = user.getPortfolios().get(event.getId());
        sb.append("\n[Your Personal Portfolio]\n");

        if (portfolio != null) {
            sb.append("Your Holdings:\n");
            for (String optionName : portfolio.getHoldings().keySet()) {
                sb.append(String.format("\t- %s: %.2f Shares\n", optionName, portfolio.getHoldings().get(optionName)));
            }
            sb.append(String.format("\nTotal Invested: %.2f$\n", portfolio.getTotalInvestment()));

            sb.append("\nYour Personal Trading History (Latest first):\n");
            List<com.data.events.entities.Ticket> history = portfolio.getHistory();
            if (history == null || history.isEmpty()) {
                sb.append("\tNo personal transactions yet.\n");
            } else {
                for (int i = history.size() - 1; i >= 0; i--) {
                    com.data.events.entities.Ticket t = history.get(i);
                    sb.append(String.format("\tOption: %s | Amount: %d | Price Paid: %.2f$\n",
                            t.getOption().getName(), t.getAmount(), t.getPricePaid()));
                }
            }
        } else {
            sb.append("You have not purchased any shares in this event yet.\n");
        }

        if (!isLmsr) {
            try {
                List<com.data.events.entities.OBOrder> allBids = systemManager.getEventBids(event.getId());
                List<com.data.events.entities.OBOrder> allAsks = systemManager.getEventAsks(event.getId());

                boolean hasOpenOrders = false;
                StringBuilder ordersSb = new StringBuilder();
                ordersSb.append("\n[Your Open Orders (Order Book)]\n");

                for (com.data.events.entities.OBOrder bid : allBids) {
                    if (bid.getUser().getName().equals(user.getName())) {
                        hasOpenOrders = true;
                        ordersSb.append(String.format("\t- [BID] %d shares of %s @ $%.2f\n", bid.getAmount(), bid.getOption().getName(), bid.getPrice()));
                    }
                }
                for (com.data.events.entities.OBOrder ask : allAsks) {
                    if (ask.getUser().getName().equals(user.getName())) {
                        hasOpenOrders = true;
                        ordersSb.append(String.format("\t- [ASK] %d shares of %s @ $%.2f\n", ask.getAmount(), ask.getOption().getName(), ask.getPrice()));
                    }
                }
                if (hasOpenOrders) sb.append(ordersSb.toString());
                else sb.append("\n[Your Open Orders (Order Book)]\n\tNo active open orders.\n");
            } catch (Exception e) {
                sb.append("\nError loading open orders.\n");
            }
        }
        return sb.toString();
    }

    // --- Event Creation Logic (VIEW 3) ---

    @FXML
    private void handleOpenCreateEvent() {
        if (currentSelectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No User Selected", "Please select a user before creating an event.");
            return;
        }

        mainUserView.setVisible(false);
        mainUserView.setManaged(false);
        tradingView.setVisible(false);
        tradingView.setManaged(false);

        leftPanel.setVisible(false);
        leftPanel.setManaged(false);

        createEventView.setVisible(true);
        createEventView.setManaged(true);
    }

    @FXML
    private void handleCancelCreateEvent() {
        createEventView.setVisible(false);
        createEventView.setManaged(false);

        mainUserView.setVisible(true);
        mainUserView.setManaged(true);

        leftPanel.setVisible(true);
        leftPanel.setManaged(true);

        eventNameField.clear();
        descriptionField.clear();
        commissionRateField.clear();
        option1Field.clear();
        option2Field.clear();
        if(liquidityParamField != null) liquidityParamField.clear();
        if(initialInvestmentField != null) initialInvestmentField.clear();
        if(baseValueField != null) baseValueField.clear();
        commissionTypeCombo.setValue(null);
        methodTypeCombo.setValue(null);
        if(allowMintCheckBox != null) allowMintCheckBox.setSelected(false);
    }

    @FXML
    private void handleApplyEvent() {
        if (currentSelectedUser == null) return;

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
                systemManager.createLmsrEvent(currentSelectedUser, name, desc, commission, commType, opt1, opt2, b);
            } else {
                int initial = Integer.parseInt(initialInvestmentField.getText());
                int d = Integer.parseInt(baseValueField.getText());
                boolean allowMint = allowMintCheckBox.isSelected();
                systemManager.createOrderBookEvent(currentSelectedUser, name, desc, commission, commType, opt1, opt2, initial, allowMint, d);
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "Event created successfully!");
            handleCancelCreateEvent(); // Go back to Dashboard View
            refreshData();
            if (viewManager != null) {
                viewManager.refreshAllScreens(); // Tell Event Tab to update too
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please ensure all numeric fields contain valid numbers.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Creation Error", e.getMessage());
        }
    }

    // --- Trading Logic (VIEW 2) ---

    @FXML
    private void handleTradeSelectedEvent() {
        if (currentSelectedEvent == null)
            return;
        mainUserView.setVisible(false);
        mainUserView.setManaged(false);
        createEventView.setVisible(false);
        createEventView.setManaged(false);
        tradingView.setVisible(true);
        tradingView.setManaged(true);
        if (leftPanel != null) {
            leftPanel.setVisible(false);
            leftPanel.setManaged(false);
        }
        populateTradeEvents();
        populateOptionsList(currentSelectedEvent);
    }

    @FXML
    private void handleOpenTradeView() {
        if (currentSelectedUser == null)
            return;
        currentSelectedEvent = null;
        mainUserView.setVisible(false);
        mainUserView.setManaged(false);
        createEventView.setVisible(false);
        createEventView.setManaged(false);
        tradingView.setVisible(true);
        tradingView.setManaged(true);
        placeOrderPane.setVisible(false);
        placeOrderPane.setManaged(false);
        if (leftPanel != null) {
            leftPanel.setVisible(false);
            leftPanel.setManaged(false);
        }
        populateTradeEvents();
    }

    @FXML
    private void handleCloseTradeView() {
        tradingView.setVisible(false);
        tradingView.setManaged(false);
        mainUserView.setVisible(true);
        mainUserView.setManaged(true);

        if (leftPanel != null) {
            leftPanel.setVisible(true);
            leftPanel.setManaged(true);
        }

        if (tradeAmountField != null) tradeAmountField.clear();
        if (tradePriceField != null) tradePriceField.clear();
        selectedOption = null;
        selectedOptionIndex = -1;
        currentlySelectedTradeEventBtn = null;
    }

    private void populateTradeEvents() {
        if (tradeEventsTileContainer == null) return;
        tradeEventsTileContainer.getChildren().clear();
        currentlySelectedTradeEventBtn = null;

        for (Event event : systemManager.getActiveEvents()) {
            Button eventTile = new Button(event.getName());
            eventTile.getStyleClass().add("action-button");
            eventTile.setMaxWidth(Double.MAX_VALUE);

            if (currentSelectedEvent != null && currentSelectedEvent.getId().equals(event.getId())) {
                eventTile.getStyleClass().remove("action-button");
                eventTile.getStyleClass().add("action-button-selected");
                currentlySelectedTradeEventBtn = eventTile;
            }

            eventTile.setOnAction(e -> {
                if (currentlySelectedTradeEventBtn != null) {
                    currentlySelectedTradeEventBtn.getStyleClass().remove("action-button-selected");
                    currentlySelectedTradeEventBtn.getStyleClass().add("action-button");
                }
                eventTile.getStyleClass().remove("action-button");
                eventTile.getStyleClass().add("action-button-selected");
                currentlySelectedTradeEventBtn = eventTile;
                populateOptionsList(event);
            });

            tradeEventsTileContainer.getChildren().add(eventTile);
        }
    }

    private void populateOptionsList(Event event) {
        this.currentSelectedEvent = event;
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
        if (totalPriceLabel != null) totalPriceLabel.setVisible(false);

        optionsContainer.getChildren().clear();
        currentlySelectedOptionBtn = null;
        selectedOption = null;
        selectedOptionIndex = -1;

        if (tradeAmountField != null) tradeAmountField.clear();

        List<Option> options = event.getOptions();

        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int currentIndex = i;

            double price = 0.0;
            try {
                price = systemManager.getOptionCurrentPrice(event.getId(), currentIndex);
            } catch (Exception ex) {
                System.err.println("Could not load price: " + ex.getMessage());
            }

            Button optBtn = new Button(String.format("%s ($%.2f)", option.getName(), price));
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
                if (totalPriceLabel != null) totalPriceLabel.setVisible(true);

                updateTotalPreview();
            });

            optionsContainer.getChildren().add(optBtn);
        }
    }

    @FXML
    private void handleCloseEvent() {
        if (currentSelectedEvent != null) {
            List<String> optionNames = new ArrayList<>();
            for (Option opt : currentSelectedEvent.getOptions()) optionNames.add(opt.getName());

            ChoiceDialog<String> dialog = new ChoiceDialog<>(optionNames.get(0), optionNames);
            dialog.setTitle("Close Event");
            dialog.setHeaderText("Select the winning option for: " + currentSelectedEvent.getName());
            dialog.setContentText("Winning Option:");

            dialog.showAndWait().ifPresent(selectedName -> {
                int winningIndex = optionNames.indexOf(selectedName);
                new Thread(() -> {
                    try {
                        TradeMonitor<FinalReceipt> monitor = systemManager.closeEvent(currentSelectedEvent.getId(), winningIndex);
                        FinalReceipt receipt = monitor.waitForResult();

                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Event Closed", receipt.getReceiptAsString(currentSelectedEvent.getCommissionRate()));
                            refreshData();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Closure Failed", e.getMessage()));
                    }
                }).start();
            });
        }
    }

    @FXML
    private void handleSubmitTrade() {
        if (selectedOption == null || selectedOptionIndex == -1 || tradeAmountField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Form", "Please select an option and enter an amount.");
            return;
        }

        try {
            int amount = Integer.parseInt(tradeAmountField.getText());
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Invalid Amount", "Amount must be strictly greater than 0.");
                return;
            }

            double price = 0.0;
            OrderDirection direction = OrderDirection.BUY;
            boolean isLmsr = currentSelectedEvent.getMethod() != null && currentSelectedEvent.getMethod().getLmsr() != null;

            if (!isLmsr) {
                if (tradePriceField == null || tradePriceField.getText().isEmpty() || tradeDirectionCombo.getValue() == null) {
                    showAlert(Alert.AlertType.WARNING, "Incomplete Form", "Please enter a limit price and select order direction.");
                    return;
                }

                price = Double.parseDouble(tradePriceField.getText());
                int baseValueD = currentSelectedEvent.getMethod().getOrderBook().getD();
                double maxPrice = baseValueD - 0.01;

                if (price <= 0.0 || price > maxPrice) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Price", String.format("Price must be strictly greater than 0 and cannot exceed %.2f$", maxPrice));
                    return;
                }
                direction = tradeDirectionCombo.getValue();
            }

            final double finalPrice = price;
            final OrderDirection finalDirection = direction;
            final Event tradedEvent = currentSelectedEvent;

            new Thread(() -> {
                try {
                    TradeMonitor<PurchaseReceipt> monitor;
                    if (isLmsr) {
                        monitor = systemManager.executePurchase(tradedEvent.getId(), selectedOptionIndex, amount, currentSelectedUser);
                    } else {
                        monitor = systemManager.executeOBTrade(tradedEvent.getId(), selectedOptionIndex, amount, finalPrice, finalDirection, currentSelectedUser);
                    }

                    PurchaseReceipt receipt = monitor.waitForResult();

                    Platform.runLater(() -> {
                        handleCloseTradeView();
                        refreshData();
                        showAlert(Alert.AlertType.INFORMATION, "Trade Successful", receipt.getReceiptAsString(tradedEvent.getCommissionRate()));
                        if (currentSelectedUser != null && currentSelectedUser.isBlocked()) {
                            showAlert(Alert.AlertType.WARNING, "Account Locked", "Your balance has dropped below 0. Your account is restricted from executing further trades.");
                        }
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Transaction Failed", ex.getMessage()));
                }
            }).start();

        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numeric values for amount (and price if OB).");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void updateTotalPreview() {
        if (totalPriceLabel == null) return;
        if (selectedOptionIndex == -1 || currentSelectedEvent == null || tradeAmountField.getText().isEmpty()) {
            totalPriceLabel.setText("Total Cost/Revenue: 0.00$");
            return;
        }

        try {
            int amount = Integer.parseInt(tradeAmountField.getText());
            if (amount > 0) {
                boolean isLmsr = currentSelectedEvent.getMethod() != null && currentSelectedEvent.getMethod().getLmsr() != null;
                if (isLmsr) {
                    double total = systemManager.getPurchaseCostPreview(currentSelectedEvent.getId(), selectedOptionIndex, amount);
                    totalPriceLabel.setText(String.format("Total Price: %.2f$", total));
                } else {
                    if (tradePriceField != null && !tradePriceField.getText().isEmpty()) {
                        double price = Double.parseDouble(tradePriceField.getText());
                        double total = amount * price;
                        String type = (tradeDirectionCombo.getValue() == OrderDirection.SELL) ? "Revenue" : "Cost";
                        totalPriceLabel.setText(String.format("Total Est. %s: %.2f$", type, total));
                    } else {
                        totalPriceLabel.setText("Total Cost/Revenue: 0.00$");
                    }
                }
            } else {
                totalPriceLabel.setText("Total Cost/Revenue: 0.00$");
            }
        } catch (Exception e) {
            totalPriceLabel.setText("Invalid Input");
        }
    }
}