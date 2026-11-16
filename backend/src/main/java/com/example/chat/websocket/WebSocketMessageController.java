package com.example.chat.websocket;

import com.example.chat.message.MessageResponse;
import com.example.chat.message.MessageService;
import com.example.chat.message.SendMessageRequest;
import com.example.chat.presence.PresenceService;
import com.example.chat.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketMessageController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageController.class);

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    public WebSocketMessageController(MessageService messageService,
                                     SimpMessagingTemplate messagingTemplate,
                                     PresenceService presenceService) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
    }

    @MessageMapping("/chat.send")
    public void handleSendMessage(@Payload ChatMessagePayload payload, Principal principal) {
        String userId = getUserId(principal);
        log.info("WebSocket chat.send received from user {} for conversation {}", userId, payload.getConversationId());

        SendMessageRequest request = new SendMessageRequest(payload.getContent(), payload.getClientMessageId());
        messageService.sendMessage(payload.getConversationId(), userId, request);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingEventPayload payload, Principal principal) {
        String userId = getUserId(principal);
        String displayName = getDisplayName(principal);

        TypingNotification notification = new TypingNotification(
                payload.getConversationId(),
                userId,
                displayName,
                payload.isTyping()
        );

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + payload.getConversationId() + "/typing",
                notification
        );
    }

    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload ReadReceiptPayload payload, Principal principal) {
        String userId = getUserId(principal);
        messageService.markConversationAsRead(payload.getConversationId(), userId, payload.getMessageId());
    }

    @MessageMapping("/presence.heartbeat")
    public void handleHeartbeat(@Header("simpSessionId") String sessionId, Principal principal) {
        String userId = getUserId(principal);
        presenceService.refreshHeartbeat(userId, sessionId);
    }

    private String getUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        return principal.getName();
    }

    private String getDisplayName(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getDisplayName();
        }
        return principal.getName();
    }
}
