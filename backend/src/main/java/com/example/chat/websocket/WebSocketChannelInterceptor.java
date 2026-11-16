package com.example.chat.websocket;

import com.example.chat.conversation.ConversationService;
import com.example.chat.security.JwtTokenProvider;
import com.example.chat.security.UserDetailsServiceImpl;
import com.example.chat.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;

@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannelInterceptor.class);

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final ConversationService conversationService;

    public WebSocketChannelInterceptor(JwtTokenProvider tokenProvider,
                                       UserDetailsServiceImpl userDetailsService,
                                       @Lazy ConversationService conversationService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.conversationService = conversationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String token = extractToken(accessor);
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                String userId = tokenProvider.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserById(userId);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                accessor.setUser(auth);
                log.info("WebSocket CONNECT authenticated for user: {}", userId);
            } else {
                log.warn("WebSocket CONNECT rejected: Missing or invalid JWT token");
                throw new AuthenticationCredentialsNotFoundException("Valid JWT token required to connect");
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            validateSubscription(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            Principal user = accessor.getUser();
            if (user == null) {
                log.warn("WebSocket SEND rejected: Unauthenticated");
                throw new AccessDeniedException("Unauthenticated SEND message");
            }
        }

        return message;
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new AccessDeniedException("Unauthenticated subscription");
        }

        String userId;
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof UserPrincipal up) {
            userId = up.getId();
        } else {
            userId = principal.getName();
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        // Validate conversation subscription: /topic/conversations/{conversationId}/**
        if (destination.startsWith("/topic/conversations/")) {
            String subPath = destination.substring("/topic/conversations/".length());
            String conversationId = subPath.split("/")[0];

            try {
                conversationService.verifyMembership(conversationId, userId);
            } catch (Exception ex) {
                log.warn("User {} denied subscription to destination {}: {}", userId, destination, ex.getMessage());
                throw new AccessDeniedException("You are not authorized to subscribe to conversation " + conversationId);
            }
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Support 'token' or 'access_token' header
        String customToken = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(customToken)) {
            return customToken;
        }

        return null;
    }
}
