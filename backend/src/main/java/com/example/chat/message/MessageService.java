package com.example.chat.message;

import com.example.chat.common.PagedResponse;
import com.example.chat.conversation.Conversation;
import com.example.chat.conversation.ConversationMembership;
import com.example.chat.conversation.ConversationMembershipRepository;
import com.example.chat.conversation.ConversationRepository;
import com.example.chat.conversation.ConversationService;
import com.example.chat.conversation.LastMessageDto;
import com.example.chat.exception.BadRequestException;
import com.example.chat.exception.ResourceNotFoundException;
import com.example.chat.user.User;
import com.example.chat.user.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMembershipRepository membershipRepository;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          ConversationMembershipRepository membershipRepository,
                          ConversationService conversationService,
                          UserRepository userRepository,
                          @Lazy SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.membershipRepository = membershipRepository;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public PagedResponse<MessageResponse> getMessages(String conversationId, String currentUserId,
                                                     Instant beforeCursor, int limit) {
        conversationService.verifyMembership(conversationId, currentUserId);

        int queryLimit = Math.min(Math.max(limit, 1), 100);
        PageRequest pageRequest = PageRequest.of(0, queryLimit);

        List<Message> messagesDesc;
        if (beforeCursor != null) {
            messagesDesc = messageRepository.findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
                    conversationId, beforeCursor, pageRequest);
        } else {
            messagesDesc = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                    conversationId, pageRequest);
        }

        boolean hasMore = messagesDesc.size() == queryLimit;
        String nextCursor = null;
        if (!messagesDesc.isEmpty() && hasMore) {
            nextCursor = messagesDesc.get(messagesDesc.size() - 1).getCreatedAt().toString();
        }

        // Reverse to return in chronological ascending order
        List<MessageResponse> chronological = new ArrayList<>(messagesDesc.size());
        for (int i = messagesDesc.size() - 1; i >= 0; i--) {
            chronological.add(MessageResponse.fromMessage(messagesDesc.get(i)));
        }

        return new PagedResponse<>(chronological, nextCursor, hasMore, queryLimit);
    }

    @Transactional
    public MessageResponse sendMessage(String conversationId, String senderId, SendMessageRequest request) {
        Conversation conversation = conversationService.getConversationById(conversationId);
        conversationService.verifyMembership(conversation, senderId);

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty");
        }

        // Idempotency check with clientMessageId
        if (request.getClientMessageId() != null && !request.getClientMessageId().isBlank()) {
            Optional<Message> existing = messageRepository.findByConversationIdAndClientMessageId(
                    conversationId, request.getClientMessageId());
            if (existing.isPresent()) {
                return MessageResponse.fromMessage(existing.get());
            }
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        Message message = new Message(
                conversationId,
                senderId,
                sender.getDisplayName(),
                sender.getAvatarUrl(),
                request.getContent().trim(),
                MessageType.TEXT,
                request.getClientMessageId()
        );
        message.getReadBy().put(senderId, Instant.now());
        message.setStatus(MessageStatus.SENT);

        Message savedMessage = messageRepository.save(message);

        // Update conversation lastMessage
        LastMessageDto lastMessageDto = new LastMessageDto(
                savedMessage.getId(),
                senderId,
                sender.getDisplayName(),
                savedMessage.getContent(),
                savedMessage.getType().name(),
                savedMessage.getCreatedAt()
        );
        conversation.setLastMessage(lastMessageDto);
        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // Update unread count for other members
        for (String memberId : conversation.getMemberIds()) {
            if (!memberId.equals(senderId)) {
                membershipRepository.findByConversationIdAndUserId(conversationId, memberId)
                        .ifPresent(m -> {
                            m.incrementUnreadCount();
                            membershipRepository.save(m);
                        });
            } else {
                membershipRepository.findByConversationIdAndUserId(conversationId, senderId)
                        .ifPresent(m -> {
                            m.setLastReadMessageId(savedMessage.getId());
                            m.setLastReadAt(savedMessage.getCreatedAt());
                            membershipRepository.save(m);
                        });
            }
        }

        MessageResponse response = MessageResponse.fromMessage(savedMessage);

        // Broadcast to WebSocket topic
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
            // Also notify user queues for inbox preview updates
            for (String memberId : conversation.getMemberIds()) {
                messagingTemplate.convertAndSendToUser(memberId, "/queue/messages", response);
            }
        }

        return response;
    }

    @Transactional
    public void markConversationAsRead(String conversationId, String currentUserId, String messageId) {
        conversationService.verifyMembership(conversationId, currentUserId);

        membershipRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .ifPresent(membership -> {
                    membership.setUnreadCount(0);
                    membership.setLastReadAt(Instant.now());
                    if (messageId != null && !messageId.isBlank()) {
                        membership.setLastReadMessageId(messageId);
                    }
                    membershipRepository.save(membership);
                });

        // Broadcast read receipt to conversation
        Map<String, Object> receipt = Map.of(
                "conversationId", conversationId,
                "userId", currentUserId,
                "messageId", messageId != null ? messageId : "",
                "readAt", Instant.now().toString()
        );

        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/receipts", receipt);
        }
    }
}
