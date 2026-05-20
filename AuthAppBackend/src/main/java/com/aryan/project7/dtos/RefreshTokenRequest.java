package com.aryan.project7.dtos;

// Simple wrapper for when the frontend needs to send us a refresh token to get a new access token
public record RefreshTokenRequest(
        String refreshToken // The long-lived token used to swap for a fresh access token
) {

}