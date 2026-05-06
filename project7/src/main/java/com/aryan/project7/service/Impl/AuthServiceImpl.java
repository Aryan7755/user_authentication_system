package com.aryan.project7.service.Impl;

import com.aryan.project7.dtos.UserDto;
import com.aryan.project7.model.User;
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
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        UserDto userDto1 =  userService.createUser(userDto);
        return userDto1;
    }
}
