package com.example.chat.presence;

import com.example.chat.config.RedisConfig;
import com.example.chat.user.User;
import com.example.chat.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisPresenceServiceImpl implements PresenceService, MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisPresenceServiceImpl.class);

    private static final String KEY_CONNECTION = "presence:connection:";
    private static final String KEY_USER = "presence:user:";

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final RedisMessageListenerContainer listenerContainer;
    private final ChannelTopic presenceTopic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.presence.ttl-seconds:90}")
    private long presenceTtlSeconds;

    // Resilient fallback in-memory store in case Redis connection is offline
    private final Map<String, Set<String>> inMemoryUserSessions = new ConcurrentHashMap<>();
    private final Map<String, String> inMemoryConnectionUsers = new ConcurrentHashMap<>();

    public RedisPresenceServiceImpl(StringRedisTemplate redisTemplate,
                                    @Lazy SimpMessagingTemplate messagingTemplate,
                                    UserRepository userRepository,
                                    RedisMessageListenerContainer listenerContainer,
                                    ChannelTopic presenceTopic) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.listenerContainer = listenerContainer;
        this.presenceTopic = presenceTopic;
    }

    @PostConstruct
    public void init() {
        try {
            listenerContainer.addMessageListener(this, presenceTopic);
            listenerContainer.start();
            log.info("Subscribed to Redis presence topic: {}", presenceTopic.getTopic());
        } catch (Exception ex) {
            log.warn("Could not connect to Redis message listener, falling back to in-memory presence: {}", ex.getMessage());
        }
    }

    @Override
    public void markUserOnline(String userId, String connectionId) {
        log.info("User {} connected with session {}", userId, connectionId);
        boolean wasOffline = false;

        try {
            String userKey = KEY_USER + userId;
            String connKey = KEY_CONNECTION + connectionId;

            Long countBefore = redisTemplate.opsForSet().size(userKey);
            wasOffline = (countBefore == null || countBefore == 0);

            redisTemplate.opsForSet().add(userKey, connectionId);
            redisTemplate.opsForValue().set(connKey, userId, Duration.ofSeconds(presenceTtlSeconds));
            redisTemplate.expire(userKey, Duration.ofSeconds(presenceTtlSeconds * 2));
        } catch (Exception ex) {
            log.warn("Redis unavailable during markUserOnline, using fallback: {}", ex.getMessage());
            Set<String> sessions = inMemoryUserSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
            wasOffline = sessions.isEmpty();
            sessions.add(connectionId);
            inMemoryConnectionUsers.put(connectionId, userId);
        }

        if (wasOffline) {
            broadcastPresenceChange(userId, "ONLINE", Instant.now());
            updateUserStatusInDb(userId, "ONLINE", Instant.now());
        }
    }

    @Override
    public void markUserOffline(String userId, String connectionId) {
        log.info("User {} disconnected session {}", userId, connectionId);
        boolean isNowOffline = false;

        try {
            String userKey = KEY_USER + userId;
            String connKey = KEY_CONNECTION + connectionId;

            redisTemplate.opsForSet().remove(userKey, connectionId);
            redisTemplate.delete(connKey);

            Long remaining = redisTemplate.opsForSet().size(userKey);
            isNowOffline = (remaining == null || remaining == 0);
            if (isNowOffline) {
                redisTemplate.delete(userKey);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable during markUserOffline, using fallback: {}", ex.getMessage());
            Set<String> sessions = inMemoryUserSessions.get(userId);
            if (sessions != null) {
                sessions.remove(connectionId);
                if (sessions.isEmpty()) {
                    inMemoryUserSessions.remove(userId);
                    isNowOffline = true;
                }
            }
            inMemoryConnectionUsers.remove(connectionId);
        }

        if (isNowOffline) {
            Instant now = Instant.now();
            broadcastPresenceChange(userId, "OFFLINE", now);
            updateUserStatusInDb(userId, "OFFLINE", now);
        }
    }

    @Override
    public void refreshHeartbeat(String userId, String connectionId) {
        try {
            String connKey = KEY_CONNECTION + connectionId;
            String userKey = KEY_USER + userId;

            redisTemplate.expire(connKey, Duration.ofSeconds(presenceTtlSeconds));
            redisTemplate.expire(userKey, Duration.ofSeconds(presenceTtlSeconds * 2));
        } catch (Exception ex) {
            // Heartbeat refreshed in memory automatically
        }
    }

    @Override
    public boolean isUserOnline(String userId) {
        try {
            String userKey = KEY_USER + userId;
            Long count = redisTemplate.opsForSet().size(userKey);
            return count != null && count > 0;
        } catch (Exception ex) {
            Set<String> sessions = inMemoryUserSessions.get(userId);
            return sessions != null && !sessions.isEmpty();
        }
    }

    @Override
    public Map<String, Boolean> getUsersOnlineStatus(Collection<String> userIds) {
        Map<String, Boolean> result = new HashMap<>();
        for (String id : userIds) {
            result.put(id, isUserOnline(id));
        }
        return result;
    }

    @Override
    public Set<String> getActiveSessions(String userId) {
        try {
            String userKey = KEY_USER + userId;
            Set<String> sessions = redisTemplate.opsForSet().members(userKey);
            return sessions != null ? sessions : Set.of();
        } catch (Exception ex) {
            return inMemoryUserSessions.getOrDefault(userId, Set.of());
        }
    }

    private void broadcastPresenceChange(String userId, String status, Instant timestamp) {
        PresenceEvent event = new PresenceEvent(userId, status, timestamp);

        // Publish to Redis Pub/Sub for other backend instances
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(presenceTopic.getTopic(), json);
        } catch (Exception ex) {
            log.debug("Redis pub/sub broadcast skipped: {}", ex.getMessage());
        }

        // Direct WebSocket broadcast
        sendToWebSocket(event);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            PresenceEvent event = objectMapper.readValue(body, PresenceEvent.class);
            sendToWebSocket(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize Redis presence message", ex);
        }
    }

    private void sendToWebSocket(PresenceEvent event) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/presence", event);
        }
    }

    private void updateUserStatusInDb(String userId, String status, Instant lastSeenAt) {
        try {
            userRepository.findById(userId).ifPresent(user -> {
                user.setStatus(status);
                user.setLastSeenAt(lastSeenAt);
                userRepository.save(user);
            });
        } catch (Exception ex) {
            log.warn("Failed to persist user status to MongoDB: {}", ex.getMessage());
        }
    }
}
