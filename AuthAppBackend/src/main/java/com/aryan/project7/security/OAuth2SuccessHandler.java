package com.aryan.project7.security;

import com.aryan.project7.entity.Provider;
import com.aryan.project7.entity.RefreshToken;
import com.aryan.project7.entity.User;
import com.aryan.project7.repository.RefreshTokenRepo;
import com.aryan.project7.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;

// This class kicks in right after a user successfully logs in via Google or GitHub.
// Its job is to save the user to our DB if they're new and hand out our own JWTs.
@Component
@RequiredArgsConstructor
@Transactional
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepo refreshTokenRepo;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontendSuccessUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info("Social login successful! Processing user data...");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = "unknown";

        // Figure out if they came from Google, GitHub, etc.
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        User userEntity;

        // We handle different social providers here since they all send data in different formats
        switch (registrationId) {
            case "google" -> {
                String googleId = oAuth2User.getAttribute("sub");
                String email = oAuth2User.getAttribute("email");
                if (email == null || email.isBlank()) {
                    throw new RuntimeException("Google account missing email. Contact support.");
                }
                String name = oAuth2User.getAttribute("name");
                String picture = oAuth2User.getAttribute("picture");

                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(picture)
                        .enabled(true)
                        .provider(Provider.GOOGLE)
                        .providerId(googleId)
                        .build();

                // If we've seen this email before, just use that user. If not, save the new one.
                userEntity = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));
            }
            case "github" -> {
                // Safely retrieve the ID object
                Object idObj = oAuth2User.getAttribute("id");

                // Convert to String regardless of whether it's an Integer, Long, or String
                String githubId = (idObj != null) ? idObj.toString() : null;

                String email = oAuth2User.getAttribute("email");
                String name = oAuth2User.getAttribute("login");
                String image = oAuth2User.getAttribute("avatar_url");

                if (email == null) {
                    email = name + "@github.com";
                }

                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(image)
                        .enabled(true)
                        .provider(Provider.GITHUB)
                        .providerId(githubId)
                        .build();

                userEntity = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));
            }
            default -> throw new RuntimeException("We don't support " + registrationId + " yet!");
        }

        // Now that we have a user in our DB, we generate our own session tokens
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(userEntity)
                .revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();

        refreshTokenRepo.save(refreshTokenOb);

        // Generate the JWTs
        String refreshToken = jwtService.generateRefreshToken(userEntity, jti);

        // --- NEW LINE: Generate the Access Token ---
        String accessToken = jwtService.generateAccessToken(userEntity);

        // Stick the refresh token in a secure cookie
        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());

        logger.info("User {} is logged in. Redirecting to frontend...", userEntity.getEmail());

        // --- NEW LOGIC: Attach the Access Token to the URL safely ---
        String finalRedirectUrl = org.springframework.web.util.UriComponentsBuilder
                .fromUriString(frontendSuccessUrl)
                .queryParam("token", accessToken)
                .build().toUriString();

        // Send the user back to the frontend (e.g., your React/Angular dashboard)
        response.sendRedirect(finalRedirectUrl);

    }
}