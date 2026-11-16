package com.example.chat.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, 60000, 120000);
    }

    @Test
    @DisplayName("Should generate and validate JWT access token")
    void testGenerateAndValidateToken() {
        UserPrincipal principal = new UserPrincipal(
                "user-123",
                "test@example.com",
                "Test User",
                "hashedpassword",
                "avatar.png",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = tokenProvider.generateAccessToken(principal);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        String userId = tokenProvider.getUserIdFromToken(token);
        assertEquals("user-123", userId);
    }

    @Test
    @DisplayName("Should reject invalid or forged token")
    void testRejectInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.payload"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Should hash tokens deterministically using SHA-256")
    void testHashToken() {
        String rawToken = "sample-refresh-token-xyz";
        String hash1 = tokenProvider.hashToken(rawToken);
        String hash2 = tokenProvider.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // 256 bits = 64 hex chars
    }
}
