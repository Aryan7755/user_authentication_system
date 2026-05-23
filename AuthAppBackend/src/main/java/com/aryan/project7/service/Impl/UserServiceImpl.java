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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

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

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    ValidationMessages.EMAIL_REQUIRED
            );
        }

        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException(
                    ValidationMessages.EMAIL_ALREADY_EXISTS
            );
        }

        User user = modelMapper.map(userDto, User.class);

        user.setProvider(
                userDto.getProvider() != null
                        ? userDto.getProvider()
                        : Provider.LOCAL
        );

        // Password was already encoded in AuthController
        user.setPassword(userDto.getPassword());
        user.setEnabled(true);

        // FIX: Fetch the USER role and attach it
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found in database"));

        // Initialize the roles set if it's null, then add the role
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(userRole);

        // Save the user (Hibernate will automatically populate the user_roles join table!)
        User savedUser = userRepository.save(user);

        return mapToDto(savedUser);
    }

    @Override
    public UserDto getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not present with given Email id"
                        )
                );

        return mapToDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(UserDto userDto, String userId) {

        UUID uuid = UserHelper.parseUUID(userId);

        User existingUser = userRepository.findById(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with the given Id"
                        )
                );

        // --- NEW SECURITY CHECK START ---
        // 1. Get the currently logged-in user from the JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentLoggedInEmail = authentication.getName();

        // 2. Check if they are an ADMIN (Admins can edit anyone)
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 3. If they aren't the owner AND aren't an admin, kick them out!
        if (!existingUser.getEmail().equals(currentLoggedInEmail) && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to update this profile");
        }
        // --- NEW SECURITY CHECK END ---


        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            existingUser.setName(userDto.getName());
        }

        if (userDto.getImage() != null) {
            existingUser.setImage(userDto.getImage());
        }

        if (userDto.getProvider() != null) {
            existingUser.setProvider(userDto.getProvider());
        }

        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            existingUser.setPassword(
                    passwordEncoder.encode(userDto.getPassword())
            );
        }

        existingUser.setEnabled(userDto.isEnabled());
        existingUser.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(existingUser);

        return mapToDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        UUID uuid = UserHelper.parseUUID(userId);

        User user = userRepository.findById(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with the given Id"
                        )
                );

        // 1. MUST DELETE TOKENS FIRST to avoid foreign key crash!
        refreshTokenRepo.deleteByUser_Id(uuid);

        // 2. Now safe to delete the user
        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userId) {

        User user = userRepository.findById(
                        UserHelper.parseUUID(userId)
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with the given Id"
                        )
                );

        return mapToDto(user);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .peek(user -> user.getRoles().size())
                .map(this::mapToDto)
                .toList();
    }

    /*
     * Centralized DTO mapper.
     * Prevents password leakage everywhere.
     */
    private UserDto mapToDto(User user) {

        UserDto dto = modelMapper.map(user, UserDto.class);

        dto.setPassword(null);

        return dto;
    }
}