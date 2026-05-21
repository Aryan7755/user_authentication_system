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

    // This handles the logic for a brand-new user signing up
    @Override
    public UserDto registerUser(UserDto userDto) {

        // Security 101: Never store plain-text passwords.
        // We hash it here before it ever gets near the database.
        String encodedPassword = passwordEncoder.encode(userDto.getPassword());
        userDto.setPassword(encodedPassword);

        // Now that the password is safe, we hand it off to the regular user service to save it
        return userService.createUser(userDto);
    }
    //This method nulls the hashed password before sending dtos through endpoints

}