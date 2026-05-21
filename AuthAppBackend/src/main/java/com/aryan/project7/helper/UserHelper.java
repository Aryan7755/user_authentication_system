package com.aryan.project7.helper;

import java.util.UUID;

// A handy helper to deal with UUID conversions without cluttering the main logic
public class UserHelper {

    // Converts a standard String ID into a UUID object that JPA and Postgres love
    public static UUID parseUUID(String uId) {
        if (uId == null || uId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        try {
            return UUID.fromString(uId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uId, e);
        }
    }
}