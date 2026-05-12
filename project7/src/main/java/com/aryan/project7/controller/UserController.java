package com.aryan.project7.controller;

import com.aryan.project7.dtos.UserDto;
import com.aryan.project7.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    // Creates a brand new user from scratch
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
    }

    // Grab everyone in the database
    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Look up a specific person using their unique ID
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId){
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    // Sometimes we only have an email to go off of, so this is a handy lookup
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email){
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    // Wipe a user from the system based on their ID
    @DeleteMapping("/{userId}")
    public void deleteUserById(@PathVariable String userId){
        userService.deleteUser(userId);
    }

    // Update an existing user's details—we take the ID from the URL and the new data from the body
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUserById(@RequestBody UserDto userDto, @PathVariable String userId){
        return ResponseEntity.ok(userService.updateUser(userDto, userId));
    }
}