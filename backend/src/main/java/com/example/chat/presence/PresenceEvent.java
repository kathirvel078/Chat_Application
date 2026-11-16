package com.example.chat.presence;

import java.time.Instant;

public class PresenceEvent {
    private String userId;
    private String status; // ONLINE, OFFLINE
    private Instant timestamp;

    public PresenceEvent() {}

    public PresenceEvent(String userId, String status, Instant timestamp) {
        this.userId = userId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
