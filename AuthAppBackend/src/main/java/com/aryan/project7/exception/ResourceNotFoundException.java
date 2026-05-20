package com.aryan.project7.exception;

// We use this whenever we try to find something (like a User or a Token)
// that doesn't exist in our database.
public class ResourceNotFoundException extends RuntimeException {

    // This one lets us pass a specific message, like "User with ID 123 not found"
    public ResourceNotFoundException(String s) {
        super(s);
    }

    // A generic fallback just in case we're feeling lazy and don't want to write a custom message
    public ResourceNotFoundException(){
        super("Resource Not Found!");
    }
}