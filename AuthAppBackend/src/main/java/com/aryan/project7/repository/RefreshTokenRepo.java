package com.aryan.project7.repository;

import com.aryan.project7.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// This is our direct line to the "refresh_tokens" table in the database
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, UUID> {

    // We use the JTI (JWT ID) to look up tokens instead of the database UUID.
    // This is the key part of our "Token Rotation" logic—it helps us find
    // the exact token we need to revoke or refresh.
    Optional<RefreshToken> findByJti(String jti);
}