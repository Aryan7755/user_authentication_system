package com.aryan.project7.repository;

import com.aryan.project7.entity.RefreshToken;
import com.aryan.project7.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// This is our direct line to the "refresh_tokens" table in the database
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, UUID> {

    // We use the JTI (JWT ID) to look up tokens instead of the database UUID.
    Optional<RefreshToken> findByJti(String jti);

    // FIX: Added @Modifying and @Transactional to allow the DELETE query to execute
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < CURRENT_TIMESTAMP OR r.revoked = true")
    void cleanupExpiredTokens();

    void deleteByUser_Id(UUID userId);

    // To this (find all active, ordered by creation date descending):
    List<RefreshToken> findByUserAndRevokedFalseOrderByCreatedAtDesc(User user);

}