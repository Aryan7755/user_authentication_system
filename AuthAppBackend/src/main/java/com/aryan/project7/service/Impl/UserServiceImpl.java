package com.aryan.project7.service.Impl;

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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    // This handles the heavy lifting of onboarding a new user
    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        // We can't have a user without an email—that's our main way of identifying them
        if(userDto.getEmail() == null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }

        // Double-check they aren't already in the system so we don't get duplicates
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Map the DTO over to our Entity so JPA can handle it
        User user = modelMapper.map(userDto, User.class);

        // If no provider is specified, we assume they're signing up directly with us
        user.setProvider((userDto.getProvider() != null) ? userDto.getProvider() : Provider.LOCAL);

        User savedUser = userRepository.save(user);

        // Send back the saved user as a DTO (best practice: never return the Entity itself)
        return modelMapper.map(savedUser, UserDto.class);
    }

    // Quick lookup by email—handy for the login flow
    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not present with given Email id"));

        return modelMapper.map(user, UserDto.class);
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
        if(userDto.getPassword() != null) existingUser.setPassword(userDto.getPassword());

        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
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
        return modelMapper.map(user, UserDto.class);
    }

    // Grabs every user—usually just for admin dashboards
    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}