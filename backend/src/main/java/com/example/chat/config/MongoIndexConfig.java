package com.example.chat.config;

import com.example.chat.auth.RefreshToken;
import com.example.chat.conversation.Conversation;
import com.example.chat.conversation.ConversationMembership;
import com.example.chat.message.Message;
import com.example.chat.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);
    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndicesAfterStartup() {
        try {
            // User email unique index
            mongoTemplate.indexOps(User.class).ensureIndex(
                    new Index().on("email", Sort.Direction.ASC).unique()
            );

            // RefreshToken indexes
            mongoTemplate.indexOps(RefreshToken.class).ensureIndex(
                    new Index().on("tokenHash", Sort.Direction.ASC).unique()
            );
            mongoTemplate.indexOps(RefreshToken.class).ensureIndex(
                    new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO)
            );

            // Conversation indexes
            mongoTemplate.indexOps(Conversation.class).ensureIndex(
                    new Index().on("directPairKey", Sort.Direction.ASC).unique().sparse()
            );
            mongoTemplate.indexOps(Conversation.class).ensureIndex(
                    new Index().on("memberIds", Sort.Direction.ASC).on("lastMessageAt", Sort.Direction.DESC)
            );

            // ConversationMembership compound unique index
            mongoTemplate.indexOps(ConversationMembership.class).ensureIndex(
                    new Index().on("conversationId", Sort.Direction.ASC).on("userId", Sort.Direction.ASC).unique()
            );

            // Message compound index for cursor pagination
            mongoTemplate.indexOps(Message.class).ensureIndex(
                    new Index().on("conversationId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            );

            log.info("MongoDB indexes verified successfully.");
        } catch (Exception ex) {
            log.warn("Non-fatal MongoDB index creation notice: {}", ex.getMessage());
        }
    }
}
