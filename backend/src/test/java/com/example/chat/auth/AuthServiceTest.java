package com.example.chat.auth;

import com.example.chat.exception.ConflictException;
import com.example.chat.security.JwtTokenProvider;
import com.example.chat.security.UserPrincipal;
import com.example.chat.user.User;
import com.example.chat.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Alice Smith", "alice@example.com", "encodedPassword", "avatar.svg");
        sampleUser.setId("user-alice");
    }

    @Test
    @DisplayName("Should successfully register a new user and set cookie")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("Alice Smith", "alice@example.com", "secret123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateAccessToken(any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("raw-refresh-token");
        when(tokenProvider.hashToken(anyString())).thenReturn("hashed-refresh-token");
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse authResponse = authService.register(request, response);

        assertNotNull(authResponse);
        assertEquals("mock-access-token", authResponse.getAccessToken());
        assertEquals("Alice Smith", authResponse.getUser().getDisplayName());
        assertNotNull(response.getHeader("Set-Cookie"));
        assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));

        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Alice Smith", "alice@example.com", "secret123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request, response));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user and issue fresh tokens")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("alice@example.com", "secret123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserPrincipal principal = UserPrincipal.create(sampleUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById("user-alice")).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generateAccessToken(any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("raw-refresh-token");
        when(tokenProvider.hashToken(anyString())).thenReturn("hashed-refresh-token");
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse authResponse = authService.login(request, response);

        assertNotNull(authResponse);
        assertEquals("mock-access-token", authResponse.getAccessToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }
}
