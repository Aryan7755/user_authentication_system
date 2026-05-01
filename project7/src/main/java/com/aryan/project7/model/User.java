package com.aryan.project7.model;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.*;

import java.time.Instant;
import java.time.InstantSource;
import java.util.*;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;
    @Column(name = "user_name",length = 500)
    private String name;
    @Column(name = "user_email",unique = true,length = 300)
    private String email;

    private String password;

    private String image;
    private boolean enabled = true;
    private Instant createdAt=Instant.now();
    private Instant updatedAt=Instant.now();
    @Enumerated(EnumType.STRING)
    private Provider provider=Provider.LOCAL;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
                joinColumns  = @JoinColumn(name="user_id"),
                inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now();
        if(createdAt==null)createdAt=now;
        updatedAt=now;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = Instant.now();
    }
}
