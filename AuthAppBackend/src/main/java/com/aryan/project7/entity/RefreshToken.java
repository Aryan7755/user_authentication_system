package com.aryan.project7.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

// This table is our security net for long-lived sessions
@Entity
@Table(name="refresh_tokens", indexes = {
        // We index the JTI because we'll be looking it up every time someone refreshes their token
        @Index(name = "refresh_tokens_jti_idx", columnList = "jti", unique = true),
        // Indexing the user_id helps when we want to clear all tokens for a specific person (like a "logout from all devices")
        @Index(name = "refresh_tokens_user_id_idx", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The "JWT ID" — a unique identifier for this specific token to prevent reuse
    @Column(name = "jti", unique = true, nullable = false, updatable = false)
    private String jti;

    // The user who owns this token. We use LAZY fetch so we don't grab user data unless we actually need it.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    // If this is true, the token is dead to us (even if it hasn't expired yet)
    @Column(nullable = false)
    private boolean revoked;

    // This is great for "Token Rotation"—it points to the next token in the chain
    // to help detect if a malicious actor is trying to use an old token.
    private String replacedByToken;
}