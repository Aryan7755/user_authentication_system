package com.aryan.project7.entity;

// This just tracks how the user signed up or logged in
public enum Provider {
    // The standard way: email and password stored in our own database
    LOCAL,

    // Social logins—no password stored on our end for these
    GOOGLE,
    GITHUB
}