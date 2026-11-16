package com.example.chat.auth;

import com.example.chat.exception.ConflictException;
import com.example.chat.exception.UnauthorizedException;
import com.example.chat.security.JwtTokenProvider;
import com.example.chat.security.UserPrincipal;
import com.example.chat.user.User;
import com.example.chat.user.UserRepository;
import com.example.chat.user.UserSummaryResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Value("${app.jwt.refresh-token-cookie-name:chat_refresh_token}")
    private String refreshTokenCookieName = "chat_refresh_token";

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ConflictException("Email is already registered: " + cleanEmail);
        }

        String displayName = request.getDisplayName().trim();
        String defaultAvatar = "https://api.dicebear.com/7.x/initials/svg?seed=" +
                URLEncoder.encode(displayName, StandardCharsets.UTF_8);

        User user = new User(
                displayName,
                cleanEmail,
                passwordEncoder.encode(request.getPassword()),
                defaultAvatar
        );

        User savedUser = userRepository.save(user);
        UserPrincipal userPrincipal = UserPrincipal.create(savedUser);

        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = createAndSaveRefreshToken(savedUser.getId());
        attachRefreshTokenCookie(response, refreshToken, tokenProvider.getRefreshTokenExpirationMs() / 1000);

        return new AuthResponse(
                accessToken,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                UserSummaryResponse.fromUser(savedUser)
        );
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(cleanEmail, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = createAndSaveRefreshToken(user.getId());
        attachRefreshTokenCookie(response, refreshToken, tokenProvider.getRefreshTokenExpirationMs() / 1000);

        return new AuthResponse(
                accessToken,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                UserSummaryResponse.fromUser(user)
        );
    }

    public TokenRefreshResponse refreshToken(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is missing");
        }

        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new UnauthorizedException("Refresh token has expired or been revoked");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Rotate refresh token
        refreshTokenRepository.delete(storedToken);
        String newRefreshToken = createAndSaveRefreshToken(user.getId());
        attachRefreshTokenCookie(response, newRefreshToken, tokenProvider.getRefreshTokenExpirationMs() / 1000);

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        return new TokenRefreshResponse(
                newAccessToken,
                tokenProvider.getAccessTokenExpirationMs() / 1000
        );
    }

    public void logout(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = tokenProvider.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(refreshTokenRepository::delete);
        }
        attachRefreshTokenCookie(response, "", 0);
        SecurityContextHolder.clearContext();
    }

    private String createAndSaveRefreshToken(String userId) {
        String rawToken = tokenProvider.generateRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawToken);
        Instant expiresAt = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private void attachRefreshTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, token)
                .httpOnly(true)
                .secure(false) // Set to false for dev compatibility, true in prod HTTPS
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
