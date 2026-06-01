package com.aryan.project7.controller;

import com.aryan.project7.dtos.*;
import com.aryan.project7.entity.*;
import com.aryan.project7.repository.*;
import com.aryan.project7.security.CookieService;
import com.aryan.project7.security.JwtService;
import com.aryan.project7.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    private final RefreshTokenRedisRepo redisRepo;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        // 1. Authenticate first - throws exception if invalid
        authenticate(loginRequest);

        // 2. Fetch user AFTER auth, guaranteed to exist
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!user.isEnabled()) {
            throw new DisabledException("User is Disabled");
        }

        // 3. Create session
        String jti = UUID.randomUUID().toString();
        var refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepo.save(refreshTokenOb);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeader(response);

        return ResponseEntity.ok(TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDto.class)));
    }

    private void authenticate(LoginRequest loginRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));
    }

    @PostMapping("/refresh")
    public synchronized ResponseEntity<TokenResponse> refreshToken( // Added 'synchronized'
                                                                    @RequestBody(required = false) RefreshTokenRequest body,
                                                                    HttpServletResponse response,
                                                                    HttpServletRequest request
    ) {
        String refreshToken = readRefreshTokenFromRequest(body, request)
                .orElseThrow(() -> new BadCredentialsException("Refresh Token Not Recognized"));

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }

        String jti = jwtService.getJti(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);

        RefreshToken storedRefreshToken = refreshTokenRepo.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Refresh Token Not Recognized"));

        if (storedRefreshToken.isRevoked() || storedRefreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        if (!storedRefreshToken.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Invalid token user");
        }

        // Rotation
        storedRefreshToken.setRevoked(true);
        String newJti = UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJti);
        refreshTokenRepo.save(storedRefreshToken);

        User user = storedRefreshToken.getUser();
        var newRefreshTokenOb = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepo.save(newRefreshTokenOb);

        String newRefreshTokenStr = jwtService.generateRefreshToken(user, newJti);
        String newAccessToken = jwtService.generateAccessToken(user);

        // Redis Hash
        String hashedToken = DigestUtils.sha256Hex(newRefreshTokenStr);
        redisRepo.save(RefreshTokenRedis.builder()
                .tokenHash(hashedToken)
                .userId(user.getId().toString())
                .expiryDate(newRefreshTokenOb.getExpiresAt().getEpochSecond())
                .build());

        cookieService.attachRefreshCookie(response, newRefreshTokenStr, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeader(response);

        return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshTokenStr, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDto.class)));
    }

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
            } catch (JwtException ignored) {}
        });
        cookieService.clearRefreshCookie(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst();
            if (fromCookie.isPresent()) return fromCookie;
        }
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) return Optional.of(body.refreshToken());
        return Optional.empty();
    }

    // Creating a new account
    @PostMapping("/register")
    public synchronized ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegisterDto userDto) {
        // 1. Manually map the register DTO to a full UserDto that your service expects
        UserDto dto = new UserDto();
        dto.setName(userDto.name());
        dto.setEmail(userDto.email());
        dto.setPassword(userDto.password());

        // 2. Now pass the correctly typed 'dto' (which is a UserDto)
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(dto));
    }

    @GetMapping("/validate")
    public ResponseEntity<UserDto> validateSession(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return userRepository.findByEmail(authentication.getName())
                .map(user -> ResponseEntity.ok(modelMapper.map(user, UserDto.class)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}