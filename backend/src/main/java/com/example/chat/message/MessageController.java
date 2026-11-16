package com.example.chat.message;

import com.example.chat.common.ApiResponse;
import com.example.chat.common.PagedResponse;
import com.example.chat.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations/{conversationId}")
@Tag(name = "Messages", description = "Message history and message operations")
@SecurityRequirement(name = "BearerAuth")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/messages")
    @Operation(summary = "Get conversation messages", description = "Cursor-paginated message history returned in chronological order")
    public ResponseEntity<ApiResponse<PagedResponse<MessageResponse>>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "30") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PagedResponse<MessageResponse> response = messageService.getMessages(
                conversationId, userPrincipal.getId(), before, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message via HTTP", description = "Optional REST fallback to send a message to a conversation")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        MessageResponse response = messageService.sendMessage(
                conversationId, userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/read")
    @Operation(summary = "Mark conversation as read", description = "Resets the user's unread badge count and records read receipt")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String conversationId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String messageId = body != null ? body.get("messageId") : null;
        messageService.markConversationAsRead(conversationId, userPrincipal.getId(), messageId);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read", null));
    }
}
