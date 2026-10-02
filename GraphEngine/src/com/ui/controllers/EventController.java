package com.ui.controllers;

import com.data.events.entities.Event;
import com.data.events.entities.OBOrder;
import com.data.events.entities.Option;
import com.data.events.entities.Ticket;
import com.data.events.enums.EventStatus;
import com.ui.core.IRefreshable;
import com.ui.core.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class EventController implements IRefreshable {

    // --- Top / Filter UI Elements ---
    @FXML private ComboBox<String> methodFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> commissionFilterCombo;

    // --- Left Panel UI Elements ---
    @FXML private VBox eventsListPanel;
    @FXML private VBox eventsTileContainer;

    // --- Center/Right Panel UI Elements ---
    @FXML private HBox orderBookTablesBox;
    @FXML private TableView<OBOrder> buyOrdersTable;
    @FXML private TableColumn<OBOrder, String> buyPriceCol;
    @FXML private TableColumn<OBOrder, Integer> buyAmountCol;
    @FXML private TableColumn<OBOrder, String> buyOptionCol;

    @FXML private TableView<OBOrder> sellOrdersTable;
    @FXML private TableColumn<OBOrder, String> sellPriceCol;
    @FXML private TableColumn<OBOrder, Integer> sellAmountCol;
    @FXML private TableColumn<OBOrder, String> sellOptionCol;

    @FXML private TableView<Ticket> historyTable;
    @FXML private TableColumn<Ticket, String> historyOptionCol;
    @FXML private TableColumn<Ticket, Integer> historyAmountCol;
    @FXML private TableColumn<Ticket, String> historyPriceCol;

    @FXML private LineChart<Number, Number> marketChart;
    @FXML private NumberAxis dealXAxis;
    @FXML private NumberAxis priceYAxis;
    @FXML private HBox optionsSummaryContainer;

    private ViewManager viewManager;
    private XYChart.Series<Number, Number> priceSeries;
    private Event currentSelectedEvent;
    private Button currentlySelectedEventBtn = null;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String BASE_URL = "http://localhost:8080/GuessMarketServer_Web_exploded";

    // Internal DTOs for parsing JSON from our new Servlet
    private static class OrderBookResponse {
        List<OBOrder> bids;
        List<OBOrder> asks;
    }

    private static class SummaryResponse {
        List<Double> prices;
        List<Map<String, Double>> obStats;
        String participants;
    }

    public void setManagers(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @Override
    public void refreshData() {
        handleRefresh();
    }

    @FXML
    public void initialize() {
        // 1. Setup Order Book Columns
        if (buyPriceCol != null) {
            buyPriceCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f$", cell.getValue().getPrice())));
            buyAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
            buyOptionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOption().getName()));
        }
        if (sellPriceCol != null) {
            sellPriceCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f$", cell.getValue().getPrice())));
            sellAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
            sellOptionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOption().getName()));
        }

        // 2. Setup History Columns
        if (historyAmountCol != null) {
            historyAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        }
        if (historyPriceCol != null) {
            historyPriceCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f$", cell.getValue().getPricePaid())));
        }
        if (historyOptionCol != null) {
            historyOptionCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOption().getName()));
        }

        // 3. Setup Chart
        if (dealXAxis != null) {
            dealXAxis.setForceZeroInRange(true);
            dealXAxis.setAutoRanging(true);
            dealXAxis.setTickUnit(1);
            dealXAxis.setMinorTickVisible(false);
            dealXAxis.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    return object.intValue() == object.doubleValue() ? String.valueOf(object.intValue()) : "";
                }
                @Override
                public Number fromString(String string) { return null; }
            });
        }

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Deal Price History");
        if (marketChart != null) {
            marketChart.setAnimated(false);
            marketChart.getData().add(priceSeries);
        }

        // 4. Setup Filters
        if (methodFilterCombo != null) {
            methodFilterCombo.setPrefWidth(240);
            methodFilterCombo.getItems().addAll("All Methods", "LMSR", "Order Book");
            methodFilterCombo.setValue("All Methods");
            methodFilterCombo.setOnAction(e -> handleRefresh());
        }

        if (statusFilterCombo != null) {
            statusFilterCombo.setPrefWidth(240);
            statusFilterCombo.getItems().clear();
            statusFilterCombo.getItems().add("All Statuses");
            for (EventStatus status : EventStatus.values()) {
                statusFilterCombo.getItems().add(status.name());
            }
            statusFilterCombo.setValue("All Statuses");
            statusFilterCombo.setOnAction(e -> handleRefresh());
        }

        if (commissionFilterCombo != null) {
            commissionFilterCombo.setPrefWidth(240);
            commissionFilterCombo.getItems().clear();
            commissionFilterCombo.getItems().addAll("All Commissions", "On Purchase", "On Close");
            commissionFilterCombo.setValue("All Commissions");
            commissionFilterCombo.setOnAction(e -> handleRefresh());
        }
    }

    @FXML
    public void handleRefresh() {
        populateEventTiles();
    }

    private void populateEventTiles() {
        if (eventsTileContainer == null) return;

        Request request = new Request.Builder().url(BASE_URL + "/events").get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager != null && viewManager.handleUnauthorized(response.code())) return;
                if (!response.isSuccessful()) return;

                Type eventListType = new TypeToken<List<Event>>(){}.getType();
                List<Event> allEvents = gson.fromJson(response.body().string(), eventListType);

                Platform.runLater(() -> {
                    eventsTileContainer.getChildren().clear();

                    String selectedMethod = methodFilterCombo != null ? methodFilterCombo.getValue() : "All Methods";
                    String selectedStatus = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Statuses";
                    String selectedCommission = commissionFilterCombo != null ? commissionFilterCombo.getValue() : "All Commissions";

                    for (Event event : allEvents) {
                        boolean isLmsr = event.getMethod() != null && event.getMethod().getLmsr() != null;
                        if ("LMSR".equals(selectedMethod) && !isLmsr) continue;
                        if ("Order Book".equals(selectedMethod) && isLmsr) continue;

                        if (!"All Statuses".equals(selectedStatus) && !event.getStatus().name().equals(selectedStatus)) continue;

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

                            // Re-fetch details for the selected event automatically
                            selectEvent(event, currentlySelectedEventBtn);
                        }

                        eventTile.setOnAction(e -> selectEvent(event, eventTile));
                        eventsTileContainer.getChildren().add(eventTile);
                    }
                });
            }
        });
    }

    private void selectEvent(Event event, Button clickedBtn) {
        this.currentSelectedEvent = event;

        if (currentlySelectedEventBtn != null) {
            currentlySelectedEventBtn.getStyleClass().remove("action-button-selected");
            currentlySelectedEventBtn.getStyleClass().add("action-button");
        }
        if (clickedBtn != null) {
            clickedBtn.getStyleClass().remove("action-button");
            clickedBtn.getStyleClass().add("action-button-selected");
            currentlySelectedEventBtn = clickedBtn;
        }

        boolean isLmsr = event.getMethod() != null && event.getMethod().getLmsr() != null;
        if (orderBookTablesBox != null) {
            orderBookTablesBox.setVisible(!isLmsr);
            orderBookTablesBox.setManaged(!isLmsr);
        }

        populateChartAndHistory(event);
        fetchAndPopulateOptionsSummary(event);
        fetchAndPopulateOrderBook(event);
    }

    private void populateChartAndHistory(Event event) {
        priceSeries.getData().clear();
        List<Ticket> history = event.getHistory();
        List<Option> options = event.getOptions();

        if (options == null || options.isEmpty()) {
            if (historyTable != null) historyTable.getItems().clear();
            return;
        }

        Option firstOption = options.get(0);
        if (priceYAxis != null) priceYAxis.setLabel("Price for " + firstOption.getName());

        if (dealXAxis != null) {
            dealXAxis.setAutoRanging(false);
            dealXAxis.setLowerBound(0);
            dealXAxis.setUpperBound(Math.max(5, history.size() + 1));
        }

        double initialPrice = 1.0 / options.size();
        priceSeries.getData().add(new XYChart.Data<>(0, initialPrice));

        if (history != null && !history.isEmpty()) {
            boolean isLmsr = event.getMethod() != null && event.getMethod().getLmsr() != null;
            if (isLmsr) {
                int b = event.getMethod().getLmsr().getB();
                if (b <= 0) b = 100;
                double[] q = new double[options.size()];

                for (int i = 0; i < history.size(); i++) {
                    Ticket ticket = history.get(i);
                    for (int j = 0; j < options.size(); j++) {
                        if (options.get(j).getName().equals(ticket.getOption().getName())) {
                            q[j] += ticket.getAmount();
                            break;
                        }
                    }

                    double sumExp = 0.0;
                    for (double shares : q) sumExp += Math.exp(shares / b);
                    double firstOptionPrice = Math.exp(q[0] / b) / sumExp;
                    priceSeries.getData().add(new XYChart.Data<>(i + 1, firstOptionPrice));
                }
            } else {
                for (int i = 0; i < history.size(); i++) {
                    Ticket ticket = history.get(i);
                    double pricePerShare = ticket.getAmount() > 0 ? ticket.getPricePaid() / ticket.getAmount() : 0.0;
                    double firstOptionPrice = ticket.getOption().getName().equals(firstOption.getName()) ? pricePerShare : 1.0 - pricePerShare;
                    priceSeries.getData().add(new XYChart.Data<>(i + 1, firstOptionPrice));
                }
            }
        }

        if (historyTable != null) {
            historyTable.setItems(FXCollections.observableArrayList(history));
        }
    }

    private void fetchAndPopulateOptionsSummary(Event event) {
        if (optionsSummaryContainer == null) return;

        Request request = new Request.Builder()
                .url(BASE_URL + "/marketData?action=summary&eventName=" + event.getName())
                .get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                SummaryResponse data = gson.fromJson(response.body().string(), SummaryResponse.class);

                Platform.runLater(() -> {
                    optionsSummaryContainer.getChildren().clear();
                    boolean isOB = event.isOrderBook();

                    for (int i = 0; i < event.getOptions().size(); i++) {
                        Option opt = event.getOptions().get(i);
                        VBox card = new VBox(2);
                        HBox.setHgrow(card, Priority.ALWAYS);
                        card.setMaxWidth(Double.MAX_VALUE);
                        card.getStyleClass().add("side-panel");
                        card.setStyle("-fx-padding: 10px; -fx-border-color: #888888; -fx-border-width: 1px; -fx-border-radius: 5px;");

                        Label nameLabel = new Label(opt.getName());
                        nameLabel.getStyleClass().add("section-title");
                        nameLabel.setStyle("-fx-font-size: 13px;");
                        card.getChildren().add(nameLabel);

                        if (isOB && data.obStats != null && i < data.obStats.size()) {
                            Map<String, Double> stats = data.obStats.get(i);
                            Label lastLabel = new Label(String.format("LAST: $%.2f", stats.getOrDefault("LAST", 0.0)));
                            lastLabel.getStyleClass().add("buy-label");
                            Label bidAskLabel = new Label(String.format("BID: $%.2f | ASK: $%.2f", stats.getOrDefault("BID", 0.0), stats.getOrDefault("ASK", 0.0)));
                            bidAskLabel.getStyleClass().add("sub-text");
                            Label midSpreadLabel = new Label(String.format("MID: $%.2f | SPREAD: $%.2f", stats.getOrDefault("MID", 0.0), stats.getOrDefault("SPREAD", 0.0)));
                            midSpreadLabel.getStyleClass().add("sub-text");
                            card.getChildren().addAll(lastLabel, bidAskLabel, midSpreadLabel);
                        } else if (data.prices != null && i < data.prices.size()) {
                            Label priceLabel = new Label(String.format("Price: $%.2f", data.prices.get(i)));
                            priceLabel.getStyleClass().add("buy-label");
                            card.getChildren().add(priceLabel);
                        }

                        Label sharesLabel = new Label(String.format("Total Shares: %.2f", opt.getTotalShares()));
                        sharesLabel.getStyleClass().add("sub-text");
                        card.getChildren().add(sharesLabel);
                        optionsSummaryContainer.getChildren().add(card);
                    }

                    if (isOB && data.participants != null) {
                        VBox partCard = new VBox(2);
                        HBox.setHgrow(partCard, Priority.ALWAYS);
                        partCard.setMaxWidth(Double.MAX_VALUE);
                        partCard.getStyleClass().add("side-panel");
                        partCard.setStyle("-fx-padding: 10px; -fx-border-color: #888888; -fx-border-width: 1px; -fx-border-radius: 5px;");

                        Label titleLabel = new Label("Participants Overview");
                        titleLabel.getStyleClass().add("section-title");
                        titleLabel.setStyle("-fx-font-size: 13px;");

                        Label contentLabel = new Label(data.participants);
                        contentLabel.getStyleClass().add("sub-text");
                        contentLabel.setWrapText(true);

                        ScrollPane scrollPane = new ScrollPane(contentLabel);
                        scrollPane.setFitToWidth(true);
                        scrollPane.getStyleClass().add("dark-scroll");
                        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
                        scrollPane.setPrefHeight(90);

                        partCard.getChildren().addAll(titleLabel, scrollPane);
                        optionsSummaryContainer.getChildren().add(partCard);
                    }
                });
            }
        });
    }

    private void fetchAndPopulateOrderBook(Event event) {
        if (buyOrdersTable == null || sellOrdersTable == null) return;
        if (!event.isOrderBook()) {
            buyOrdersTable.getItems().clear();
            sellOrdersTable.getItems().clear();
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/marketData?action=orderbook&eventName=" + event.getName())
                .get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                OrderBookResponse data = gson.fromJson(response.body().string(), OrderBookResponse.class);

                Platform.runLater(() -> {
                    if (data.bids != null) buyOrdersTable.setItems(FXCollections.observableArrayList(data.bids));
                    if (data.asks != null) sellOrdersTable.setItems(FXCollections.observableArrayList(data.asks));

                    if (buyPriceCol != null) {
                        buyPriceCol.setSortType(TableColumn.SortType.DESCENDING);
                        buyOrdersTable.getSortOrder().setAll(buyPriceCol);
                    }
                    if (sellPriceCol != null) {
                        sellPriceCol.setSortType(TableColumn.SortType.ASCENDING);
                        sellOrdersTable.getSortOrder().setAll(sellPriceCol);
                    }
                });
            }
        });
    }
}