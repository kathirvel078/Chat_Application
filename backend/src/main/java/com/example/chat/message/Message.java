package com.example.chat.message;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "messages")
@CompoundIndexes({
        @CompoundIndex(name = "conv_created_idx", def = "{ 'conversationId': 1, 'createdAt': -1 }"),
        @CompoundIndex(name = "conv_client_msg_idx", def = "{ 'conversationId': 1, 'clientMessageId': 1 }", sparse = true)
})
public class Message {

    @Id
    private String id;

    private String conversationId;

    private String senderId;

    private String senderName;

    private String senderAvatarUrl;

    private String content;

    private MessageType type = MessageType.TEXT;

    private String clientMessageId;

    private MessageStatus status = MessageStatus.SENT;

    private Map<String, Instant> readBy = new HashMap<>();

    @CreatedDate
    private Instant createdAt = Instant.now();

    private Instant editedAt;

    private Instant deletedAt;

    public Message() {}

    public Message(String conversationId, String senderId, String senderName, String senderAvatarUrl,
                   String content, MessageType type, String clientMessageId) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatarUrl = senderAvatarUrl;
        this.content = content;
        this.type = type != null ? type : MessageType.TEXT;
        this.clientMessageId = clientMessageId;
        this.status = MessageStatus.SENT;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Map<String, Instant> getReadBy() {
        return readBy;
    }

    public void setReadBy(Map<String, Instant> readBy) {
        this.readBy = readBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Instant editedAt) {
        this.editedAt = editedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
