package com.aryan.project7.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;

// This is our main User entity. It also doubles as a "UserDetails" object
// so Spring Security knows exactly how to handle it during login.
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "user_name", length = 500)
    private String name;

    @Column(name = "user_email", unique = true, length = 300)
    private String email;

    private String password;

    private String image;

    private boolean enabled = true;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    // Are they logging in with a local password, or via Google/GitHub?
    @Enumerated(EnumType.STRING)
    private Provider provider = Provider.LOCAL;

    // If they use OAuth2 (Google/GitHub), this stores their unique ID from that provider
    private String providerId;

    // We use EAGER here because we almost always need the user's roles
    // the moment they log in to check their permissions.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns  = @JoinColumn(name="user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // Just before saving a new user, we make sure the timestamps are set
    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now();
        if(createdAt == null) createdAt = now;
        updatedAt = now;
    }

    // Every time we update user info, we refresh the "updatedAt" timer
    @PreUpdate
    protected void onUpdate(){
        updatedAt = Instant.now();
    }

    // This converts our Role entities into the format Spring Security understands
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    // Spring Security calls this to get the "login name"—we use email for that
    @Override
    public String getUsername() {
        return this.email;
    }

    // For now, we're keeping these simple, but you could add logic later
    // to lock accounts after too many failed password attempts.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}