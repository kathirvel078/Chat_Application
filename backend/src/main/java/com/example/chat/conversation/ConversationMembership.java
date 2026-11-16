package com.example.chat.conversation;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "conversation_memberships")
@CompoundIndexes({
        @CompoundIndex(name = "conv_user_idx", def = "{ 'conversationId': 1, 'userId': 1 }", unique = true),
        @CompoundIndex(name = "user_conv_idx", def = "{ 'userId': 1, 'conversationId': 1 }")
})
public class ConversationMembership {

    @Id
    private String id;

    private String conversationId;

    private String userId;

    private String lastReadMessageId;

    private Instant lastReadAt = Instant.now();

    private int unreadCount = 0;

    public ConversationMembership() {}

    public ConversationMembership(String conversationId, String userId) {
        this.id = conversationId + "_" + userId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.lastReadAt = Instant.now();
        this.unreadCount = 0;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(String lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(Instant lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = Math.max(0, unreadCount);
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }
}
