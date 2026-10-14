
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
import com.ui.core.ApiClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventController implements IRefreshable {

    // --- Top / Filter UI Elements ---
    @FXML private ComboBox<String> methodFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> commissionFilterCombo;

    // --- Left Panel UI Elements ---
    @FXML private VBox eventsListPanel;
    @FXML private TilePane eventsTileContainer;

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
    private XYChart.Series<Number, Number> priceSeries;       // price line of option 1
    private XYChart.Series<Number, Number> secondPriceSeries; // price line of option 2
    private Event currentSelectedEvent;
    private Button currentlySelectedEventBtn = null;

    private final OkHttpClient httpClient = ApiClient.http();     // shared client = shared session cookie
    private final Gson gson = ApiClient.GSON;
    private volatile boolean pulling = false;                        // don't stack requests if the server is slow
    // Last rendered state per widget group: the 1s pull only touches the UI when something really changed
    private String lastChartEvent = "";
    private String lastChartKey = "";
    private String lastSummaryKey = "";
    private String lastOrderBookKey = "";
    private String lastOrderBookEvent = "";

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

    /** Clears the selected event and everything drawn for it (called when the user changes). */
    public void resetSession() {
        currentSelectedEvent = null;
        currentlySelectedEventBtn = null;
        lastChartEvent = "";
        lastChartKey = "";
        lastSummaryKey = "";
        lastOrderBookKey = "";
        lastOrderBookEvent = "";
        if (eventsTileContainer != null) eventsTileContainer.getChildren().clear();
        if (priceSeries != null) priceSeries.getData().clear();
        if (secondPriceSeries != null) secondPriceSeries.getData().clear();
        if (historyTable != null) historyTable.getItems().clear();
        if (buyOrdersTable != null) buyOrdersTable.getItems().clear();
        if (sellOrdersTable != null) sellOrdersTable.getItems().clear();
        if (optionsSummaryContainer != null) optionsSummaryContainer.getChildren().clear();
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
        priceSeries.setName("Option 1");
        secondPriceSeries = new XYChart.Series<>();
        secondPriceSeries.setName("Option 2");
        if (marketChart != null) {
            marketChart.setAnimated(false);
            marketChart.setLegendVisible(true);
            marketChart.getData().add(priceSeries);
            marketChart.getData().add(secondPriceSeries);
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

        if (pulling) return;
        pulling = true;
        Request request = ApiClient.get(ApiClient.url("/events").build());

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { pulling = false; }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                pulling = false;
                if (viewManager != null && viewManager.handleUnauthorized(response)) return;
                if (!response.isSuccessful()) return;

                Type eventListType = new TypeToken<List<Event>>(){}.getType();
                List<Event> allEvents = gson.fromJson(response.body().string(), eventListType);

                Platform.runLater(() -> {
                    List<Event> filteredEvents = new ArrayList<>();
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
                        filteredEvents.add(event);
                    }

                    int currentIndex = 0;
                    for (Event event : filteredEvents) {
                        String buttonText = event.getName() + " (" + event.getStatus() + ")";

                        if (currentIndex < eventsTileContainer.getChildren().size()) {
                            Button existingBtn = (Button) eventsTileContainer.getChildren().get(currentIndex);
                            existingBtn.setText(buttonText);
                            existingBtn.setOnAction(e -> selectEvent(event, existingBtn));

                            if (currentSelectedEvent != null && currentSelectedEvent.getName().equals(event.getName())) {
                                existingBtn.getStyleClass().remove("action-button");
                                if (!existingBtn.getStyleClass().contains("action-button-selected")) {
                                    existingBtn.getStyleClass().add("action-button-selected");
                                }
                                currentlySelectedEventBtn = existingBtn;
                                selectEvent(event, existingBtn);
                            } else {
                                existingBtn.getStyleClass().remove("action-button-selected");
                                if (!existingBtn.getStyleClass().contains("action-button")) {
                                    existingBtn.getStyleClass().add("action-button");
                                }

                            }
                        } else {
                            Button newBtn = new Button(buttonText);
                            newBtn.getStyleClass().add("action-button");
                            newBtn.setMaxWidth(Double.MAX_VALUE);
                            newBtn.setPrefHeight(60);
                            newBtn.setOnAction(e -> selectEvent(event, newBtn));

                            if (currentSelectedEvent != null && currentSelectedEvent.getName().equals(event.getName())) {
                                newBtn.getStyleClass().remove("action-button");
                                newBtn.getStyleClass().add("action-button-selected");
                                currentlySelectedEventBtn = newBtn;
                                selectEvent(event, newBtn);
                            }

                            eventsTileContainer.getChildren().add(newBtn);
                        }
                        currentIndex++;
                    }

                    while (eventsTileContainer.getChildren().size() > filteredEvents.size()) {
                        eventsTileContainer.getChildren().remove(eventsTileContainer.getChildren().size() - 1);
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
        List<Ticket> history = event.getHistory() != null ? event.getHistory() : new ArrayList<>();
        List<Option> options = event.getOptions();

        // history is append-only: same event + same size = nothing to redraw (keeps selection and avoids flicker)
        String chartKey = event.getName() + "#" + history.size();
        if (chartKey.equals(lastChartKey)) return;
        boolean sameEvent = event.getName().equals(lastChartEvent);
        int selectedHistoryRow = (historyTable != null && sameEvent) ? historyTable.getSelectionModel().getSelectedIndex() : -1;
        lastChartKey = chartKey;
        lastChartEvent = event.getName();

        priceSeries.getData().clear();
        secondPriceSeries.getData().clear();

        if (options == null || options.isEmpty()) {
            if (historyTable != null) historyTable.getItems().clear();
            return;
        }

        priceSeries.setName(options.get(0).getName());
        secondPriceSeries.setName(options.size() > 1 ? options.get(1).getName() : "");
        if (priceYAxis != null) priceYAxis.setLabel("Price ($)");

        if (dealXAxis != null) {
            dealXAxis.setAutoRanging(false);
            dealXAxis.setLowerBound(0);
            dealXAxis.setUpperBound(Math.max(5, history.size() + 1));
        }

        int n = options.size();
        double[] prices = new double[n];                     // current price of every option
        java.util.Arrays.fill(prices, event.getBaseValue() / n);
        addChartPoints(0, prices);

        boolean isLmsr = event.isLmsr();
        double[] q = new double[n];                          // LMSR: shares bought per option
        int b = isLmsr ? Math.max(1, event.getMethod().getLmsr().getB()) : 1;

        for (int i = 0; i < history.size(); i++) {
            Ticket ticket = history.get(i);
            int idx = optionIndexOf(options, ticket.getOption());
            if (idx >= 0) {
                if (isLmsr) {
                    q[idx] += ticket.getAmount();
                    double sumExp = 0.0;
                    for (double shares : q) sumExp += Math.exp(shares / b);
                    for (int j = 0; j < n; j++) prices[j] = Math.exp(q[j] / b) / sumExp;
                } else {
                    prices[idx] = ticket.getPricePaid();     // order book: each option keeps its own last deal price
                }
            }
            addChartPoints(i + 1, prices);
        }

        if (historyTable != null) {
            historyTable.setItems(FXCollections.observableArrayList(history));
            if (selectedHistoryRow >= 0 && selectedHistoryRow < history.size()) {
                historyTable.getSelectionModel().select(selectedHistoryRow);
            }
        }
    }

    private void fetchAndPopulateOptionsSummary(Event event) {
        if (optionsSummaryContainer == null) return;

        Request request = ApiClient.get(ApiClient.url("/marketData")
                .addQueryParameter("action", "summary")
                .addQueryParameter("eventName", event.getName()).build());

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager != null && viewManager.handleUnauthorized(response)) return;
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                SummaryResponse data = gson.fromJson(body, SummaryResponse.class);
                if (data == null) return;

                // the cards also show total shares, which come from the events list, so they are part of the key
                StringBuilder key = new StringBuilder(event.getName()).append('|').append(body);
                for (Option o : event.getOptions()) key.append('|').append(o.getTotalShares());
                String summaryKey = key.toString();

                Platform.runLater(() -> {
                    if (summaryKey.equals(lastSummaryKey)) return;
                    lastSummaryKey = summaryKey;
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
            lastOrderBookKey = "";
            lastOrderBookEvent = "";
            return;
        }

        Request request = ApiClient.get(ApiClient.url("/marketData")
                .addQueryParameter("action", "orderbook")
                .addQueryParameter("eventName", event.getName()).build());

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager != null && viewManager.handleUnauthorized(response))return;
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                OrderBookResponse data = gson.fromJson(body, OrderBookResponse.class);
                if (data == null) return;
                String orderBookKey = event.getName() + "|" + body;

                Platform.runLater(() -> {
                    if (orderBookKey.equals(lastOrderBookKey)) return;
                    boolean sameEvent = event.getName().equals(lastOrderBookEvent);
                    lastOrderBookKey = orderBookKey;
                    lastOrderBookEvent = event.getName();

                    String selectedBid = sameEvent ? orderKey(buyOrdersTable.getSelectionModel().getSelectedItem()) : null;
                    String selectedAsk = sameEvent ? orderKey(sellOrdersTable.getSelectionModel().getSelectedItem()) : null;

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

                    reselectOrder(buyOrdersTable, selectedBid);
                    reselectOrder(sellOrdersTable, selectedAsk);
                });
            }
        });
    }

    private void addChartPoints(int x, double[] prices) {
        priceSeries.getData().add(new XYChart.Data<>(x, prices[0]));
        if (prices.length > 1) secondPriceSeries.getData().add(new XYChart.Data<>(x, prices[1]));
    }

    private static int optionIndexOf(List<Option> options, Option option) {
        if (option == null) return -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).getName().equals(option.getName())) return i;
        }
        return -1;
    }

    private static String orderKey(OBOrder o) {
        if (o == null) return null;
        return o.getUserName() + "@" + o.getPrice() + "x" + o.getAmount() + "#" + o.getSequenceId();
    }

    private static void reselectOrder(TableView<OBOrder> table, String key) {
        if (key == null) return;
        for (OBOrder o : table.getItems()) {
            if (key.equals(orderKey(o))) {
                table.getSelectionModel().select(o);
                return;
            }
        }
    }
}
