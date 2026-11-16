package com.example.chat.conversation;

import jakarta.validation.constraints.NotBlank;

public class CreateDirectConversationRequest {

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;

    public CreateDirectConversationRequest() {}

    public CreateDirectConversationRequest(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
}
