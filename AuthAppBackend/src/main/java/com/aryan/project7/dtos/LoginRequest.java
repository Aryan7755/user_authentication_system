package com.aryan.project7.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// This is the simple data packet we expect when someone tries to log in
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}