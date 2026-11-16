package com.example.chat.websocket;

public class TypingEventPayload {
    private String conversationId;
    private boolean typing;

    public TypingEventPayload() {}

    public TypingEventPayload(String conversationId, boolean typing) {
        this.conversationId = conversationId;
        this.typing = typing;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
