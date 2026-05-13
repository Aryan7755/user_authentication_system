package com.aryan.project7.dtos;

import java.util.UUID;

// This represents a user's role (like ADMIN or USER) when sending data back and forth
public class RoleDto {

    // The unique ID for the role
    private UUID id;

    // The actual role name—usually something like "ROLE_USER" or "ROLE_ADMIN"
    private String name;
}