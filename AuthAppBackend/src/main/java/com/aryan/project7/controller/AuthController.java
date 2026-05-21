package com.aryan.project7.controller;

import com.aryan.project7.dtos.LoginRequest;
import com.aryan.project7.dtos.RefreshTokenRequest;
import com.aryan.project7.dtos.TokenResponse;
import com.aryan.project7.dtos.UserDto;
import com.aryan.project7.entity.RefreshToken;
import com.aryan.project7.entity.User;
import com.aryan.project7.repository.RefreshTokenRepo;
import com.aryan.project7.repository.UserRepository;
import com.aryan.project7.security.CookieService;
import com.aryan.project7.security.JwtService;
import com.aryan.project7.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenRepo refreshTokenRepo;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final CookieService cookieService;

    // This is the front door. We check credentials and hand out keys (tokens).
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        // First, make sure they actually are who they say they are
        authenticate(loginRequest);

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid Username or Password"));

        if (!user.isEnabled()) {
            throw new DisabledException("User is Disabled");
        }

        // We create a unique ID for this refresh token so we can track/revoke it later
        String jti = UUID.randomUUID().toString();
        var refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepo.save(refreshTokenOb);

        // Generate the actual JWT strings
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

        // We tuck the refresh token into a secure cookie so the frontend doesn't have to manage it manually
        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeader(response);

        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDto.class));
        return ResponseEntity.ok(tokenResponse);
    }

    // Helper method to let Spring Security handle the password check
    private Authentication authenticate(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid Username or Password");
        }
    }

    // This is for when the short-lived access token expires but the user is still active
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        // Try to find the refresh token in cookies, the body, or headers
        String refreshToken = readRefreshTokenFromRequest(body, request)
                .orElseThrow(() -> new BadCredentialsException("Refresh Token Not Recognized"));

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }

        String jti = jwtService.getJti(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);

        RefreshToken storedRefreshToken = refreshTokenRepo.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Refresh Token Not Recognized"));

        // Security check: has this token been used or expired?
        if (storedRefreshToken.isRevoked() || storedRefreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        if (!storedRefreshToken.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Refresh token does not belong to this user");
        }

        // "Token Rotation": Kill the old one, create a brand new one
        storedRefreshToken.setRevoked(true);
        String newJti = UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJti);
        refreshTokenRepo.save(storedRefreshToken);

        User user = storedRefreshToken.getUser();
        var newRefreshTokenOb = RefreshToken.builder()
                .jti(newJti) // Using the new JTI here
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepo.save(newRefreshTokenOb);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenStr = jwtService.generateRefreshToken(user, newRefreshTokenOb.getJti());

        cookieService.attachRefreshCookie(response, newRefreshTokenStr, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeader(response);

        return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshTokenStr, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDto.class)));
    }

    // Kill the session, revoke the token in the DB, and wipe the cookie
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        readRefreshTokenFromRequest(null, request).ifPresent(token -> {
            try {
                if (jwtService.isRefreshToken(token)) {
                    String jti = jwtService.getJti(token);
                    refreshTokenRepo.findByJti(jti).ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepo.save(rt);
                    });
                }
            } catch (JwtException ignored) {
                // If the token is already mangled, we don't care, they're logging out anyway
            }
        });

        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeader(response);
        SecurityContextHolder.clearContext();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // A flexible helper that looks for the refresh token wherever the client might have hidden it
    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        // 1. Look in Cookies (safest/best way)
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue) // You were grabbing getName() here before! Fixed to getValue()
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst();
            if (fromCookie.isPresent()) return fromCookie;
        }

        // 2. Look in the JSON body
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return Optional.of(body.refreshToken());
        }

        // 3. Look in a custom header
        String refreshHeader = request.getHeader("X-Refresh-Token");
        if (refreshHeader != null && !refreshHeader.isBlank()) {
            return Optional.of(refreshHeader.trim());
        }

        // 4. Look in the Auth header (if they sent the refresh token as a Bearer)
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer", 0, 7)) {
            String candidate = authHeader.substring(7).trim();
            try {
                if (!candidate.isEmpty() && jwtService.isRefreshToken(candidate)) {
                    return Optional.of(candidate);
                }
            } catch (Exception ignored) {}
        }

        return Optional.empty();
    }

    // Creating a new account
    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegisterDto userDto) {
        UserDto dto = new UserDto();
        dto.setName(userDto.name());
        dto.setEmail(userDto.email());
        dto.setPassword(userDto.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
    }
}