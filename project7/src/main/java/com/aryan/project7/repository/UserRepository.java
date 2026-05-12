package com.aryan.project7.repository;

import com.aryan.project7.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

// This is our gateway to the 'users' table
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // We use this constantly during login to see if a user exists with that email
    // It returns an Optional so we can handle "user not found" cases gracefully
    Optional<User> findByEmail(String email);

    // Super handy for the registration flow—it lets us check if an email is
    // already taken before we try to create a new account
    boolean existsByEmail(String email);

}