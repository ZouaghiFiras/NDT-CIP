package com.network.digitaltwin.controller.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.network.digitaltwin.dto.auth.JwtResponse;
import com.network.digitaltwin.dto.auth.LoginRequest;
import com.network.digitaltwin.dto.auth.RegisterRequest;
import com.network.digitaltwin.dto.response.ApiResponse;
import com.network.digitaltwin.service.auth.AuthService;

import jakarta.validation.Valid;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("User login attempt: {}", loginRequest.getUsername());

        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);

        logger.info("User logged in successfully: {}", loginRequest.getUsername());

        return ResponseEntity.ok(
            ApiResponse.success(
                "User signed in successfully",
                jwtResponse
            )
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        logger.info("User registration attempt: {}", signUpRequest.getUsername());

        authService.registerUser(signUpRequest);

        logger.info("User registered successfully: {}", signUpRequest.getUsername());

        return ResponseEntity.ok(
            ApiResponse.success(
                "User registered successfully!",
                "User registered successfully!"
            )
        );
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestBody String refreshToken) {
        logger.info("Token refresh attempt");

        JwtResponse jwtResponse = authService.refreshToken(refreshToken);

        logger.info("Token refreshed successfully");

        return ResponseEntity.ok(
            ApiResponse.success(
                "Token refreshed successfully",
                jwtResponse
            )
        );
    }

    @PostMapping("/signout")
    public ResponseEntity<ApiResponse<Void>> logoutUser() {
        // In a stateless JWT application, logout is typically handled on the client side
        // by removing the token. Server-side logout is not implemented as the server
        // doesn't maintain session state.

        logger.info("User logout");

        return ResponseEntity.ok(
            ApiResponse.success(
                "User signed out successfully",
                null
            )
        );
    }
}
