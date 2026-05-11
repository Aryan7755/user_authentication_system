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

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        // check 1 - empty email
        if(userDto.getEmail()==null || userDto.getEmail().isBlank()){
            throw  new IllegalArgumentException("Email is required");
        }
        // check 2 - existing email
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("User with this email already exists");
        }
        //convert userDto into User class
        User user = modelMapper.map(userDto, User.class);
        //set provider
        user.setProvider((userDto.getProvider()!=null)?userDto.getProvider(): Provider.LOCAL);
        //save the user
        User savedUser = userRepository.save(user);
        //return dto obj of saved user
        return modelMapper.map(savedUser,UserDto.class);

    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not present with given Email id"));

        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uuid = UserHelper.parseUUID(userId);
        User existingUser = userRepository.findById(uuid)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with the given Id"));
        //email is not allowed to change
        if(userDto.getName()!=null)existingUser.setName(userDto.getName());
        if(userDto.getImage()!=null)existingUser.setImage(userDto.getImage());
        if(userDto.getProvider()!=null)existingUser.setProvider(userDto.getProvider());
        existingUser.setEnabled(userDto.isEnabled());
        existingUser.setUpdatedAt(Instant.now());
        //TODO: change the password updation logic
        if(userDto.getPassword()!=null)existingUser.setPassword(userDto.getPassword());

        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User user  = userRepository.findById(uId).orElseThrow(()->new ResourceNotFoundException("User not found with the given Id"));
        userRepository.delete(user);

    }

    @Override
    public UserDto getUserById(String userId) {
        User user  = userRepository.findById(UserHelper.parseUUID(userId)).orElseThrow(()->new ResourceNotFoundException("User not found with the given Id"));
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map( user -> modelMapper.map(user,UserDto.class))
                .toList();
    }

}
