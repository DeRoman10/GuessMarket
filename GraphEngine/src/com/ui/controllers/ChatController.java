package com.ui.controllers;

import com.data.users.dto.ChatMessage;
import com.ui.core.ApiClient;
import com.ui.core.IRefreshable;
import com.ui.core.ViewManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatController implements IRefreshable {

    @FXML private ListView<ChatMessage> chatListView;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;

    private ViewManager viewManager;
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private int nextIndex = 0;

    private static class ChatResponse {
        List<ChatMessage> messages;
        int next;
    }

    public void setManagers(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @FXML
    public void initialize() {
        chatListView.setItems(messages);

        chatListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ChatMessage item, boolean empty) {
                super.updateItem(item, empty);

                setStyle("-fx-background-color: transparent;");

                if (empty || item == null) {
                    setText(null);
                } else {
                    String time = TIME_FMT.format(Instant.ofEpochMilli(item.getTimestamp()));
                    setText(String.format("[%s] %s: %s", time, item.getUser(), item.getMessage()));

                    if (!getStyleClass().contains("sub-text")) {
                        getStyleClass().add("sub-text");
                    }
                }
            }
        });

        messageInput.setOnAction(event -> handleSendMessage());
    }

    @FXML
    private void handleSendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || viewManager == null || !viewManager.isLoggedIn()) return;

        Request request = ApiClient.post(ApiClient.url("/chat").addQueryParameter("message", text).build());
        ApiClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager.handleUnauthorized(response)) return;

                boolean ok = response.isSuccessful();
                String body = ApiClient.message(response.body().string());
                Platform.runLater(() -> {
                    if (ok) {
                        messageInput.clear();
                        refreshData();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING, body);
                        alert.setHeaderText(null);
                        alert.showAndWait();
                    }
                });
            }
        });
    }

    @Override
    public void refreshData() {
        if (viewManager == null || !viewManager.isLoggedIn()) return;

        final int requestedFrom = nextIndex;
        Request request = ApiClient.get(ApiClient.url("/chat")
                .addQueryParameter("since", String.valueOf(requestedFrom)).build());

        ApiClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (viewManager.handleUnauthorized(response)) return;
                if (!response.isSuccessful()) {
                    response.close();
                    return;
                }

                ChatResponse data = ApiClient.GSON.fromJson(response.body().string(), ChatResponse.class);
                Platform.runLater(() -> {
                    if (data.next == 0 && nextIndex > 0 && (data.messages == null || data.messages.isEmpty())) {
                        messages.clear();
                        nextIndex = 0;
                        return;
                    }

                    if (requestedFrom != nextIndex || data.messages == null || data.messages.isEmpty()) return;

                    messages.addAll(data.messages);
                    nextIndex = data.next;
                    chatListView.scrollTo(messages.size() - 1);
                });
            }
        });
    }

    public void reset() {
        messages.clear();
        nextIndex = 0;
    }
}