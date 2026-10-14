package com.data.users.dto;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String user;
    private String message;
    private long timestamp;

    public ChatMessage() {} // Required for JSON/Serialization

    public ChatMessage(String user, String message, long timestamp) {
        this.user = user;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}