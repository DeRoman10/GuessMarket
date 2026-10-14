
package com.ui.controllers;

import com.data.events.entities.Event;
import com.data.events.entities.Option;
import com.data.events.entities.Ticket;
import com.data.events.enums.CommissionType;
import com.data.events.enums.EventStatus;
import com.data.events.enums.OrderDirection;
import com.data.users.dto.*;
import com.data.users.entities.AccountEntry;
import com.data.users.entities.Portfolio;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ui.core.IRefreshable;
import com.ui.core.ViewManager;
import com.ui.core.ApiClient;
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class UserController implements IRefreshable {

    // --- Left panel: OTHER users (read only) ---
    @FXML private TableView<AllUsersPublicData> usersTable;
    @FXML private TableColumn<AllUsersPublicData, String> userNameCol;
    @FXML private TableColumn<AllUsersPublicData, String> userBalanceCol;
    @FXML private TableColumn<AllUsersPublicData, String> userMmCol;

    // --- VIEW 1: Main dashboard ---
    @FXML private SplitPane mainUserView;
    @FXML private Label accountBalanceLabel;
    @FXML private Button loadFundsBtn;
    @FXML private LineChart<Number, Number> balanceChart;
    @FXML private TilePane userEventsTileContainer;
    @FXML private Label eventInfoLabel;
    @FXML private Button closeEventBtn;
    @FXML private Button openTradeViewBtn;
    @FXML private Button tradeSelectedEventBtn;
    @FXML private Button createEventBtn;

    // --- Account statement (one row per balance change). OPTIONAL in the FXML - see notes. ---
    @FXML private TableView<AccountEntry> ledgerTable;
    @FXML private TableColumn<AccountEntry, String> ledgerTimeCol;
    @FXML private TableColumn<AccountEntry, String> ledgerDescCol;
    @FXML private TableColumn<AccountEntry, String> ledgerAmountCol;
    @FXML private TableColumn<AccountEntry, String> ledgerBalanceCol;

    // --- VIEW 2: Trading view ---
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

    // --- VIEW 3: Create event (bonus) ---
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

    // --- state ---
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private ViewManager viewManager;
    private UserOwnPrivateData profile;                    // my own account, refreshed by every pull
    private String currentSelectedEventName;     // event shown in the main page overview
    private String tradeEventName;               // event chosen inside the trading view (kept separate on purpose)
    private boolean currentSelectedIsMM;
    private String eventTilesSignature = "";        // rebuild the tiles only when the set of events actually changed
    private boolean wasBlocked = false;
    private volatile boolean pullingProfile = false;
    private volatile boolean pullingUsers = false;
    private String lastLedgerKey = "";
    private String defaultEventInfoText = "";

    private Event tradeTargetEvent;
    private XYChart.Series<Number, Number> balanceSeries;
    private Button currentlySelectedOptionBtn = null;
    private int selectedOptionIndex = -1;

    private final okhttp3.OkHttpClient httpClient = ApiClient.http();
    private final Gson gson = ApiClient.GSON;

    private Button currentlySelectedTradeEventBtn = null;

    private static class SummaryResponse {
        List<Double> prices;
    }

    public void setManagers(ViewManager viewManager) {
        this.viewManager = viewManager;               // no network call here: the user is not logged in yet
    }

    /** Wipes everything that belongs to the previous user (selection, buttons, tables, trade view). */
    public void resetSession() {
        currentSelectedEventName = null;
        tradeEventName = null;
        currentSelectedIsMM = false;
        profile = null;
        wasBlocked = false;
        eventTilesSignature = "";
        lastLedgerKey = "";

        if (tradeSelectedEventBtn != null) { tradeSelectedEventBtn.setVisible(false); tradeSelectedEventBtn.setManaged(false); }
        if (closeEventBtn != null) { closeEventBtn.setVisible(false); closeEventBtn.setManaged(false); }
        if (eventInfoLabel != null) eventInfoLabel.setText(defaultEventInfoText);
        if (accountBalanceLabel != null) accountBalanceLabel.setText("");
        if (userEventsTileContainer != null) userEventsTileContainer.getChildren().clear();
        if (usersTable != null) usersTable.getItems().clear();
        if (ledgerTable != null) ledgerTable.getItems().clear();
        if (balanceSeries != null) balanceSeries.getData().clear();
        if (tradingView != null && mainUserView != null) handleCloseTradeView();
    }

    @Override
    public void refreshData() {
        fetchProfile();
        fetchOtherUsers();
        if (currentSelectedEventName != null) fetchEventDetails();
    }

    @FXML
    public void initialize() {
        if (eventInfoLabel != null) defaultEventInfoText = eventInfoLabel.getText();
        // 1. Restore the "Other Users" table columns
        if (userNameCol != null) userNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        if (userBalanceCol != null) userBalanceCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f$", c.getValue().getBalance())));
        if (userMmCol != null) userMmCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isMarketMaker() ? "Yes" : "No"));

        if (ledgerTimeCol != null) ledgerTimeCol.setCellValueFactory(c -> new SimpleStringProperty(TIME_FMT.format(Instant.ofEpochMilli(c.getValue().getTimestamp()))));
        if (ledgerDescCol != null) ledgerDescCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        if (ledgerAmountCol != null) ledgerAmountCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%+.2f$", c.getValue().getAmount())));
        if (ledgerBalanceCol != null) ledgerBalanceCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f$", c.getValue().getBalanceAfter())));

        balanceSeries = new XYChart.Series<>();
        balanceSeries.setName("Balance History");
        if (balanceChart != null) {
            balanceChart.setAnimated(false);
            balanceChart.getData().add(balanceSeries);
        }

        if (loadFundsBtn != null) loadFundsBtn.setDisable(true);

        if (tradeDirectionCombo != null) {
            tradeDirectionCombo.setItems(FXCollections.observableArrayList(OrderDirection.values()));
            tradeDirectionCombo.setValue(OrderDirection.BUY);
            tradeDirectionCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        }
        if (tradeAmountField != null) {
            tradeAmountField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        }
        if (tradePriceField != null) {
            tradePriceField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        }
        if (commissionTypeCombo != null) commissionTypeCombo.getItems().addAll(CommissionType.ON_PURCHASE, CommissionType.ON_CLOSE);
        if (methodTypeCombo != null) {
            methodTypeCombo.getItems().addAll("LMSR", "Order Book");
            methodTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                boolean isLmsr = "LMSR".equals(newVal);
                boolean isOB = "Order Book".equals(newVal);
                if (lmsrFieldsBox != null) { lmsrFieldsBox.setVisible(isLmsr); lmsrFieldsBox.setManaged(isLmsr); }
                if (obFieldsBox != null) { obFieldsBox.setVisible(isOB); obFieldsBox.setManaged(isOB); }
            });
        }
    }

    private void fetchProfile() {
        if (pullingProfile) return;
        pullingProfile = true;

        httpClient.newCall(ApiClient.get(ApiClient.url("/userHomePage").build())).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { pullingProfile = false; }

            @Override public void onResponse(Call call, Response response) throws IOException {
                pullingProfile = false;
                if (viewManager != null && viewManager.handleUnauthorized(response)) return;
                if (!response.isSuccessful()) return;
                UserOwnPrivateData p = gson.fromJson(response.body().string(), UserOwnPrivateData.class);
                if (p != null) Platform.runLater(() -> applyProfile(p));
            }
        });
    }

    private void fetchOtherUsers() {
        if (pullingUsers) return;
        pullingUsers = true;

        httpClient.newCall(ApiClient.get(ApiClient.url("/users").build())).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { pullingUsers = false; }

            @Override public void onResponse(Call call, Response response) throws IOException {
                pullingUsers = false;
                if (viewManager != null && viewManager.handleUnauthorized(response)) return;
                if (!response.isSuccessful()) return;
                Type listType = new TypeToken<List<AllUsersPublicData>>() {}.getType();
                List<AllUsersPublicData> others = gson.fromJson(response.body().string(), listType);
                if (others != null && usersTable != null) {
                    Platform.runLater(() -> {
                        AllUsersPublicData selectedUser = usersTable.getSelectionModel().getSelectedItem();
                        String selectedName = (selectedUser != null) ? selectedUser.getName() : null;
                        usersTable.setItems(FXCollections.observableArrayList(others));

                        if (selectedName != null) {
                            for (AllUsersPublicData u : usersTable.getItems()) {
                                if (u.getName().equals(selectedName)) {
                                    usersTable.getSelectionModel().select(u);
                                    break;
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void applyProfile(UserOwnPrivateData p) {
        this.profile = p;

        if (accountBalanceLabel != null) {
            accountBalanceLabel.setText(String.format("User: %s | Balance: %.2f$%s", p.getName(), p.getBalance(),
                    p.isBlocked() ? "   [BLOCKED: negative balance - load funds to continue]" : ""));
        }
        if (p.isBlocked() && !wasBlocked) {
            Alert a = new Alert(Alert.AlertType.WARNING,
                    "Your balance is negative. Your account is blocked: you cannot trade until you load funds.");
            a.setTitle("Account Blocked");
            a.setHeaderText(null);
            a.show();
        }
        wasBlocked = p.isBlocked();

        if (loadFundsBtn != null) loadFundsBtn.setDisable(false);
        if (openTradeViewBtn != null) openTradeViewBtn.setDisable(p.isBlocked());
        if (createEventBtn != null) { createEventBtn.setDisable(false); createEventBtn.setVisible(true); }
        if (tradeSelectedEventBtn != null && currentSelectedEventName != null) tradeSelectedEventBtn.setDisable(p.isBlocked());

        // the ledger is append-only: redraw the graph / table only when a row was added or the balance changed
        String ledgerKey = p.getName() + "#" + p.getLedger().size() + "#" + p.getBalance();
        if (!ledgerKey.equals(lastLedgerKey)) {
            lastLedgerKey = ledgerKey;
            drawBalanceGraph(p);
            updateLedgerTable(p);
        }

        String signature = String.join("|", p.getManagedEvents()) + "#" + String.join("|", p.getPortfolios().keySet());
        if (!signature.equals(eventTilesSignature)) {
            eventTilesSignature = signature;
            populateUserEvents(p);
        }
    }

    private void updateLedgerTable(UserOwnPrivateData p) {
        if (ledgerTable == null) return;
        int oldSize = ledgerTable.getItems() == null ? 0 : ledgerTable.getItems().size();
        int selected = ledgerTable.getSelectionModel().getSelectedIndex();

        List<AccountEntry> rows = new ArrayList<>(p.getLedger());
        Collections.reverse(rows);                                    // newest first
        ledgerTable.setItems(FXCollections.observableArrayList(rows));

        if (selected >= 0 && oldSize > 0) {                           // new rows are added on top: shift the selection
            int newIndex = rows.size() - 1 - (oldSize - 1 - selected);
            if (newIndex >= 0 && newIndex < rows.size()) ledgerTable.getSelectionModel().select(newIndex);
        }
    }

    private void drawBalanceGraph(UserOwnPrivateData p) {
        if (balanceSeries == null) return;
        List<AccountEntry> ledger = p.getLedger();
        balanceSeries.getData().clear();
        if (ledger.isEmpty()) {
            balanceSeries.getData().add(new XYChart.Data<>(0, p.getBalance()));
            return;
        }
        AccountEntry first = ledger.get(0);
        balanceSeries.getData().add(new XYChart.Data<>(0, first.getBalanceAfter() - first.getAmount()));   // balance before the first row
        for (int i = 0; i < ledger.size(); i++) {
            balanceSeries.getData().add(new XYChart.Data<>(i + 1, ledger.get(i).getBalanceAfter()));
        }
    }

    private void populateUserEvents(UserOwnPrivateData p) {
        if (userEventsTileContainer == null) return;

        Set<String> involved = new LinkedHashSet<>(p.getManagedEvents());
        involved.addAll(p.getPortfolios().keySet());

        if (involved.isEmpty()) {
            userEventsTileContainer.getChildren().clear();
            Label empty = new Label("No active participations. Click 'Trade Market' below to start!");
            empty.getStyleClass().add("sub-text");
            userEventsTileContainer.getChildren().add(empty);
            return;
        }

        if (!userEventsTileContainer.getChildren().isEmpty() && userEventsTileContainer.getChildren().get(0) instanceof Label) {
            userEventsTileContainer.getChildren().clear();
        }

        int currentIndex = 0;
        for (String eventName : involved) {
            boolean isMM = p.getManagedEvents().contains(eventName);
            String btnText = (isMM ? "[MM] " : "[Participant] ") + eventName;

            if (currentIndex < userEventsTileContainer.getChildren().size()) {
                Button existingBtn = (Button) userEventsTileContainer.getChildren().get(currentIndex);
                existingBtn.setText(btnText);
                existingBtn.setUserData(eventName);
                existingBtn.setOnAction(e -> selectEvent(eventName, isMM));

                if (currentSelectedEventName != null && currentSelectedEventName.equals(eventName)) {
                    existingBtn.getStyleClass().remove("action-button");
                    if (!existingBtn.getStyleClass().contains("action-button-selected")) {
                        existingBtn.getStyleClass().add("action-button-selected");
                    }
                } else {
                    existingBtn.getStyleClass().remove("action-button-selected");
                    if (!existingBtn.getStyleClass().contains("action-button")) {
                        existingBtn.getStyleClass().add("action-button");
                    }
                }
            } else {
                Button newBtn = new Button(btnText);
                newBtn.setUserData(eventName);
                newBtn.getStyleClass().add("action-button");
                newBtn.setMaxWidth(Double.MAX_VALUE);
                newBtn.setOnAction(e -> selectEvent(eventName, isMM));

                if (currentSelectedEventName != null && currentSelectedEventName.equals(eventName)) {
                    newBtn.getStyleClass().remove("action-button");
                    newBtn.getStyleClass().add("action-button-selected");
                }
                userEventsTileContainer.getChildren().add(newBtn);
            }
            currentIndex++;
        }

        while (userEventsTileContainer.getChildren().size() > involved.size()) {
            userEventsTileContainer.getChildren().remove(userEventsTileContainer.getChildren().size() - 1);
        }
    }


    private void selectEvent(String eventName, boolean isMM) {
        this.currentSelectedEventName = eventName;
        this.currentSelectedIsMM = isMM;

        if (tradeSelectedEventBtn != null) {
            tradeSelectedEventBtn.setVisible(true);
            tradeSelectedEventBtn.setManaged(true);
            tradeSelectedEventBtn.setDisable(profile != null && profile.isBlocked());
        }
        if (closeEventBtn != null) { closeEventBtn.setVisible(isMM); closeEventBtn.setManaged(isMM); }
        restyleUserEventTiles();
        fetchEventDetails();
    }

    /** Highlights the tile of the selected event (tiles carry their event name in userData). */
    private void restyleUserEventTiles() {
        if (userEventsTileContainer == null) return;
        for (javafx.scene.Node node : userEventsTileContainer.getChildren()) {
            if (!(node instanceof Button btn)) continue;
            boolean selected = currentSelectedEventName != null && currentSelectedEventName.equals(btn.getUserData());
            btn.getStyleClass().removeAll("action-button", "action-button-selected");
            btn.getStyleClass().add(selected ? "action-button-selected" : "action-button");
        }
    }

    private void fetchEventDetails() {
        final String eventName = currentSelectedEventName;
        if (eventName == null) return;

        Request request = ApiClient.get(ApiClient.url("/events").addQueryParameter("name", eventName).build());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (viewManager != null && viewManager.handleUnauthorized(response)) return;
                String status = response.isSuccessful() ? ApiClient.message(response.body().string()) : "Error fetching details.";
                Platform.runLater(() -> {
                    if (!eventName.equals(currentSelectedEventName)) return;       // user clicked another event meanwhile
                    if (eventInfoLabel != null) eventInfoLabel.setText(status + involvementText(eventName, status));
                });
            }
        });
    }

    /** Task 2's "my involvement in this event" block, built from my private profile. */
    private String involvementText(String eventName, String globalStatus) {
        if (profile == null) return "";
        StringBuilder sb = new StringBuilder(String.format("%n--- Your involvement ---%n"));
        if (profile.getManagedEvents().contains(eventName)) sb.append("You are the Market Maker of this event.\n");

        Portfolio p = profile.getPortfolios().get(eventName);
        if (p == null) return sb.append("You have no trades in this event yet.\n").toString();

        for (Map.Entry<String, Double> h : p.getHoldings().entrySet()) {
            if (h.getValue() > 0) sb.append(String.format("  %s: %.0f shares%n", h.getKey(), h.getValue()));
        }
        sb.append(String.format("Total invested: %.2f$ | Commission paid: %.2f$%n", p.getTotalInvestment(), p.getTotalCommissionPaid()));
        if (globalStatus != null && globalStatus.contains("Event is CLOSED")) {
            sb.append(String.format("Payout received: %.2f$ | Profit/Loss: %+.2f$%n", p.getRevenue(), p.getRevenue() - p.getTotalInvestment()));
        }

        List<Ticket> history = new ArrayList<>(p.getHistory());
        Collections.reverse(history);                                              // latest first
        sb.append(String.format("Your trades (latest first):%n"));
        for (Ticket t : history) {
            sb.append(String.format("  %s %d x %s - price paid %.2f$%n",
                    t.getAction() != null ? t.getAction() : "BUY", t.getAmount(), t.getOption().getName(), t.getPricePaid()));
        }
        return sb.toString();
    }

    // =====================================================================================
    //  Actions: load funds / close event
    // =====================================================================================

    @FXML
    private void handleDepositFunds() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Load Funds");
        dialog.setHeaderText("Deposit money to your account");
        dialog.setContentText("Amount ($):");

        dialog.showAndWait().ifPresent(amountStr -> {
            double amount;
            try {
                amount = Double.parseDouble(amountStr.trim());
                if (!Double.isFinite(amount) || amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid positive number.");
                return;
            }

            Request request = ApiClient.post(ApiClient.url("/deposit").addQueryParameter("amount", String.valueOf(amount)).build());
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Network Error", "Could not connect to server."));
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (viewManager.handleUnauthorized(response)) return;
                    String msg = ApiClient.message(response.body().string());
                    Platform.runLater(() -> {
                        if (response.isSuccessful()) {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Funds loaded successfully.");
                            refreshData();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Deposit Failed", msg);
                        }
                    });
                }
            });
        });
    }

    @FXML
    private void handleCloseEvent() {
        final String eventName = currentSelectedEventName;
        if (eventName == null || !currentSelectedIsMM) return;

        // Fetch the event's options so the MM picks the winner by NAME instead of typing an index.
        httpClient.newCall(ApiClient.get(ApiClient.url("/events").build())).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (viewManager.handleUnauthorized(response)) return;
                if (!response.isSuccessful()) return;
                Type listType = new TypeToken<List<Event>>() {}.getType();
                List<Event> events = gson.fromJson(response.body().string(), listType);
                Event target = events.stream().filter(e -> e.getName().equals(eventName)).findFirst().orElse(null);
                if (target == null) return;
                Platform.runLater(() -> askWinnerAndClose(target));
            }
        });
    }

    private void askWinnerAndClose(Event event) {
        List<String> names = event.getOptions().stream().map(Option::getName).collect(Collectors.toList());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Close Event");
        dialog.setHeaderText("Close event: " + event.getName());
        dialog.setContentText("Winning option:");

        dialog.showAndWait().ifPresent(winner -> {
            Request request = ApiClient.post(ApiClient.url("/closeEvent")
                    .addQueryParameter("eventName", event.getName())
                    .addQueryParameter("winningOptionIndex", String.valueOf(names.indexOf(winner))).build());

            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Network Error", "Could not connect to server."));
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (viewManager.handleUnauthorized(response)) return;
                    String msg = ApiClient.message(response.body().string());
                    Platform.runLater(() -> {
                        if (response.isSuccessful()) {
                            showAlert(Alert.AlertType.INFORMATION, "Event Closed", msg);
                            refreshData();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Closure Failed", msg);
                        }
                    });
                }
            });
        });
    }

    // =====================================================================================
    //  Trading view
    // =====================================================================================

    @FXML
    private void handleOpenTradeView() {
        if (profile == null) return;
        mainUserView.setVisible(false);
        mainUserView.setManaged(false);
        tradingView.setVisible(true);
        tradingView.setManaged(true);
        placeOrderPane.setVisible(false);
        placeOrderPane.setManaged(false);
        tradeEventName = null;
        fetchActiveEventsForTrading();
    }

    @FXML
    private void handleTradeSelectedEvent() {
        if (currentSelectedEventName == null) return;
        tradeEventName = currentSelectedEventName;
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
        currentlySelectedOptionBtn = null;
        selectedOptionIndex = -1;
    }

    private void fetchActiveEventsForTrading() {
        httpClient.newCall(ApiClient.get(ApiClient.url("/events").build())).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (viewManager != null && viewManager.handleUnauthorized(response)) return;
                if (!response.isSuccessful()) return;

                Type listType = new TypeToken<List<Event>>() {}.getType();
                List<Event> events = gson.fromJson(response.body().string(), listType);

                Platform.runLater(() -> {
                    if (tradeEventsTileContainer != null) tradeEventsTileContainer.getChildren().clear();
                    currentlySelectedTradeEventBtn = null;

                    for (Event event : events) {
                        if (event.getStatus() != EventStatus.ACTIVE) continue; // only open events can be traded

                        Button tile = new Button(event.getName());
                        tile.getStyleClass().add("action-button");
                        tile.setMaxWidth(Double.MAX_VALUE);

                        if (tradeEventName != null && event.getName().equals(tradeEventName)) {
                            tile.getStyleClass().remove("action-button");
                            tile.getStyleClass().add("action-button-selected");
                            currentlySelectedTradeEventBtn = tile;
                            populateOptionsList(event);
                        }

                        tile.setOnAction(e -> {
                            if (currentlySelectedTradeEventBtn != null) {
                                currentlySelectedTradeEventBtn.getStyleClass().remove("action-button-selected");
                                currentlySelectedTradeEventBtn.getStyleClass().add("action-button");
                            }
                            tile.getStyleClass().remove("action-button");
                            tile.getStyleClass().add("action-button-selected");
                            currentlySelectedTradeEventBtn = tile;

                            tradeEventName = event.getName();
                            populateOptionsList(event);
                        });
                        tradeEventsTileContainer.getChildren().add(tile);
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

        boolean isLmsr = event.isLmsr();
        if (priceLabel != null) {
            priceLabel.setVisible(!isLmsr);
            priceLabel.setManaged(!isLmsr);
        }
        if (tradePriceField != null) {
            tradePriceField.setVisible(!isLmsr);
            tradePriceField.setManaged(!isLmsr);
        }
        if (directionLabel != null) {
            directionLabel.setVisible(!isLmsr);
            directionLabel.setManaged(!isLmsr);
        }
        if (tradeDirectionCombo != null) {
            tradeDirectionCombo.setVisible(!isLmsr);
            tradeDirectionCombo.setManaged(!isLmsr);
        }

        if (amountLabel != null) amountLabel.setVisible(false);
        if (tradeAmountField != null) tradeAmountField.setVisible(false);
        if (submitTradeBtn != null) submitTradeBtn.setVisible(false);

        optionsContainer.getChildren().clear();
        selectedOptionIndex = -1;
        currentlySelectedOptionBtn = null;

        Request request = ApiClient.get(ApiClient.url("/marketData")
                .addQueryParameter("action", "summary")
                .addQueryParameter("eventName", event.getName()).build());

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                SummaryResponse data = gson.fromJson(response.body().string(), SummaryResponse.class);

                Platform.runLater(() -> {
                    if (!event.getName().equals(tradeTargetEvent.getName())) return;

                    optionsContainer.getChildren().clear();
                    List<Option> options = event.getOptions();

                    for (int i = 0; i < options.size(); i++) {
                        Option option = options.get(i);
                        int index = i;
                        double price = (data.prices != null && i < data.prices.size()) ? data.prices.get(i) : 0.0;

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
                            selectedOptionIndex = index;

                            if (amountLabel != null) amountLabel.setVisible(true);
                            if (tradeAmountField != null) tradeAmountField.setVisible(true);
                            if (submitTradeBtn != null) submitTradeBtn.setVisible(true);
                            updateTotalPreview();
                        });
                        optionsContainer.getChildren().add(optBtn);
                    }
                });
            }
        });
    }

    @FXML
    private void handleSubmitTrade() {
        if (selectedOptionIndex == -1 || tradeAmountField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Form", "Please fill all required fields.");
            return;
        }

        try {
            int amount = Integer.parseInt(tradeAmountField.getText().trim());

            HttpUrl.Builder url = ApiClient.url("/trade")
                    .addQueryParameter("eventName", tradeTargetEvent.getName())
                    .addQueryParameter("optionIndex", String.valueOf(selectedOptionIndex))
                    .addQueryParameter("amount", String.valueOf(amount));

            if (!tradeTargetEvent.isLmsr()) {
                if (tradePriceField.getText().isEmpty() || tradeDirectionCombo.getValue() == null) throw new NumberFormatException();
                url.addQueryParameter("limitPrice", tradePriceField.getText().trim());
                url.addQueryParameter("direction", tradeDirectionCombo.getValue().name());
            }

            httpClient.newCall(ApiClient.post(url.build())).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Network Error", "Could not connect to server."));
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (viewManager.handleUnauthorized(response)) return;
                    String body = response.body().string();
                    Platform.runLater(() -> {
                        if (response.isSuccessful()) {
                            handleCloseTradeView();
                            showAlert(Alert.AlertType.INFORMATION, "Trade Successful", "Your transaction was executed.");
                            refreshData();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Transaction Failed", ApiClient.message(body));
                        }
                    });
                }
            });
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers.");
        }
    }

    private void updateTotalPreview() {
        if (totalPriceLabel == null || tradeTargetEvent == null || selectedOptionIndex == -1 || tradeAmountField.getText().isEmpty()) {
            if (totalPriceLabel != null) totalPriceLabel.setText("Total Cost/Revenue: 0.00$");
            return;
        }

        try {
            int amount = Integer.parseInt(tradeAmountField.getText().trim());
            if (amount <= 0) {
                totalPriceLabel.setText("Invalid Amount");
                return;
            }

            if (tradeTargetEvent.isLmsr()) {
                Request request = ApiClient.get(ApiClient.url("/marketData")
                        .addQueryParameter("action", "costPreview")
                        .addQueryParameter("eventName", tradeTargetEvent.getName())
                        .addQueryParameter("optionIndex", String.valueOf(selectedOptionIndex))
                        .addQueryParameter("amount", String.valueOf(amount))
                        .build());

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        Platform.runLater(() -> totalPriceLabel.setText("Network Error"));
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            Platform.runLater(() -> totalPriceLabel.setText("Server Error"));
                            return;
                        }

                        java.util.Map<String, Double> data = gson.fromJson(response.body().string(), new TypeToken<java.util.Map<String, Double>>(){}.getType());

                        Platform.runLater(() -> {
                            if (data != null && data.containsKey("cost")) {
                                totalPriceLabel.setText(String.format("Total Est. Cost: %.2f$", data.get("cost")));
                            } else {
                                totalPriceLabel.setText("Parse Error");
                            }
                        });
                    }
                });
            } else {
                if (tradePriceField != null && !tradePriceField.getText().isEmpty()) {
                    double price = Double.parseDouble(tradePriceField.getText().trim());
                    double total = amount * price;
                    String type = (tradeDirectionCombo.getValue() == OrderDirection.SELL) ? "Revenue" : "Cost";
                    totalPriceLabel.setText(String.format("Total Est. %s: %.2f$", type, total));
                } else {
                    totalPriceLabel.setText("Total Cost/Revenue: 0.00$");
                }
            }
        } catch (NumberFormatException e) {
            totalPriceLabel.setText("Invalid Input");
        }
    }

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

    @FXML
    private void handleApplyEvent() {
        try {
            String method = methodTypeCombo.getValue();
            HttpUrl.Builder url = ApiClient.url("/createEvent")
                    .addQueryParameter("name", eventNameField.getText())
                    .addQueryParameter("description", descriptionField.getText())
                    .addQueryParameter("commissionRate", commissionRateField.getText().trim())
                    .addQueryParameter("commissionType", commissionTypeCombo.getValue().name())
                    .addQueryParameter("option1", option1Field.getText())
                    .addQueryParameter("option2", option2Field.getText())
                    .addQueryParameter("method", method);

            if ("LMSR".equals(method)) {
                url.addQueryParameter("b", liquidityParamField.getText().trim());
            } else {
                url.addQueryParameter("initial", initialInvestmentField.getText().trim());
                url.addQueryParameter("d", baseValueField.getText().trim());
                url.addQueryParameter("allowMint", String.valueOf(allowMintCheckBox.isSelected()));
            }

            httpClient.newCall(ApiClient.post(url.build())).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Network Error", "Could not connect to server."));
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (viewManager.handleUnauthorized(response)) return;
                    String msg = ApiClient.message(response.body().string());
                    Platform.runLater(() -> {
                        if (response.isSuccessful()) {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Event created successfully!");
                            handleCancelCreateEvent();
                            refreshData();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Creation Failed", msg);
                        }
                    });
                }
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please check your input fields.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

