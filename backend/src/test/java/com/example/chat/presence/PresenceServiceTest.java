package com.example.chat.presence;

import com.example.chat.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisMessageListenerContainer listenerContainer;

    private RedisPresenceServiceImpl presenceService;
    private ChannelTopic topic;

    @BeforeEach
    void setUp() {
        topic = new ChannelTopic("chat:presence");
        presenceService = new RedisPresenceServiceImpl(
                redisTemplate, messagingTemplate, userRepository, listenerContainer, topic);
    }

    @Test
    @DisplayName("Should track multi-session presence transitions correctly")
    void testMultiSessionPresenceTracking() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String userId = "user-alice";
        String conn1 = "session-tab-1";
        String conn2 = "session-tab-2";

        // Tab 1 connects (was previously offline)
        when(setOperations.size("presence:user:" + userId)).thenReturn(0L);
        presenceService.markUserOnline(userId, conn1);

        verify(setOperations).add("presence:user:" + userId, conn1);
        verify(messagingTemplate).convertAndSend(eq("/topic/presence"), any(PresenceEvent.class));

        // Tab 2 connects (already online with 1 active session)
        reset(messagingTemplate);
        when(setOperations.size("presence:user:" + userId)).thenReturn(1L);
        presenceService.markUserOnline(userId, conn2);

        verify(setOperations).add("presence:user:" + userId, conn2);
        // Should NOT broadcast ONLINE again because already online
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/presence"), any(PresenceEvent.class));

        // Tab 1 disconnects (Tab 2 remains active)
        when(setOperations.size("presence:user:" + userId)).thenReturn(1L);
        presenceService.markUserOffline(userId, conn1);

        // Should NOT broadcast OFFLINE because Tab 2 is still active
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/presence"), any(PresenceEvent.class));

        // Tab 2 disconnects (no sessions left)
        when(setOperations.size("presence:user:" + userId)).thenReturn(0L);
        presenceService.markUserOffline(userId, conn2);

        // Now must broadcast OFFLINE
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/presence"), any(PresenceEvent.class));
    }
}
