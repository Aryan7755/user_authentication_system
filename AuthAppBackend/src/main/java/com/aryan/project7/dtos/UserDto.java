package com.aryan.project7.dtos;

import com.aryan.project7.entity.Provider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// This is the main data object we use to pass user information around the app
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private UUID id;
    private String name;
    private String email;

    // We keep the password here for registration, but usually, we'd null this out
    // before sending the user data back to the frontend for security
    private String password;

    // A URL or path to the user's profile picture
    private String image;

    // Is the account active? Defaults to true because we like new users.
    private boolean enabled = true;

    // Tracking when the user joined and when they last changed their info
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    // Tells us if they signed up normally (LOCAL) or via something like Google/GitHub
    @Enumerated(EnumType.STRING)
    private Provider provider = Provider.LOCAL;

    // A list of what this user is allowed to do (ADMIN, USER, etc.)
    private Set<RoleDto> roles = new HashSet<>();
}