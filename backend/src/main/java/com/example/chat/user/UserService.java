package com.example.chat.user;

import com.example.chat.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public UserResponse getCurrentUserProfile(String userId) {
        User user = getUserById(userId);
        return UserResponse.fromUser(user);
    }

    public List<UserSummaryResponse> searchUsers(String query, String currentUserId) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        String cleanQuery = query.trim();
        List<User> users = userRepository.searchUsers(cleanQuery, currentUserId);
        return users.stream()
                .map(UserSummaryResponse::fromUser)
                .toList();
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        user.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromUser(updatedUser);
    }

    public List<User> getUsersByIds(List<String> userIds) {
        return userRepository.findByIdIn(userIds);
    }
}
