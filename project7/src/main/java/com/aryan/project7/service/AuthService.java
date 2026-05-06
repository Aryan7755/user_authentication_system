package com.aryan.project7.service;

import com.aryan.project7.dtos.UserDto;
import com.aryan.project7.model.User;

public interface AuthService {
    UserDto registerUser(UserDto userDto);
}
