package com.example.chat.conversation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    List<Conversation> findByMemberIdsContainingOrderByLastMessageAtDesc(String userId);

    Optional<Conversation> findByDirectPairKey(String directPairKey);

    boolean existsByDirectPairKey(String directPairKey);
}
