package com.aryan.project7.service.Impl;

import com.aryan.project7.config.ValidationMessages;
import com.aryan.project7.dtos.UserDto;
import com.aryan.project7.entity.Provider;
import com.aryan.project7.entity.Role;
import com.aryan.project7.entity.User;
import com.aryan.project7.exception.ResourceNotFoundException;
import com.aryan.project7.helper.UserHelper;
import com.aryan.project7.repository.RefreshTokenRepo;
import com.aryan.project7.repository.RoleRepository;
import com.aryan.project7.repository.UserRepository;
import com.aryan.project7.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepo refreshTokenRepo;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) throw new IllegalArgumentException(ValidationMessages.EMAIL_REQUIRED);
        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) throw new IllegalArgumentException("Password is required");
        if (userRepository.existsByEmail(userDto.getEmail())) throw new IllegalArgumentException(ValidationMessages.EMAIL_ALREADY_EXISTS);

        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
        user.setPassword(userDto.getPassword()); // Assumes pre-encoded or handled by AuthController
        user.setEnabled(true);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found in database"));

        if (user.getRoles() == null) user.setRoles(new HashSet<>());
        user.getRoles().add(userRole);

        return mapToDto(userRepository.save(user));
    }

    @Override
    public UserDto getUserByEmail(String email) {
        return mapToDto(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not present with given Email id")));
    }

    @Override
    @Transactional
    public UserDto updateUser(UserDto userDto, String userId) {
        User existingUser = userRepository.findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with the given Id"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // FIX: Safely retrieve email from Principal
        String currentLoggedInEmail;
        if (authentication.getPrincipal() instanceof User userPrincipal) {
            currentLoggedInEmail = userPrincipal.getEmail();
        } else {
            currentLoggedInEmail = authentication.getName();
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!existingUser.getEmail().equals(currentLoggedInEmail) && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to update this profile");
        }

        if (userDto.getName() != null && !userDto.getName().isBlank()) existingUser.setName(userDto.getName());
        if (userDto.getImage() != null) existingUser.setImage(userDto.getImage());
        if (userDto.getProvider() != null) existingUser.setProvider(userDto.getProvider());
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));

        existingUser.setEnabled(userDto.isEnabled());
        existingUser.setUpdatedAt(Instant.now());

        return mapToDto(userRepository.save(existingUser));
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        UUID uuid = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uuid).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenRepo.deleteByUser_Id(uuid);
        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userId) {
        return mapToDto(userRepository.findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @Override
    @Transactional
    public List<UserDto> getAllUsers() {
        // FIX: Removed .peek() - @EntityGraph in UserRepository.findAll() handles this
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserDto mapToDto(User user) {
        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setPassword(null);
        return dto;
    }
}