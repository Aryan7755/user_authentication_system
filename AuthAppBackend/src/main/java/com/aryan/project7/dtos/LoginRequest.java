package com.aryan.project7.dtos;

// This is the simple data packet we expect when someone tries to log in
public record LoginRequest(
        String email,     // The user's account email
        String password   // Their plain-text password (which we'll hash and check later)
) {
}