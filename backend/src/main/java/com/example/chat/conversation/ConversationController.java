package com.example.chat.conversation;

import com.example.chat.common.ApiResponse;
import com.example.chat.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Direct and group conversation management")
@SecurityRequirement(name = "BearerAuth")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    @Operation(summary = "Get current user conversations", description = "Retrieves all conversations for the authenticated user, ordered by most recent message")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getUserConversations(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ConversationResponse> conversations = conversationService.getUserConversations(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @PostMapping("/direct")
    @Operation(summary = "Start or retrieve direct conversation", description = "Creates a direct conversation between two users or returns an existing one, preventing duplicates")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateDirectConversation(
            @Valid @RequestBody CreateDirectConversationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ConversationResponse response = conversationService.getOrCreateDirectConversation(
                userPrincipal.getId(), request.getRecipientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/group")
    @Operation(summary = "Create group conversation", description = "Creates a new group conversation with the specified members")
    public ResponseEntity<ApiResponse<ConversationResponse>> createGroupConversation(
            @Valid @RequestBody CreateGroupConversationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ConversationResponse response = conversationService.createGroupConversation(
                userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation details", description = "Retrieves details and member list for a conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ConversationResponse response = conversationService.getConversationDetails(conversationId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{conversationId}/members")
    @Operation(summary = "Add member to group", description = "Adds a new member to an existing group conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> addMember(
            @PathVariable String conversationId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ConversationResponse response = conversationService.addMemberToGroup(
                conversationId, request.getUserId(), userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", response));
    }

    @DeleteMapping("/{conversationId}/members/{userId}")
    @Operation(summary = "Remove member or leave group", description = "Removes a member from a group (admins only) or allows a member to leave")
    public ResponseEntity<ApiResponse<ConversationResponse>> removeMember(
            @PathVariable String conversationId,
            @PathVariable String userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ConversationResponse response = conversationService.removeMemberFromGroup(
                conversationId, userId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", response));
    }
}
