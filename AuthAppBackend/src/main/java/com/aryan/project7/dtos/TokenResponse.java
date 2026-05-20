package com.aryan.project7.dtos;

// This is the final package we send back when someone logs in or refreshes their session.
// It’s got the keys to the kingdom and a bit of info about who just logged in.
public record TokenResponse (
        String accessToken,  // The short-lived key for accessing protected APIs
        String refreshToken, // The long-lived key used to get a new access token
        Long expiresIn,      // Telling the frontend exactly how many seconds until the access token dies
        String tokenType,    // Usually just "Bearer"
        UserDto userDto      // The user's profile details so the UI can show their name/avatar immediately
){

    // A handy static helper to build the response without having to type "Bearer" every single time
    public static TokenResponse of(String accessToken,
                                   String refreshToken,
                                   Long expiresIn,
                                   UserDto userDto){
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer", userDto);
    }
}