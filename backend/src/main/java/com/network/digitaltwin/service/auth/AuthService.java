package com.network.digitaltwin.service.auth;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.network.digitaltwin.dto.auth.JwtResponse;
import com.network.digitaltwin.dto.auth.LoginRequest;
import com.network.digitaltwin.dto.auth.RegisterRequest;
import com.network.digitaltwin.model.auth.Role;
import com.network.digitaltwin.model.auth.User;
import com.network.digitaltwin.repository.auth.UserRepository;
import com.network.digitaltwin.utils.JwtUtils;

/**
 * Service for handling authentication operations.
 */
@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthService(AuthenticationManager authenticationManager, 
                     UserRepository userRepository,
                     PasswordEncoder encoder,
                     JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Register a new user.
     * @param signUpRequest the registration request
     * @return registered user
     */
    @Transactional
    public User registerUser(RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());

        Set<String> strRoles = new HashSet<>();
        strRoles.add("user");

        Set<Role> roles = new HashSet<>();

        if (strRoles.isEmpty()) {
            roles.add(Role.ROLE_USER);
        } else {
            strRoles.forEach(role -> {
                if ("admin".equals(role)) {
                    roles.add(Role.ROLE_ADMIN);
                } else {
                    roles.add(Role.ROLE_USER);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        logger.info("User registered successfully: {}", user.getUsername());

        return user;
    }

    /**
     * Authenticate user and generate JWT token.
     * @param loginRequest the login request
     * @return JWT response
     */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(), 
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String username = userDetails.getUsername();

        // Update last login time
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setLastLoginTime(java.time.Instant.now());
            userRepository.save(user);
        }

        return new JwtResponse(
                jwt,
                "Bearer",
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .toArray(String[]::new));
    }

    /**
     * Refresh JWT token.
     * @param refreshToken the refresh token
     * @return JWT response
     */
    public JwtResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
        UserDetailsImpl userDetails = (UserDetailsImpl) 
                org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("") // Password is not needed for refresh
                .authorities(userRepository.findByUsername(username)
                        .map(u -> u.getRoles())
                        .orElseThrow(() -> new RuntimeException("User not found")))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String jwt = jwtUtils.generateJwtToken(authentication);

        return new JwtResponse(
                jwt,
                "Bearer",
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .toArray(String[]::new));
    }
}
