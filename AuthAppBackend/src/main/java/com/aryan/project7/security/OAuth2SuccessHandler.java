package com.aryan.project7.security;

import com.aryan.project7.entity.*;
import com.aryan.project7.repository.RefreshTokenRedisRepo;
import com.aryan.project7.repository.RefreshTokenRepo;
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
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepo refreshTokenRepo;
    private final RefreshTokenRedisRepo redisRepo;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontendSuccessUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            logger.debug("Response already committed, skipping redirect.");
            return;
        }

        logger.info("Social login successful! Processing user data...");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = (authentication instanceof OAuth2AuthenticationToken token)
                ? token.getAuthorizedClientRegistrationId() : "unknown";

        User userEntity = processUser(oAuth2User, registrationId);

        // --- IDEMPOTENCY CHECK: Reuse active session if it exists ---
        Optional<RefreshToken> activeTokenOpt = refreshTokenRepo.findByUserAndRevokedFalse(userEntity);

        String refreshToken;
        if (activeTokenOpt.isPresent()) {
            refreshToken = jwtService.generateRefreshToken(userEntity, activeTokenOpt.get().getJti());
            logger.info("Reusing existing session for user: {}", userEntity.getEmail());
        } else {
            String jti = UUID.randomUUID().toString();
            RefreshToken refreshTokenOb = RefreshToken.builder()
                    .jti(jti)
                    .user(userEntity)
                    .revoked(false)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                    .build();
            refreshTokenRepo.save(refreshTokenOb);
            refreshToken = jwtService.generateRefreshToken(userEntity, jti);
            logger.info("Created new session for user: {}", userEntity.getEmail());
        }

        // --- Update Redis Cache ---
        String hashedToken = DigestUtils.sha256Hex(refreshToken);
        RefreshTokenRedis redisToken = RefreshTokenRedis.builder()
                .tokenHash(hashedToken)
                .userId(userEntity.getId().toString())
                .expiryDate(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()).getEpochSecond())
                .build();
        redisRepo.save(redisToken);

        // --- Finalize response ---
        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());

        logger.info("Redirecting user {} to frontend...", userEntity.getEmail());
        response.sendRedirect(frontendSuccessUrl);
    }

    private User processUser(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId) {
            case "google" -> {
                String email = oAuth2User.getAttribute("email");
                yield userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(oAuth2User.getAttribute("name"))
                        .image(oAuth2User.getAttribute("picture"))
                        .enabled(true)
                        .provider(Provider.GOOGLE)
                        .providerId(oAuth2User.getAttribute("sub"))
                        .build()));
            }
            case "github" -> {
                String email = oAuth2User.getAttribute("email") != null ? oAuth2User.getAttribute("email") : oAuth2User.getAttribute("login") + "@github.com";
                yield userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(oAuth2User.getAttribute("login"))
                        .image(oAuth2User.getAttribute("avatar_url"))
                        .enabled(true)
                        .provider(Provider.GITHUB)
                        .providerId(String.valueOf(oAuth2User.getAttribute("id")))
                        .build()));
            }
            default -> throw new RuntimeException("Provider " + registrationId + " not supported!");
        };
    }
}