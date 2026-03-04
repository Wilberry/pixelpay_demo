package com.pixelwallet.controller;

import com.pixelwallet.dto.AuthRequestDTO;
import com.pixelwallet.dto.AuthResponseDTO;
import com.pixelwallet.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController handles all authentication-related HTTP requests.
 * <p>
 * Provides endpoints for user registration and login, returning JWT tokens
 * for authenticated clients. All credentials are validated and passwords are
 * securely hashed using BCrypt.
 * </p>
 * <p>
 * <strong>Security:</strong> Credentials are transmitted over HTTPS (in production).
 * Passwords are never logged or returned in responses.
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Constructs AuthController with required AuthService dependency.
     *
     * @param authService Service layer for authentication operations
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates user with email and password, returns JWT token.
     * <p>
     * <strong>Request:</strong> POST /api/auth/login<br>
     * <strong>Response:</strong> 200 OK with JWT token<br>
     * <strong>Errors:</strong> 401 Unauthorized if credentials invalid
     * </p>
     *
     * @param request DTO containing email and password credentials
     * @return ResponseEntity with {@link AuthResponseDTO} containing accessToken, tokenType, and expiresIn
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers new user account and creates associated wallet.
     * <p>
     * <strong>Request:</strong> POST /api/auth/register?firstName=John&lastName=Doe<br>
     * <strong>Response:</strong> 201 Created with JWT token<br>
     * <strong>Errors:</strong> 400 Bad Request if email already registered
     * </p>
     * <p>
     * On successful registration:
     * <ul>
     *   <li>New User entity is created with encrypted password</li>
     *   <li>Associated Wallet is created with initial balance of $1000</li>
     *   <li>User is automatically logged in (JWT token returned)</li>
     * </ul>
     * </p>
     *
     * @param request DTO containing email and password (password must be 8-128 characters)
     * @param firstName User's first name (optional, defaults to empty)
     * @param lastName User's last name (optional, defaults to empty)
     * @return ResponseEntity with {@link AuthResponseDTO} containing JWT token
     * @throws IllegalArgumentException if email already registered
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody AuthRequestDTO request,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName) {
        
        log.info("Registration request for email: {}", request.getEmail());
        AuthResponseDTO response = authService.register(request, firstName, lastName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Health check endpoint for service availability monitoring.
     * <p>
     * Returns 200 OK if service is running. Can be called without authentication.
     * </p>
     *
     * @return Simple success message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is healthy");
    }
}
