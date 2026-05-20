package com.aryan.project7.service;

import com.aryan.project7.dtos.UserDto;

/**
 * The AuthService defines the high-level security actions
 * a user can take, like signing up for a new account.
 */
public interface AuthService {

    // Takes in the user's details (name, email, plain password)
    // and returns the saved user profile (with the password hidden/hashed).
    UserDto registerUser(UserDto userDto);
}