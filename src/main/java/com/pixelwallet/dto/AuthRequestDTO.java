package com.pixelwallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user authentication requests.
 * <p>
 * <strong>Purpose:</strong>
 * Used for both user login and registration operations. Contains the minimum
 * required information to authenticate or create a user account.
 * </p>
 * <p>
 * <strong>Validation Rules:</strong>
 * <ul>
 *   <li>Email must be valid email format and non-blank</li>
 *   <li>Password must be 8-128 characters and non-blank</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Usage:</strong>
 * <ul>
 *   <li>POST /api/auth/login - User authentication</li>
 *   <li>POST /api/auth/register - User account creation</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Security Considerations:</strong>
 * <ul>
 *   <li>Password is transmitted in plain text (HTTPS required)</li>
 *   <li>Server-side validation prevents malicious input</li>
 *   <li>Passwords are BCrypt-hashed before storage</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Builder Pattern:</strong>
 * Provides fluent builder API for convenient object construction:
 * <pre>
 * AuthRequestDTO request = AuthRequestDTO.builder()
 *     .email("user@example.com")
 *     .password("securePassword123")
 *     .build();
 * </pre>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
public class AuthRequestDTO {

    /**
     * User's email address for authentication.
     * <p>
     * Must be a valid email format and is used as the unique identifier
     * for user accounts. Case-sensitive for lookup operations.
     * </p>
     */
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    /**
     * User's password for authentication.
     * <p>
     * Must be between 8 and 128 characters. Will be BCrypt-hashed
     * before storage during registration. Used for password verification
     * during login.
     * </p>
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    /**
     * Default constructor for frameworks and deserialization.
     */
    public AuthRequestDTO() {}

    /**
     * Constructor with all required fields.
     *
     * @param email User's email address
     * @param password User's password
     */
    public AuthRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Gets the user's email address.
     *
     * @return The email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email The email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's password.
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user's password.
     *
     * @param password The password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Creates a new builder instance for fluent construction.
     *
     * @return A new AuthRequestDTOBuilder instance
     */
    public static AuthRequestDTOBuilder builder() {
        return new AuthRequestDTOBuilder();
    }

    /**
     * Builder class for fluent AuthRequestDTO construction.
     * <p>
     * Provides a fluent API for creating AuthRequestDTO instances
     * with method chaining and optional field setting.
     * </p>
     */
    public static class AuthRequestDTOBuilder {
        private String email;
        private String password;

        /**
         * Sets the email for the builder.
         *
         * @param email The email address
         * @return This builder instance for method chaining
         */
        public AuthRequestDTOBuilder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the password for the builder.
         *
         * @param password The password
         * @return This builder instance for method chaining
         */
        public AuthRequestDTOBuilder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Builds the AuthRequestDTO instance.
         *
         * @return A new AuthRequestDTO with the configured values
         */
        public AuthRequestDTO build() {
            return new AuthRequestDTO(email, password);
        }
    }
}
