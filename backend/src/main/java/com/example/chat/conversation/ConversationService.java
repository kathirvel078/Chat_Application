package com.example.chat.conversation;

import com.example.chat.exception.BadRequestException;
import com.example.chat.exception.ForbiddenException;
import com.example.chat.exception.ResourceNotFoundException;
import com.example.chat.message.Message;
import com.example.chat.message.MessageRepository;
import com.example.chat.message.MessageType;
import com.example.chat.presence.PresenceService;
import com.example.chat.user.User;
import com.example.chat.user.UserRepository;
import com.example.chat.user.UserSummaryResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PresenceService presenceService;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMembershipRepository membershipRepository,
                               UserRepository userRepository,
                               MessageRepository messageRepository,
                               @Lazy PresenceService presenceService) {
        this.conversationRepository = conversationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.presenceService = presenceService;
    }

    public List<ConversationResponse> getUserConversations(String currentUserId) {
        List<Conversation> conversations = conversationRepository
                .findByMemberIdsContainingOrderByLastMessageAtDesc(currentUserId);

        if (conversations.isEmpty()) {
            return List.of();
        }

        // Preload memberships for unread count
        Map<String, ConversationMembership> membershipMap = membershipRepository.findByUserId(currentUserId).stream()
                .collect(Collectors.toMap(ConversationMembership::getConversationId, m -> m, (a, b) -> a));

        // Preload all users involved in these conversations
        Set<String> allUserIds = conversations.stream()
                .flatMap(c -> c.getMemberIds().stream())
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findByIdIn(new ArrayList<>(allUserIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return conversations.stream()
                .map(c -> toResponse(c, currentUserId, userMap, membershipMap.get(c.getId())))
                .toList();
    }

    public Conversation getConversationById(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
    }

    public ConversationResponse getConversationDetails(String conversationId, String currentUserId) {
        Conversation conversation = getConversationById(conversationId);
        verifyMembership(conversation, currentUserId);

        Map<String, User> userMap = userRepository.findByIdIn(conversation.getMemberIds()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        ConversationMembership membership = membershipRepository
                .findByConversationIdAndUserId(conversationId, currentUserId).orElse(null);

        return toResponse(conversation, currentUserId, userMap, membership);
    }

    @Transactional
    public ConversationResponse getOrCreateDirectConversation(String currentUserId, String recipientId) {
        if (currentUserId.equals(recipientId)) {
            throw new BadRequestException("Cannot create a direct conversation with yourself");
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", recipientId));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        String pairKey = Conversation.buildDirectPairKey(currentUserId, recipientId);

        Optional<Conversation> existing = conversationRepository.findByDirectPairKey(pairKey);
        if (existing.isPresent()) {
            return getConversationDetails(existing.get().getId(), currentUserId);
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.DIRECT);
        conversation.setDirectPairKey(pairKey);
        conversation.setMemberIds(List.of(currentUserId, recipientId));
        conversation.setAdminIds(List.of(currentUserId, recipientId));
        conversation.setCreatedBy(currentUserId);
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        conversation.setLastMessageAt(Instant.now());

        Conversation saved = conversationRepository.save(conversation);

        // Create initial memberships
        membershipRepository.save(new ConversationMembership(saved.getId(), currentUserId));
        membershipRepository.save(new ConversationMembership(saved.getId(), recipientId));

        Map<String, User> userMap = Map.of(currentUser.getId(), currentUser, recipient.getId(), recipient);
        return toResponse(saved, currentUserId, userMap, null);
    }

    @Transactional
    public ConversationResponse createGroupConversation(String currentUserId, CreateGroupConversationRequest request) {
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        Set<String> memberIdSet = new HashSet<>(request.getMemberIds());
        memberIdSet.add(currentUserId);

        List<User> members = userRepository.findByIdIn(new ArrayList<>(memberIdSet));
        if (members.size() < 2) {
            throw new BadRequestException("A group conversation must have at least 2 members");
        }

        List<String> validMemberIds = members.stream().map(User::getId).toList();

        String avatarUrl = request.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = "https://api.dicebear.com/7.x/identicon/svg?seed=" +
                    URLEncoder.encode(request.getName(), StandardCharsets.UTF_8);
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.GROUP);
        conversation.setName(request.getName().trim());
        conversation.setAvatarUrl(avatarUrl);
        conversation.setMemberIds(validMemberIds);
        conversation.setAdminIds(List.of(currentUserId));
        conversation.setCreatedBy(currentUserId);
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        conversation.setLastMessageAt(Instant.now());

        Conversation saved = conversationRepository.save(conversation);

        final String conversationId = saved.getId();

        // Create memberships
        List<ConversationMembership> memberships = validMemberIds.stream()
                .map(uid -> new ConversationMembership(conversationId, uid))
                .toList();
        membershipRepository.saveAll(memberships);

        // Create system welcome message
        Message systemMsg = new Message(
                conversationId,
                currentUserId,
                "System",
                null,
                creator.getDisplayName() + " created group \"" + conversation.getName() + "\"",
                MessageType.SYSTEM,
                null
        );
        Message savedMsg = messageRepository.save(systemMsg);

        saved.setLastMessage(new LastMessageDto(
                savedMsg.getId(),
                currentUserId,
                "System",
                savedMsg.getContent(),
                MessageType.SYSTEM.name(),
                savedMsg.getCreatedAt()
        ));
        Conversation finalSaved = conversationRepository.save(saved);

        Map<String, User> userMap = members.stream().collect(Collectors.toMap(User::getId, u -> u));
        return toResponse(finalSaved, currentUserId, userMap, null);
    }

    @Transactional
    public ConversationResponse addMemberToGroup(String conversationId, String newUserId, String currentUserId) {
        Conversation conversation = getConversationById(conversationId);
        if (conversation.getType() != ConversationType.GROUP) {
            throw new BadRequestException("Members can only be added to group conversations");
        }
        verifyMembership(conversation, currentUserId);

        if (conversation.getMemberIds().contains(newUserId)) {
            throw new BadRequestException("User is already a member of this conversation");
        }

        User newUser = userRepository.findById(newUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", newUserId));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        conversation.getMemberIds().add(newUserId);
        conversation.setUpdatedAt(Instant.now());

        // Create membership
        membershipRepository.save(new ConversationMembership(conversationId, newUserId));

        // Create system message
        Message systemMsg = new Message(
                conversationId,
                currentUserId,
                "System",
                null,
                currentUser.getDisplayName() + " added " + newUser.getDisplayName() + " to the group",
                MessageType.SYSTEM,
                null
        );
        Message savedMsg = messageRepository.save(systemMsg);

        conversation.setLastMessage(new LastMessageDto(
                savedMsg.getId(),
                currentUserId,
                "System",
                savedMsg.getContent(),
                MessageType.SYSTEM.name(),
                savedMsg.getCreatedAt()
        ));
        conversation.setLastMessageAt(savedMsg.getCreatedAt());
        Conversation updated = conversationRepository.save(conversation);

        return getConversationDetails(updated.getId(), currentUserId);
    }

    @Transactional
    public ConversationResponse removeMemberFromGroup(String conversationId, String targetUserId, String currentUserId) {
        Conversation conversation = getConversationById(conversationId);
        if (conversation.getType() != ConversationType.GROUP) {
            throw new BadRequestException("Members can only be removed from group conversations");
        }
        verifyMembership(conversation, currentUserId);

        boolean isSelf = currentUserId.equals(targetUserId);
        boolean isAdmin = conversation.getAdminIds().contains(currentUserId);

        if (!isSelf && !isAdmin) {
            throw new ForbiddenException("Only group admins can remove other members");
        }

        if (!conversation.getMemberIds().contains(targetUserId)) {
            throw new BadRequestException("User is not a member of this group");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        conversation.getMemberIds().remove(targetUserId);
        conversation.getAdminIds().remove(targetUserId);
        conversation.setUpdatedAt(Instant.now());

        // If no admins left and group still has members, promote the first member
        if (conversation.getAdminIds().isEmpty() && !conversation.getMemberIds().isEmpty()) {
            conversation.getAdminIds().add(conversation.getMemberIds().get(0));
        }

        membershipRepository.deleteByConversationIdAndUserId(conversationId, targetUserId);

        String systemText = isSelf
                ? targetUser.getDisplayName() + " left the group"
                : currentUser.getDisplayName() + " removed " + targetUser.getDisplayName() + " from the group";

        Message systemMsg = new Message(
                conversationId,
                currentUserId,
                "System",
                null,
                systemText,
                MessageType.SYSTEM,
                null
        );
        Message savedMsg = messageRepository.save(systemMsg);

        conversation.setLastMessage(new LastMessageDto(
                savedMsg.getId(),
                currentUserId,
                "System",
                savedMsg.getContent(),
                MessageType.SYSTEM.name(),
                savedMsg.getCreatedAt()
        ));
        conversation.setLastMessageAt(savedMsg.getCreatedAt());
        Conversation updated = conversationRepository.save(conversation);

        return getConversationDetails(updated.getId(), currentUserId);
    }

    public void verifyMembership(Conversation conversation, String userId) {
        if (!conversation.getMemberIds().contains(userId)) {
            throw new ForbiddenException("You are not a member of this conversation");
        }
    }

    public void verifyMembership(String conversationId, String userId) {
        Conversation conversation = getConversationById(conversationId);
        verifyMembership(conversation, userId);
    }

    private ConversationResponse toResponse(Conversation conversation, String currentUserId,
                                            Map<String, User> userMap, ConversationMembership membership) {
        ConversationResponse resp = new ConversationResponse();
        resp.setId(conversation.getId());
        resp.setType(conversation.getType());
        resp.setMemberIds(conversation.getMemberIds());
        resp.setAdminIds(conversation.getAdminIds());
        resp.setCreatedBy(conversation.getCreatedBy());
        resp.setLastMessage(conversation.getLastMessage());
        resp.setLastMessageAt(conversation.getLastMessageAt());
        resp.setCreatedAt(conversation.getCreatedAt());
        resp.setUpdatedAt(conversation.getUpdatedAt());
        resp.setUnreadCount(membership != null ? membership.getUnreadCount() : 0);

        List<UserSummaryResponse> members = conversation.getMemberIds().stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(UserSummaryResponse::fromUser)
                .toList();
        resp.setMembers(members);

        if (conversation.getType() == ConversationType.DIRECT) {
            String peerId = conversation.getMemberIds().stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElse(currentUserId);

            User peer = userMap.get(peerId);
            if (peer != null) {
                resp.setName(peer.getDisplayName());
                resp.setAvatarUrl(peer.getAvatarUrl());
                resp.setOnline(presenceService != null && presenceService.isUserOnline(peerId));
            } else {
                resp.setName("Direct Chat");
            }
        } else {
            resp.setName(conversation.getName());
            resp.setAvatarUrl(conversation.getAvatarUrl());
            resp.setOnline(false);
        }

        return resp;
    }
}
