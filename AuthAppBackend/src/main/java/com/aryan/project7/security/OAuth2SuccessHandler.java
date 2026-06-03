package com.aryan.project7.security;

import com.aryan.project7.entity.*;
import com.aryan.project7.repository.RefreshTokenRedisRepo;
import com.aryan.project7.repository.RefreshTokenRepo;
import com.aryan.project7.repository.RoleRepository;
import com.aryan.project7.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Injected RoleRepository
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepo refreshTokenRepo;
    private final RefreshTokenRedisRepo redisRepo;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontendSuccessUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) return;

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = (authentication instanceof OAuth2AuthenticationToken token)
                ? token.getAuthorizedClientRegistrationId() : "unknown";

        User userEntity = processUser(oAuth2User, registrationId);

        // Fetch list of active tokens
        List<RefreshToken> activeTokens = refreshTokenRepo.findByUserAndRevokedFalseOrderByCreatedAtDesc(userEntity);

        String refreshToken;
        long ttl = jwtService.getRefreshTtlSeconds(); // Get TTL from JwtService

        if (!activeTokens.isEmpty()) {
            RefreshToken activeToken = activeTokens.get(0);
            refreshToken = jwtService.generateRefreshToken(userEntity, activeToken.getJti());
            logger.info("Reusing existing session for user: {}", userEntity.getEmail());
        } else {
            refreshToken = createNewSession(userEntity);
            logger.info("Created new session for user: {}", userEntity.getEmail());
        }

        // --- Update Redis Cache with TTL ---
        try {
            String hashedToken = DigestUtils.sha256Hex(refreshToken);

            // Sync with Redis using the new builder structure that supports TTL
            redisRepo.save(RefreshTokenRedis.builder()
                    .tokenHash(hashedToken)
                    .userId(userEntity.getId().toString())
                    .expiryDate(Instant.now().plusSeconds(ttl).getEpochSecond())
                    .ttl(ttl) // This maps to the @TimeToLive field in your Redis entity
                    .build());

            logger.info("Successfully cached refresh token in Redis for user: {}", userEntity.getEmail());
        } catch (Exception e) {
            logger.error("Redis unreachable, skipping cache update: {}", e.getMessage());
        }

        cookieService.attachRefreshCookie(response, refreshToken, (int) ttl);
        response.sendRedirect(frontendSuccessUrl);
    }
    private String createNewSession(User user) {
        String jti = UUID.randomUUID().toString();
        refreshTokenRepo.save(RefreshToken.builder()
                .jti(jti).user(user).revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build());
        return jwtService.generateRefreshToken(user, jti);
    }

    private User processUser(OAuth2User oAuth2User, String registrationId) {
        // Force exact match for "USER"
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Error: Default Role 'USER' not found in database."));

        String email = registrationId.equals("google") ? oAuth2User.getAttribute("email")
                : (oAuth2User.getAttribute("email") != null ? oAuth2User.getAttribute("email") : oAuth2User.getAttribute("login") + "@github.com");

        // Use .map() to avoid duplication and build cleanly
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(registrationId.equals("google") ? oAuth2User.getAttribute("name") : oAuth2User.getAttribute("login"))
                    .image(registrationId.equals("google") ? oAuth2User.getAttribute("picture") : oAuth2User.getAttribute("avatar_url"))
                    .roles(Set.of(userRole)) // Assign the role here
                    .enabled(true)
                    .provider(registrationId.equals("google") ? Provider.GOOGLE : Provider.GITHUB)
                    .providerId(registrationId.equals("google") ? oAuth2User.getAttribute("sub") : String.valueOf(oAuth2User.getAttribute("id")))
                    .build();
            return userRepository.save(newUser);
        });
    }
}