package com.pixelwallet.service;

import com.pixelwallet.dto.AuthRequestDTO;
import com.pixelwallet.dto.AuthResponseDTO;
import com.pixelwallet.exception.UserNotFoundException;
import com.pixelwallet.model.User;
import com.pixelwallet.model.Wallet;
import com.pixelwallet.repository.UserRepository;
import com.pixelwallet.repository.WalletRepository;
import com.pixelwallet.security.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * AuthService handles user authentication and registration operations.
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>User login with email/password authentication</li>
 *   <li>User registration with automatic wallet creation</li>
 *   <li>JWT token generation and validation</li>
 *   <li>Password encoding using BCrypt</li>
 *   <li>Initial wallet funding for new users</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Security Features:</strong>
 * <ul>
 *   <li>Passwords are BCrypt-hashed (one-way encryption)</li>
 *   <li>JWT tokens expire after 24 hours</li>
 *   <li>Duplicate email registration is prevented</li>
 *   <li>Atomic registration (user + wallet creation)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Transaction Management:</strong>
 * Registration operations use @Transactional to ensure atomicity:
 * either both user and wallet are created, or neither is.
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs AuthService with required dependencies.
     *
     * @param userRepository Repository for user data access
     * @param walletRepository Repository for wallet data access
     * @param authenticationManager Spring Security authentication manager
     * @param jwtProvider JWT token generation and validation service
     * @param passwordEncoder BCrypt password encoder
     */
    public AuthService(UserRepository userRepository, WalletRepository walletRepository,
                       AuthenticationManager authenticationManager, JwtProvider jwtProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates user with email and password, returns JWT token.
     * <p>
     * <strong>Authentication Process:</strong>
     * <ol>
     *   <li>Validate email/password credentials using Spring Security</li>
     *   <li>Generate JWT token with user identity and expiration</li>
     *   <li>Return token and expiration time in response</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Security:</strong> Invalid credentials throw AuthenticationException
     * which is handled by GlobalExceptionHandler and returns HTTP 401.
     * </p>
     * <p>
     * <strong>JWT Token:</strong> Contains user email as subject, expires in 24 hours.
     * Token can be used for subsequent authenticated API calls.
     * </p>
     *
     * @param request DTO containing email and password credentials
     * @return AuthResponseDTO with JWT access token and expiration time
     * @throws org.springframework.security.core.AuthenticationException if credentials invalid
     */
    public AuthResponseDTO login(AuthRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String accessToken = jwtProvider.generateToken(authentication);
        long expirationTime = 24 * 60 * 60; // 24 hours in seconds

        log.info("User logged in: {}", request.getEmail());

        return new AuthResponseDTO(accessToken, expirationTime);
    }

    /**
     * Registers a new user account with automatic wallet creation.
     * <p>
     * <strong>Registration Process:</strong>
     * <ol>
     *   <li>Validate email is not already registered</li>
     *   <li>Create User entity with BCrypt-hashed password</li>
     *   <li>Create Wallet entity with $1000 initial balance</li>
     *   <li>Associate wallet with user</li>
     *   <li>Automatically authenticate user and return JWT token</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Atomicity:</strong> Uses @Transactional to ensure both user and wallet
     * are created successfully, or both operations are rolled back.
     * </p>
     * <p>
     * <strong>Initial Balance:</strong> New users receive $1000 USD to enable
     * immediate transfers and testing of the system.
     * </p>
     * <p>
     * <strong>Duplicate Prevention:</strong> Throws IllegalArgumentException if
     * email is already registered.
     * </p>
     *
     * @param request DTO containing email and password for registration
     * @param firstName User's first name (optional, defaults to empty)
     * @param lastName User's last name (optional, defaults to empty)
     * @return AuthResponseDTO with JWT token for immediate authentication
     * @throws IllegalArgumentException if email is already registered
     */
    @Transactional
    public AuthResponseDTO register(AuthRequestDTO request, String firstName, String lastName) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Create user entity
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .firstName(firstName)
                .lastName(lastName)
                .build();

        User savedUser = userRepository.save(user);

        // Create associated wallet with initial balance
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(BigDecimal.valueOf(1000)) // Initial credit
                .currency("USD")
                .build();

        walletRepository.save(wallet);
        user.setWallet(wallet);
        userRepository.save(user);

        log.info("User registered successfully: {}", request.getEmail());

        // Automatically authenticate after registration
        return login(request);
    }

    /**
     * Validates whether a JWT token is valid and not expired.
     * <p>
     * Checks token signature, expiration time, and format validity.
     * Used by security filters to authenticate incoming requests.
     * </p>
     *
     * @param token JWT token string to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        return jwtProvider.validateToken(token);
    }

    /**
     * Extracts user email from a valid JWT token.
     * <p>
     * Parses the JWT token and returns the email claim (subject).
     * Used to identify the authenticated user for authorization.
     * </p>
     *
     * @param token JWT token string
     * @return Email address of the token's subject
     * @throws IllegalArgumentException if token is invalid or malformed
     */
    public String getUserEmailFromToken(String token) {
        return jwtProvider.getEmailFromToken(token);
    }
}
