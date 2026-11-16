package com.example.chat.message;

import com.example.chat.common.PagedResponse;
import com.example.chat.conversation.Conversation;
import com.example.chat.conversation.ConversationMembership;
import com.example.chat.conversation.ConversationMembershipRepository;
import com.example.chat.conversation.ConversationRepository;
import com.example.chat.conversation.ConversationService;
import com.example.chat.user.User;
import com.example.chat.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMembershipRepository membershipRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        sender = new User("Alice", "alice@example.com", "hash", "avatar.png");
        sender.setId("user-1");

        conversation = new Conversation();
        conversation.setId("conv-1");
        conversation.setMemberIds(new ArrayList<>(List.of("user-1", "user-2")));
    }

    @Test
    @DisplayName("Should persist and broadcast message with idempotency key")
    void testSendMessageSuccess() {
        SendMessageRequest request = new SendMessageRequest("Hello Bob!", "client-uuid-1");

        when(conversationService.getConversationById("conv-1")).thenReturn(conversation);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));
        when(messageRepository.findByConversationIdAndClientMessageId("conv-1", "client-uuid-1"))
                .thenReturn(Optional.empty());

        Message savedMsg = new Message("conv-1", "user-1", "Alice", "avatar.png", "Hello Bob!", MessageType.TEXT, "client-uuid-1");
        savedMsg.setId("msg-101");
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        MessageResponse resp = messageService.sendMessage("conv-1", "user-1", request);

        assertNotNull(resp);
        assertEquals("msg-101", resp.getId());
        assertEquals("Hello Bob!", resp.getContent());

        verify(messageRepository, times(1)).save(any(Message.class));
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/conversations/conv-1"), any(MessageResponse.class));
    }

    @Test
    @DisplayName("Should return existing message if clientMessageId already processed")
    void testSendMessageIdempotency() {
        SendMessageRequest request = new SendMessageRequest("Hello Bob!", "client-uuid-1");

        Message existing = new Message("conv-1", "user-1", "Alice", "avatar.png", "Hello Bob!", MessageType.TEXT, "client-uuid-1");
        existing.setId("msg-existing");

        when(conversationService.getConversationById("conv-1")).thenReturn(conversation);
        when(messageRepository.findByConversationIdAndClientMessageId("conv-1", "client-uuid-1"))
                .thenReturn(Optional.of(existing));

        MessageResponse resp = messageService.sendMessage("conv-1", "user-1", request);

        assertEquals("msg-existing", resp.getId());
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    @DisplayName("Should paginate messages in chronological order")
    void testGetMessagesChronological() {
        Message m1 = new Message("conv-1", "user-1", "Alice", null, "First", MessageType.TEXT, null);
        m1.setId("1");
        m1.setCreatedAt(Instant.parse("2026-11-02T10:00:00Z"));

        Message m2 = new Message("conv-1", "user-2", "Bob", null, "Second", MessageType.TEXT, null);
        m2.setId("2");
        m2.setCreatedAt(Instant.parse("2026-11-02T10:01:00Z"));

        // Descending order returned by MongoDB
        List<Message> mongoDesc = List.of(m2, m1);
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv-1"), any(Pageable.class)))
                .thenReturn(mongoDesc);

        PagedResponse<MessageResponse> result = messageService.getMessages("conv-1", "user-1", null, 30);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        // Must be in chronological order: First then Second
        assertEquals("First", result.getItems().get(0).getContent());
        assertEquals("Second", result.getItems().get(1).getContent());
    }
}
