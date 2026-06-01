package com.aryan.project7.repository;

import com.aryan.project7.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// This is our gateway to the 'users' table
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // FIX: Added @EntityGraph here! This is used by the Authentication process.
    // Without this, loading roles during login will likely throw LazyInitializationException.
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    // FIX: Added @EntityGraph here to prevent N+1 queries when fetching all users.
    @Override
    @EntityGraph(attributePaths = "roles")
    List<User> findAll();

    // Super handy for the registration flow—it lets us check if an email is
    // already taken before we try to create a new account
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(UUID id);
}