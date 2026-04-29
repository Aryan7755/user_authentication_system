package com.aryan.project7.repository;

import com.aryan.project7.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Used during Login to find the user trying to authenticate.
     * We use Optional to avoid NullPointerExceptions if the user isn't found.
     */
    Optional<User> findByUsername(String username);

    /**
     * Used during Registration to check if a username is already taken.
     */
    Boolean existsByUsername(String username);

    /**
     * Used during Registration to check if an email is already registered.
     */
    Boolean existsByEmail(String email);
}