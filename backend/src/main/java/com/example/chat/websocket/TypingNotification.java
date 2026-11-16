package com.example.chat.websocket;

import java.time.Instant;

public class TypingNotification {
    private String conversationId;
    private String userId;
    private String displayName;
    private boolean typing;
    private Instant timestamp;

    public TypingNotification() {}

    public TypingNotification(String conversationId, String userId, String displayName, boolean typing) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.displayName = displayName;
        this.typing = typing;
        this.timestamp = Instant.now();
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
