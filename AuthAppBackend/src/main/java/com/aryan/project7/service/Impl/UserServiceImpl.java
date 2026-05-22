package com.aryan.project7.service.Impl;

import com.aryan.project7.config.ValidationMessages;
import com.aryan.project7.dtos.UserDto;
import com.aryan.project7.exception.ResourceNotFoundException;
import com.aryan.project7.helper.UserHelper;
import com.aryan.project7.entity.Provider;
import com.aryan.project7.entity.User;
import com.aryan.project7.repository.UserRepository;
import com.aryan.project7.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    // This handles the heavy lifting of onboarding a new user
    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        // We can't have a user without an email—that's our main way of identifying them
        if(userDto.getEmail() == null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException(ValidationMessages.EMAIL_REQUIRED);
        }

        // Double-check they aren't already in the system so we don't get duplicates
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException(ValidationMessages.EMAIL_ALREADY_EXISTS);
        }

        // Map the DTO over to our Entity so JPA can handle it
        User user = modelMapper.map(userDto, User.class);

        // If no provider is specified, we assume they're signing up directly with us
        user.setProvider((userDto.getProvider() != null) ? userDto.getProvider() : Provider.LOCAL);

        User savedUser = userRepository.save(user);

        UserDto responseDto = modelMapper.map(savedUser, UserDto.class);
        responseDto.setPassword(null);

        return responseDto;
    }

    // Quick lookup by email—handy for the login flow
    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not present with given Email id"));

        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setPassword(null);
        return dto;
    }

    // Updates an existing user's profile
    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uuid = UserHelper.parseUUID(userId);
        User existingUser = userRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with the given Id"));

        // We only update fields that were actually sent in the request
        if(userDto.getName() != null) existingUser.setName(userDto.getName());
        if(userDto.getImage() != null) existingUser.setImage(userDto.getImage());
        if(userDto.getProvider() != null) existingUser.setProvider(userDto.getProvider());

        existingUser.setEnabled(userDto.isEnabled());
        existingUser.setUpdatedAt(Instant.now());

        // TODO: This password logic needs a second look later to make sure we hash it correctly here too!
        if(userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            // Only encode if it doesn't look like a hash (BCrypt starts with $2a$, $2b$, $2y$)
            String pwd = userDto.getPassword();
            if (!pwd.startsWith("$2") && !pwd.startsWith("$y$")) {
                existingUser.setPassword(passwordEncoder.encode(pwd));
            }
            //existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        User updatedUser = userRepository.save(existingUser);
        UserDto dto = modelMapper.map(updatedUser, UserDto.class);
        dto.setPassword(null);  // ← ADD THIS
        return dto;
    }

    // Wipes a user from the database
    @Override
    public void deleteUser(String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with the given Id"));
        userRepository.delete(user);
    }

    // Find a single user by their primary key
    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with the given Id"));
        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setPassword(null);  // ← ADD THIS
        return dto;
    }

    // Grabs every user—usually just for admin dashboards
    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .peek(user -> user.getRoles().size())
                .map(user -> {
                    UserDto dto = modelMapper.map(user, UserDto.class);
                    dto.setPassword(null);  // ← ADD THIS
                    return dto;
                })
                .toList();
    }
}