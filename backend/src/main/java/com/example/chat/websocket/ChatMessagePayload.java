package com.example.chat.websocket;

public class ChatMessagePayload {
    private String conversationId;
    private String content;
    private String clientMessageId;

    public ChatMessagePayload() {}

    public ChatMessagePayload(String conversationId, String content, String clientMessageId) {
        this.conversationId = conversationId;
        this.content = content;
        this.clientMessageId = clientMessageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }
}
