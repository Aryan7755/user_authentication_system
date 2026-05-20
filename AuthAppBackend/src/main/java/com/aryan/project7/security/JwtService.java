package com.aryan.project7.security;

import com.aryan.project7.entity.Role;
import com.aryan.project7.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// This service is the "Master Key" maker for the app.
// It handles everything related to creating and reading JWTs.
@Service
@Getter
@Setter
public class JwtService {
    private final SecretKey secretKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.access-ttl-seconds}") long accessTtlSeconds,
                      @Value("${security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
                      @Value("${security.jwt.issuer}") String issuer) {

        // Security check: if the secret is too short, the HS512 algorithm will complain,
        // so we catch it early.
        if (secret == null || secret.length() < 64)
            throw new IllegalArgumentException("Your JWT secret needs to be at least 64 characters long!");

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
    }

    // Creates a short-lived token that the user sends with every API request
    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        // We pack the roles into the token so the "JwtAuthenticationFilter"
        // doesn't have to hit the database to know what the user is allowed to do.
        List<String> roles = user.getRoles() == null ? List.of() :
                user.getRoles().stream().map(Role::getName).toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claims(Map.of(
                        "email", user.getEmail(),
                        "roles", roles,
                        "typ", "access" // This label prevents using this token as a refresh token
                ))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // Creates a long-lived token used only to get a new access token
    public String generateRefreshToken(User user, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti) // We use the JTI from the database so we can revoke it if needed
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claims(Map.of(
                        "typ", "refresh"
                ))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // This method cracks open the token and verifies the signature
    public Jws<Claims> parse(String token) {
        return Jwts.parser().setSigningKey(secretKey).build().parseClaimsJws(token);
    }

    // Helper to check if a token is an "access" type
    public boolean isAccessToken(String token) {
        Claims c = parse(token).getPayload();
        return "access".equals(c.get("typ"));
    }

    // Helper to check if a token is a "refresh" type
    public boolean isRefreshToken(String token) {
        Claims c = parse(token).getPayload();
        return "refresh".equals(c.get("typ"));
    }

    // Extracts the user ID from the token subject
    public UUID getUserId(String token) {
        Claims c = parse(token).getPayload();
        return UUID.fromString(c.getSubject());
    }

    // Gets the unique ID of the token itself
    public String getJti(String token) {
        return parse(token).getPayload().getId();
    }
}