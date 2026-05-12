package com.aryan.project7.service;

import com.aryan.project7.dtos.UserDto;

/**
 * The UserService interface defines the core business logic
 * for managing user accounts in the system.
 */
public interface UserService {

    // Logic for registering or manually adding a new user
    UserDto createUser(UserDto userDto);

    // Finding a user by their unique email (essential for login)
    UserDto getUserByEmail(String email);

    // Modifying existing profile details like name or image
    UserDto updateUser(UserDto userDto, String userId);

    // Removing a user from the system entirely
    void deleteUser(String userId);

    // Finding a specific user by their database UUID
    UserDto getUserById(String userId);

    // Grabbing the whole list—perfect for an admin dashboard
    Iterable<UserDto> getAllUsers();
}