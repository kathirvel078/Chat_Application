package com.example.chat.conversation;

import com.example.chat.exception.BadRequestException;
import com.example.chat.message.Message;
import com.example.chat.message.MessageRepository;
import com.example.chat.presence.PresenceService;
import com.example.chat.user.User;
import com.example.chat.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMembershipRepository membershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private ConversationService conversationService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("Alice", "alice@example.com", "hash", "avatar1.png");
        user1.setId("user-1");

        user2 = new User("Bob", "bob@example.com", "hash", "avatar2.png");
        user2.setId("user-2");
    }

    @Test
    @DisplayName("Should create direct conversation if none exists")
    void testCreateDirectConversation() {
        String pairKey = Conversation.buildDirectPairKey("user-1", "user-2");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(user2));
        when(conversationRepository.findByDirectPairKey(pairKey)).thenReturn(Optional.empty());

        Conversation created = new Conversation();
        created.setId("conv-123");
        created.setType(ConversationType.DIRECT);
        created.setMemberIds(List.of("user-1", "user-2"));
        created.setDirectPairKey(pairKey);

        when(conversationRepository.save(any(Conversation.class))).thenReturn(created);

        ConversationResponse resp = conversationService.getOrCreateDirectConversation("user-1", "user-2");

        assertNotNull(resp);
        assertEquals(ConversationType.DIRECT, resp.getType());
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(membershipRepository, times(2)).save(any(ConversationMembership.class));
    }

    @Test
    @DisplayName("Should prevent creating direct conversation with self")
    void testPreventDirectChatWithSelf() {
        assertThrows(BadRequestException.class, () ->
                conversationService.getOrCreateDirectConversation("user-1", "user-1")
        );
    }

    @Test
    @DisplayName("Should return existing conversation without creating duplicate")
    void testPreventDuplicateDirectConversation() {
        String pairKey = Conversation.buildDirectPairKey("user-1", "user-2");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(user2));

        Conversation existing = new Conversation();
        existing.setId("conv-existing");
        existing.setType(ConversationType.DIRECT);
        existing.setMemberIds(List.of("user-1", "user-2"));
        existing.setDirectPairKey(pairKey);

        when(conversationRepository.findByDirectPairKey(pairKey)).thenReturn(Optional.of(existing));
        when(conversationRepository.findById("conv-existing")).thenReturn(Optional.of(existing));
        when(userRepository.findByIdIn(anyList())).thenReturn(List.of(user1, user2));

        ConversationResponse resp = conversationService.getOrCreateDirectConversation("user-1", "user-2");

        assertNotNull(resp);
        assertEquals("conv-existing", resp.getId());
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Should create group conversation and publish system message")
    void testCreateGroupConversation() {
        CreateGroupConversationRequest request = new CreateGroupConversationRequest(
                "Engineering Team", List.of("user-2"), null);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(userRepository.findByIdIn(anyList())).thenReturn(List.of(user1, user2));

        Conversation group = new Conversation();
        group.setId("group-100");
        group.setType(ConversationType.GROUP);
        group.setName("Engineering Team");
        group.setMemberIds(new ArrayList<>(List.of("user-1", "user-2")));

        when(conversationRepository.save(any(Conversation.class))).thenReturn(group);

        Message sysMsg = new Message();
        sysMsg.setId("sys-msg-1");
        sysMsg.setContent("Alice created group \"Engineering Team\"");
        when(messageRepository.save(any(Message.class))).thenReturn(sysMsg);

        ConversationResponse resp = conversationService.createGroupConversation("user-1", request);

        assertNotNull(resp);
        assertEquals("Engineering Team", resp.getName());
        verify(messageRepository, times(1)).save(any(Message.class));
    }
}
