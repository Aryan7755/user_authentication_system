package com.aryan.project7.service.Impl;

import com.aryan.project7.dtos.UserDto;
import com.aryan.project7.service.AuthService;
import com.aryan.project7.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto registerUser(UserDto userDto) {
        // Strict validation: Prevent empty passwords from reaching the DB
        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Registration failed: Password cannot be blank.");
        }

        // Hash before processing
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

        // Save via UserService
        return userService.createUser(userDto);
    }
}