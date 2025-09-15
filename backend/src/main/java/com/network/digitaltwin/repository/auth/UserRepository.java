package com.network.digitaltwin.repository.auth;

import com.network.digitaltwin.model.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Find user by username.
     * @param username the username
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     * @param email the email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String username);

    /**
     * Check if username exists.
     * @param username the username
     * @return true if exists, false otherwise
     */
    Boolean existsByUsername(String username);

    /**
     * Check if email exists.
     * @param email the email
     * @return true if exists, false otherwise
     */
    Boolean existsByEmail(String email);
}
