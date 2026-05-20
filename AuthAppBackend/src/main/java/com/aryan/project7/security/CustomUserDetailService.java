package com.aryan.project7.security;

import com.aryan.project7.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// This service is what Spring Security calls behind the scenes during the login process.
// Its only job is to go fetch the user data from the database.
@RequiredArgsConstructor
@Service
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    // When someone types their email into the login form, Spring calls this method.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // We look for the user by email since that's what we use as their unique 'username'
        return userRepository
                .findByEmail(username)
                .orElseThrow(() -> {
                    // We throw a BadCredentialsException here to keep things vague.
                    // Security-wise, it's better not to tell a hacker "Hey, that email exists but the password was wrong."
                    return new BadCredentialsException("Invalid email or password!");
                });
    }
}