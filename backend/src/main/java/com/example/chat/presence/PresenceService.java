package com.example.chat.presence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface PresenceService {

    void markUserOnline(String userId, String connectionId);

    void markUserOffline(String userId, String connectionId);

    void refreshHeartbeat(String userId, String connectionId);

    boolean isUserOnline(String userId);

    Map<String, Boolean> getUsersOnlineStatus(Collection<String> userIds);

    Set<String> getActiveSessions(String userId);
}
