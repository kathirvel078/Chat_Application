package com.example.chat.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
            String conversationId, Instant createdAt, Pageable pageable);

    List<Message> findByConversationIdOrderByCreatedAtDesc(
            String conversationId, Pageable pageable);

    Optional<Message> findByConversationIdAndClientMessageId(String conversationId, String clientMessageId);

    int countByConversationIdAndCreatedAtGreaterThan(String conversationId, Instant createdAt);

    void deleteByConversationId(String conversationId);
}
