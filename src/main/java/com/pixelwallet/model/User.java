package com.pixelwallet.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User represents a system account with authentication and authorization capabilities.
 * <p>
 * <strong>Entity Characteristics:</strong>
 * <ul>
 *   <li>Authentication: Email and password-based login with BCrypt password hashing</li>
 *   <li>Authorization: Role-based access control (USER or ADMIN)</li>
 *   <li>Financial: Associated with exactly one Wallet for balance and transaction management</li>
 *   <li>Auditable: Has createdAt, updatedAt timestamps for compliance and debugging</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Relationships:</strong>
 * <ul>
 *   <li>OneToOne with Wallet: Auto-created during user registration</li>
 *   <li>Cascade: Wallet deleted when user is deleted (orphanRemoval=true)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Email Uniqueness:</strong> Unique constraint on email ensures
 * each email address belongs to at most one user account.
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * Unique identifier for this user.
     * <p>
     * Auto-generated UUID provides global uniqueness across all deployments.
     * Primary key for user lookup and relationship management.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Email address used for login and identification.
     * <p>
     * UNIQUE constraint ensures no two users share the same email.
     * Format validation performed at application level (via Spring Validation).
     * </p>
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * BCrypt-hashed password for authentication.
     * <p>
     * Stored hashed, never in plain text. Hashing uses BCrypt algorithm
     * with configurable strength factor (default: 10 rounds).
     * </p>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *   <li>Never returned in API responses</li>
     *   <li>Never logged or displayed to users</li>
     *   <li>Hashing is one-way: original password cannot be recovered</li>
     *   <li>Salt is automatically generated per password</li>
     * </ul>
     * </p>
     */
    @Column(nullable = false)
    private String password;

    /**
     * User role for authorization (USER or ADMIN).
     * <p>
     * <strong>Roles:</strong>
     * <ul>
     *   <li>USER: Standard user with access to own wallet and transfers</li>
     *   <li>ADMIN: Administrative user with access to all user data (future enhancement)</li>
     * </ul>
     * </p>
     * <p>
     * Used by Spring Security for role-based access control (@PreAuthorize, hasRole, etc).
     * </p>
     */
    @Column(nullable = false)
    private String role; // USER or ADMIN

    /**
     * User's first name for identification and display purposes.
     * <p>
     * Optional at database level but required during registration.
     * Used in transaction history and user interface.
     * </p>
     */
    @Column(nullable = false)
    private String firstName;

    /**
     * User's last name for identification and display purposes.
     * <p>
     * Optional at database level but required during registration.
     * Used in transaction history and user interface.
     * </p>
     */
    @Column(nullable = false)
    private String lastName;

    /**
     * The wallet associated with this user.
     * <p>
     * OneToOne relationship: Each user has exactly one wallet created
     * automatically during registration. Wallet cannot exist without user.
     * </p>
     * <p>
     * <strong>Cascade Behavior:</strong>
     * <ul>
     *   <li>CascadeType.ALL: Wallet operations (save, update, delete) cascade</li>
     *   <li>orphanRemoval=true: Wallet is deleted when user is deleted</li>
     * </ul>
     * </p>
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Wallet wallet;

    /**
     * Server timestamp when this user account was created.
     * <p>
     * Immutable: Set automatically in @PrePersist, never updated.
     * Useful for audit trails and account age calculation.
     * </p>
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Server timestamp of last update to this user account.
     * <p>
     * Updated automatically in @PreUpdate when any field changes
     * (e.g., password update, profile updates).
     * </p>
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor for JPA.
     */
    public User() {}

    /**
     * Constructor with all fields.
     *
     * @param id User unique identifier
     * @param email Email address
     * @param password BCrypt-hashed password
     * @param role User role (USER or ADMIN)
     * @param firstName First name
     * @param lastName Last name
     * @param wallet Associated wallet
     * @param createdAt Creation timestamp
     * @param updatedAt Last update timestamp
     */
    public User(UUID id, String email, String password, String role, String firstName, 
                String lastName, Wallet wallet, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.wallet = wallet;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Automatically called by JPA before first insert.
     * <p>
     * Sets immutable timestamp fields:
     * <ul>
     *   <li>createdAt = current server time</li>
     *   <li>updatedAt = current server time</li>
     * </ul>
     * </p>
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Automatically called by JPA before update.
     * <p>
     * Updates the updatedAt timestamp to track when the user record was last modified.
     * </p>
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters with field documentation

    /**
     * Gets the user's unique identifier.
     * @return UUID id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the user's unique identifier.
     * @param id UUID id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the user's email address.
     * @return Email address string
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     * @param email Email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the BCrypt-hashed password.
     * <p>
     * Note: Returns hashed value, not plain text password.
     * </p>
     * @return Hashed password string
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user's password (expected to be BCrypt-hashed by caller).
     * @param password BCrypt-hashed password string
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the user's role (USER or ADMIN).
     * @return Role string
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the user's role.
     * @param role Role string (USER or ADMIN)
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the user's first name.
     * @return First name string
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name.
     * @param firstName First name string
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name.
     * @return Last name string
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name.
     * @param lastName Last name string
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the wallet associated with this user.
     * @return Associated Wallet object
     */
    public Wallet getWallet() {
        return wallet;
    }

    /**
     * Sets the wallet associated with this user.
     * @param wallet Wallet object to associate
     */
    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    /**
     * Gets the account creation timestamp.
     * @return LocalDateTime of account creation
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the account creation timestamp.
     * <p>
     * Note: Normally set automatically by @PrePersist, not called manually.
     * </p>
     * @param createdAt Creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last update timestamp.
     * @return LocalDateTime of last update
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     * <p>
     * Note: Normally updated automatically by @PreUpdate, not called manually.
     * </p>
     * @param updatedAt Last update timestamp
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a UserBuilder for fluent construction of User objects.
     * <p>
     * Example:
     * <pre>
     * User user = User.builder()
     *     .email("alice@example.com")
     *     .password(hashedPassword)
     *     .firstName("Alice")
     *     .lastName("Smith")
     *     .role("USER")
     *     .build();
     * </pre>
     * </p>
     * @return UserBuilder instance
     */
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    /**
     * Builder class for constructing User objects fluently.
     * <p>
     * Simplifies creation of User entities with optional field setting.
     * </p>
     */
    public static class UserBuilder {
        private UUID id;
        private String email;
        private String password;
        private String role;
        private String firstName;
        private String lastName;
        private Wallet wallet;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        /**
         * Sets the user ID.
         * @param id UUID to set
         * @return This builder for chaining
         */
        public UserBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the email address.
         * @param email Email to set
         * @return This builder for chaining
         */
        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the password (should be BCrypt-hashed).
         * @param password BCrypt-hashed password to set
         * @return This builder for chaining
         */
        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the user role.
         * @param role Role to set (USER or ADMIN)
         * @return This builder for chaining
         */
        public UserBuilder role(String role) {
            this.role = role;
            return this;
        }

        /**
         * Sets the first name.
         * @param firstName First name to set
         * @return This builder for chaining
         */
        public UserBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        /**
         * Sets the last name.
         * @param lastName Last name to set
         * @return This builder for chaining
         */
        public UserBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        /**
         * Sets the associated wallet.
         * @param wallet Wallet to associate
         * @return This builder for chaining
         */
        public UserBuilder wallet(Wallet wallet) {
            this.wallet = wallet;
            return this;
        }

        /**
         * Sets the creation timestamp.
         * @param createdAt Creation timestamp
         * @return This builder for chaining
         */
        public UserBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last update timestamp.
         * @param updatedAt Last update timestamp
         * @return This builder for chaining
         */
        public UserBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Constructs a User instance from builder state.
         * @return Built User object
         */
        public User build() {
            User user = new User();
            user.id = this.id;
            user.email = this.email;
            user.password = this.password;
            user.role = this.role;
            user.firstName = this.firstName;
            user.lastName = this.lastName;
            user.wallet = this.wallet;
            user.createdAt = this.createdAt;
            user.updatedAt = this.updatedAt;
            return user;
        }
    }
}
