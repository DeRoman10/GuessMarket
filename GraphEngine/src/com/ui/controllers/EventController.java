package com.ui.controllers;

import com.data.events.entities.Event;
import com.data.events.entities.OBOrder;
import com.data.events.entities.Option;
import com.data.events.entities.Ticket;
import com.data.events.enums.EventStatus;
import com.maneger.SystemManager;
import com.ui.core.IRefreshable;
import com.ui.core.ViewManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class EventController implements IRefreshable {

    @Override
    public void refreshData() {
        handleRefresh();
    }

    // --- Top / Filter UI Elements ---
    @FXML private TextField filePathField;
    @FXML private ComboBox<String> methodFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> commissionFilterCombo;

    // --- Left Panel UI Elements ---
    @FXML private VBox eventsListPanel;
    @FXML private TilePane eventsTileContainer;
    @FXML private Button openBuyFormBtn;

    // --- Left Panel: Buy Form Elements ---
    @FXML private VBox buyFormPanel;
    @FXML private VBox optionsContainer;
    @FXML private TextField buyAmountField;

    // --- Center/Right Panel UI Elements ---
    @FXML private HBox orderBookTablesBox;
    @FXML private TableView<OBOrder> buyOrdersTable;
    @FXML private TableColumn<OBOrder, String> buyPriceCol;
    @FXML private TableColumn<OBOrder, Integer> buyAmountCol;
    @FXML private TableColumn<OBOrder, String> buyOptionCol;
    @FXML private TableColumn<OBOrder, String> sellOptionCol;

    @FXML private TableView<OBOrder> sellOrdersTable;
    @FXML private TableColumn<OBOrder, String> sellPriceCol;
    @FXML private TableColumn<OBOrder, Integer> sellAmountCol;

    @FXML private TableView<Ticket> historyTable;
    @FXML private TableColumn<Ticket, String> historyOptionCol;
    @FXML private TableColumn<Ticket, Integer> historyAmountCol;
    @FXML private TableColumn<Ticket, String> historyPriceCol;

    @FXML private LineChart<Number, Number> marketChart;
    @FXML private NumberAxis dealXAxis;
    @FXML private NumberAxis priceYAxis;

    private SystemManager systemManager;
    private ViewManager viewManager;
    private XYChart.Series<Number, Number> priceSeries;
    private Event currentSelectedEvent;

    @FXML private HBox optionsSummaryContainer;

    private Button currentlySelectedEventBtn = null;
    private Button currentlySelectedOptionBtn = null;
    private Option selectedOption = null;
    private int selectedOptionIndex = -1;

    public void setManagers(SystemManager manager, ViewManager viewManager) {
        this.systemManager = manager;
        this.viewManager = viewManager;
    }

    @FXML
    public void initialize() {
        // 1. Setup Order Book Columns
        if (buyPriceCol != null) {
            buyPriceCol.setCellValueFactory(cell ->
                    new SimpleStringProperty(String.format("%.2f$", cell.getValue().getPrice())));
        }
        if (buyAmountCol != null) {
            buyAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        }
        if (sellPriceCol != null) {
            sellPriceCol.setCellValueFactory(cell ->
                    new SimpleStringProperty(String.format("%.2f$", cell.getValue().getPrice())));
        }
        if (sellAmountCol != null) {
            sellAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        }

        // 2. Setup History Columns
        if (historyAmountCol != null) {
            historyAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        }
        if (historyPriceCol != null) {
            historyPriceCol.setCellValueFactory(cell ->
                    new SimpleStringProperty(String.format("%.2f$", cell.getValue().getPricePaid())));
        }
        if (historyOptionCol != null) {
            historyOptionCol.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getOption().getName()));
        }

        if (dealXAxis != null) {
            dealXAxis.setForceZeroInRange(true);
            dealXAxis.setAutoRanging(true);
        }

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Deal Price History");
        if (marketChart != null) marketChart.getData().add(priceSeries);

        if (methodFilterCombo != null) {
            methodFilterCombo.getItems().addAll("All Methods", "LMSR", "Order Book");
            methodFilterCombo.setValue("All Methods");
            methodFilterCombo.setOnAction(e -> handleRefresh());
        }

        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().clear();
            statusFilterCombo.getItems().add("All Statuses");
            for (EventStatus status : EventStatus.values()) {
                statusFilterCombo.getItems().add(status.name());
            }
            statusFilterCombo.setValue("All Statuses");
            statusFilterCombo.setOnAction(e -> handleRefresh());
        }

        if (commissionFilterCombo != null) {
            commissionFilterCombo.getItems().clear();
            commissionFilterCombo.getItems().addAll("All Commissions", "On Purchase", "On Close");
            commissionFilterCombo.setValue("All Commissions");
            commissionFilterCombo.setOnAction(e -> handleRefresh());
        }

        if (buyOptionCol != null) {
            buyOptionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOption().getName()));
        }
        if (sellOptionCol != null) {
            sellOptionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOption().getName()));
        }
    }

    @FXML
    public void handleRefresh() {
        if (systemManager != null) {
            populateEventTiles();
            if (currentSelectedEvent != null) {
                selectEvent(currentSelectedEvent, currentlySelectedEventBtn);
            }
        }
    }

    private void populateEventTiles() {
        if (eventsTileContainer == null || systemManager == null) return;
        eventsTileContainer.getChildren().clear();

        String selectedMethod = (methodFilterCombo != null && methodFilterCombo.getValue() != null) ? methodFilterCombo.getValue() : "All Methods";
        String selectedStatus = (statusFilterCombo != null && statusFilterCombo.getValue() != null) ? statusFilterCombo.getValue() : "All Statuses";
        String selectedCommission = (commissionFilterCombo != null && commissionFilterCombo.getValue() != null) ? commissionFilterCombo.getValue() : "All Commissions";

        for (Event event : systemManager.getEvents()) {
            boolean isLmsr = event.isLmsr();
            if ("LMSR".equals(selectedMethod) && !isLmsr) continue;
            if ("Order Book".equals(selectedMethod) && isLmsr) continue;

            if (!"All Statuses".equals(selectedStatus)) {
                if (!event.getStatus().name().equals(selectedStatus)) continue;
            }

            if (!"All Commissions".equals(selectedCommission)) {
                com.data.events.enums.CommissionType evComm = event.getCommissionType();
                if ("On Purchase".equals(selectedCommission) && evComm != com.data.events.enums.CommissionType.ON_PURCHASE) continue;
                if ("On Close".equals(selectedCommission) && evComm != com.data.events.enums.CommissionType.ON_CLOSE) continue;
            }

            Button eventTile = new Button(event.getName() + " (" + event.getStatus() + ")");
            eventTile.getStyleClass().add("action-button");
            eventTile.setMaxWidth(Double.MAX_VALUE);
            eventTile.setPrefHeight(60);

            if (currentSelectedEvent != null && currentSelectedEvent.getId().equals(event.getId())) {
                eventTile.getStyleClass().remove("action-button");
                eventTile.getStyleClass().add("action-button-selected");
                currentlySelectedEventBtn = eventTile;
            }

            eventTile.setOnAction(e -> selectEvent(event, eventTile));
            eventsTileContainer.getChildren().add(eventTile);
        }
    }

    private void selectEvent(Event event, Button clickedBtn) {
        this.currentSelectedEvent = event;

        updateEventButtonSelection(clickedBtn);
        updateUIForSelectedEvent(event);
        populateChartAndHistory(event);
        populateOptionsSummary(event);
        populateOrderBookTables(event);
    }

    private void updateEventButtonSelection(Button clickedBtn) {
        if (currentlySelectedEventBtn != null) {
            currentlySelectedEventBtn.getStyleClass().remove("action-button-selected");
            currentlySelectedEventBtn.getStyleClass().add("action-button");
        }
        if (clickedBtn != null) {
            clickedBtn.getStyleClass().remove("action-button");
            clickedBtn.getStyleClass().add("action-button-selected");
            currentlySelectedEventBtn = clickedBtn;
        }
    }

    private void updateUIForSelectedEvent(Event event) {
        if (openBuyFormBtn != null) {
            openBuyFormBtn.setDisable(false);
        }

        boolean isLmsr = event.isLmsr();
        if (orderBookTablesBox != null) {
            orderBookTablesBox.setVisible(!isLmsr);
            orderBookTablesBox.setManaged(!isLmsr);
        }
    }

    private void populateChartAndHistory(Event event) {
        initializeChart(event);
        plotHistoryData(event);
        updateHistoryTable(event.getHistory());
    }

    private void initializeChart(Event event) {
        priceSeries.getData().clear();
        double initialPrice = event.getOptions().isEmpty() ? 0.0 : 1.0 / event.getOptions().size();
        priceSeries.getData().add(new XYChart.Data<>(0, initialPrice));

        if (event.getOptions() != null && !event.getOptions().isEmpty()) {
            String firstOptionName = event.getOptions().get(0).getName();
            if (priceYAxis != null) {
                priceYAxis.setLabel("Price for " + firstOptionName);
            }
        }
    }

    private void plotHistoryData(Event event) {
        List<Ticket> history = event.getHistory();
        if (history == null || history.isEmpty()) {
            return;
        }

        if (event.isLmsr()) {
            double[] q = new double[event.getOptions().size()];
            int b = getLmsrBParameter(event);

            for (int i = 0; i < history.size(); i++) {
                updateShares(q, event, history.get(i));
                double firstOptionPrice = calculateLmsrMarginalPrice(q, b);
                priceSeries.getData().add(new XYChart.Data<>(i + 1, firstOptionPrice));
            }
        } else {
            String firstOptionName = event.getOptions().get(0).getName();
            int dealIndex = 1;
            for (Ticket ticket : history) {
                if (ticket.getOption().getName().equals(firstOptionName)) {
                    priceSeries.getData().add(new XYChart.Data<>(dealIndex, ticket.getPricePaid()));
                }
                dealIndex++;
            }
        }
    }

    private int getLmsrBParameter(Event event) {
        if (event.isLmsr()) {
            return event.getMethod().getLmsr().getB();
        }
        return 100;
    }

    private void updateShares(double[] q, Event event, Ticket ticket) {
        for (int j = 0; j < event.getOptions().size(); j++) {
            if (event.getOptions().get(j).getName().equals(ticket.getOption().getName())) {
                q[j] += ticket.getAmount();
                break;
            }
        }
    }

    private double calculateLmsrMarginalPrice(double[] q, int b) {
        double sumExp = 0;
        for (double shares : q) {
            sumExp += Math.exp(shares / b);
        }
        return Math.exp(q[0] / b) / sumExp;
    }

    private void updateHistoryTable(List<Ticket> history) {
        if (historyTable != null && history != null) {
            historyTable.setItems(FXCollections.observableArrayList(history));
        }
    }

    @FXML
    private void handleOpenBuyForm() {
        // Buying here has no notion of "who is buying" - this screen has no
        // selected user. Trading must always be attributed to a real user
        // (their balance and portfolio need to be updated), so this quick-buy
        // form is not a valid purchase flow. Route to the User Dashboard's
        // Trade view instead, which already selects a real user before trading.
        showAlert(Alert.AlertType.INFORMATION, "Select a User to Trade",
                "To buy or sell shares, go to the User Dashboard, pick a user, "
                        + "then choose \"Trade Market\" for this event so the trade "
                        + "can be attributed to their account.");
    }

    @FXML
    private void handleCloseBuyForm() {
        buyFormPanel.setVisible(false);
        buyFormPanel.setManaged(false);

        eventsListPanel.setVisible(true);
        eventsListPanel.setManaged(true);

        if (buyAmountField != null) buyAmountField.clear();
        selectedOption = null;
        selectedOptionIndex = -1;
    }

    private void populateOptionsList() {
        optionsContainer.getChildren().clear();
        currentlySelectedOptionBtn = null;
        selectedOption = null;
        selectedOptionIndex = -1;

        List<Option> options = currentSelectedEvent.getOptions();

        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int currentIndex = i;

            double price = 0.0;
            try {
                price = systemManager.getOptionCurrentPrice(currentSelectedEvent.getId(), currentIndex);
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
            });
            optionsContainer.getChildren().add(optBtn);
        }
    }

    @FXML
    private void handleSubmitBuy() {
        // This form is never opened anymore (see handleOpenBuyForm) since it has
        // no attributable user. Kept as a safety net in case it is still wired
        // to a control in the FXML, so it can never silently execute an
        // un-attributed trade.
        handleOpenBuyForm();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void populateOptionsSummary(Event event) {
        if (optionsSummaryContainer == null) return;
        optionsSummaryContainer.getChildren().clear();

        boolean isOB = event.isOrderBook();
        List<Option> options = event.getOptions();

        for (int i = 0; i < options.size(); i++) {
            Option opt = options.get(i);
            VBox card = isOB ? buildOBCard(event, opt, i) : buildLMSRCard(event, opt, i);
            optionsSummaryContainer.getChildren().add(card);
        }
        if (isOB) {
            optionsSummaryContainer.getChildren().add(buildParticipantsCard(event));
        }
    }

    private VBox buildParticipantsCard(Event event) {
        VBox card = new VBox(2);
        javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("side-panel");
        card.setStyle("-fx-padding: 10px; -fx-border-color: #888888; -fx-border-width: 1px; -fx-border-radius: 5px;");

        Label titleLabel = new Label("Participants Overview");
        titleLabel.getStyleClass().add("section-title");
        titleLabel.setStyle("-fx-font-size: 13px;");

        String overview;
        try {
            overview = systemManager.getOBParticipantsOverview(event.getId());
        } catch (Exception e) {
            overview = "Error loading participants: " + e.getMessage();
        }

        Label contentLabel = new Label(overview);
        contentLabel.getStyleClass().add("sub-text");
        contentLabel.setWrapText(true);

        ScrollPane scrollPane = new ScrollPane(contentLabel);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("dark-scroll");
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPrefHeight(90);

        card.getChildren().addAll(titleLabel, scrollPane);
        return card;
    }

    private VBox buildLMSRCard(Event event, Option opt, int index) {
        VBox card = createBaseCard(opt.getName());

        try {
            double price = systemManager.getOptionCurrentPrice(event.getId(), index);
            Label priceLabel = new Label(String.format("Price: $%.2f", price));
            priceLabel.getStyleClass().add("buy-label");
            card.getChildren().add(priceLabel);
        } catch (Exception e) {
            System.err.println("Could not load price: " + e.getMessage());
        }

        Label sharesLabel = new Label(String.format("Total Shares: %.2f", opt.getTotalShares()));
        sharesLabel.getStyleClass().add("sub-text");
        card.getChildren().add(sharesLabel);

        return card;
    }

    private VBox buildOBCard(Event event, Option opt, int index) {
        VBox card = createBaseCard(opt.getName());

        try {
            java.util.Map<String, Double> stats = systemManager.getOptionOBStats(event.getId(), index);

            Label lastLabel = new Label(String.format("LAST: $%.2f", stats.getOrDefault("LAST", 0.0)));
            lastLabel.getStyleClass().add("buy-label");

            Label bidAskLabel = new Label(String.format("BID: $%.2f | ASK: $%.2f", stats.getOrDefault("BID", 0.0), stats.getOrDefault("ASK", 0.0)));
            bidAskLabel.getStyleClass().add("sub-text");

            Label midSpreadLabel = new Label(String.format("MID: $%.2f | SPREAD: $%.2f", stats.getOrDefault("MID", 0.0), stats.getOrDefault("SPREAD", 0.0)));
            midSpreadLabel.getStyleClass().add("sub-text");

            card.getChildren().addAll(lastLabel, bidAskLabel, midSpreadLabel);
        } catch (Exception e) {
            card.getChildren().add(new Label("Stats Error"));
        }

        Label sharesLabel = new Label(String.format("Total Shares: %.2f", opt.getTotalShares()));
        sharesLabel.getStyleClass().add("sub-text");
        card.getChildren().add(sharesLabel);

        return card;
    }

    private VBox createBaseCard(String optionName) {
        VBox card = new VBox(2);
        javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("side-panel");
        card.setStyle("-fx-padding: 10px; -fx-border-color: #888888; -fx-border-width: 1px; -fx-border-radius: 5px;");

        Label nameLabel = new Label(optionName);
        nameLabel.getStyleClass().add("section-title");
        nameLabel.setStyle("-fx-font-size: 13px;");
        nameLabel.setWrapText(true);
        card.getChildren().add(nameLabel);

        return card;
    }

    private void populateOrderBookTables(Event event) {
        if (buyOrdersTable == null || sellOrdersTable == null) return;

        boolean isOB = event.isOrderBook();
        if (!isOB) {
            buyOrdersTable.getItems().clear();
            sellOrdersTable.getItems().clear();
            return;
        }

        try {
            List<OBOrder> bids = systemManager.getEventBids(event.getId());
            List<OBOrder> asks = systemManager.getEventAsks(event.getId());

            buyOrdersTable.setItems(javafx.collections.FXCollections.observableArrayList(bids));
            sellOrdersTable.setItems(javafx.collections.FXCollections.observableArrayList(asks));

            if (buyPriceCol != null) {
                buyPriceCol.setSortType(TableColumn.SortType.DESCENDING);
                buyOrdersTable.getSortOrder().setAll(buyPriceCol);
            }
            if (sellPriceCol != null) {
                sellPriceCol.setSortType(TableColumn.SortType.ASCENDING);
                sellOrdersTable.getSortOrder().setAll(sellPriceCol);
            }
        } catch (Exception e) {
            System.err.println("Error fetching Order Book data: " + e.getMessage());
        }
    }
}
