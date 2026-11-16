package com.example.chat.user;

import java.time.Instant;

public class UserSummaryResponse {
    private String id;
    private String displayName;
    private String email;
    private String avatarUrl;
    private String status;
    private Instant lastSeenAt;

    public UserSummaryResponse() {}

    public UserSummaryResponse(String id, String displayName, String email, String avatarUrl, String status, Instant lastSeenAt) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.lastSeenAt = lastSeenAt;
    }

    public static UserSummaryResponse fromUser(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getLastSeenAt()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
