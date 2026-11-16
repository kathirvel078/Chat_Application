package com.example.chat.conversation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMembershipRepository extends MongoRepository<ConversationMembership, String> {

    Optional<ConversationMembership> findByConversationIdAndUserId(String conversationId, String userId);

    List<ConversationMembership> findByConversationId(String conversationId);

    List<ConversationMembership> findByUserId(String userId);

    void deleteByConversationIdAndUserId(String conversationId, String userId);

    void deleteByConversationId(String conversationId);
}
