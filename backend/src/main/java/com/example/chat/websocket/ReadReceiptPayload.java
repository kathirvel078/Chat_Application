package com.example.chat.websocket;

public class ReadReceiptPayload {
    private String conversationId;
    private String messageId;

    public ReadReceiptPayload() {}

    public ReadReceiptPayload(String conversationId, String messageId) {
        this.conversationId = conversationId;
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
