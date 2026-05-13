package com.aryan.project7.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

// This table maps out the different permission levels like ADMIN or USER
@Entity
@Table(name = "user_role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id")
    private UUID id; // JPA will handle generating this ID for us

    // This is the actual role name. We make it unique so we don't end up with
    // two "ADMIN" roles accidentally.
    @Column(unique = true, nullable = false)
    private String name;
}