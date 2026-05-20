package com.aryan.project7.dtos;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// This is our standard "oops" response.
// Whenever something goes wrong, we send this back so the frontend knows exactly what happened.
public record ApiError(
        int status,            // The HTTP status code (like 404 or 500)
        String error,          // A short error title (like "Not Found")
        String message,        // The human-friendly explanation of the mistake
        String path,           // The URL the user was trying to hit
        OffsetDateTime timeStamp // Exactly when this happened
) {

    // Quick helper to build an error with the current time in UTC
    public static ApiError of(int status,
                              String error,
                              String message,
                              String path){
        return new ApiError(status, error, message, path, OffsetDateTime.now(ZoneOffset.UTC));
    }

    // This one is for when we don't want a timestamp (maybe for simpler logs or specific client needs)
    public static ApiError of(int status,
                              String error,
                              String message,
                              String path,
                              boolean notDateTime){
        return new ApiError(status, error, message, path, null);
    }
}