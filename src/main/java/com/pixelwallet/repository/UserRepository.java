package com.pixelwallet.repository;

import com.pixelwallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity data access operations.
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>User CRUD operations (Create, Read, Update, Delete)</li>
 *   <li>Email-based user lookup and existence checks</li>
 *   <li>Primary key operations using UUID</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Key Query Methods:</strong>
 * <ul>
 *   <li>findByEmail(): Retrieves user by unique email address</li>
 *   <li>existsByEmail(): Checks if email is already registered</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Database Design:</strong>
 * <ul>
 *   <li>Primary key: UUID for global uniqueness</li>
 *   <li>Unique constraint on email column</li>
 *   <li>Indexed email field for fast lookups</li>
 *   <li>One-to-one relationship with Wallet entity</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Performance Characteristics:</strong>
 * <ul>
 *   <li>Email queries use database index for O(log n) performance</li>
 *   <li>Existence checks are optimized for registration validation</li>
 *   <li>Lazy loading for wallet relationship to avoid N+1 queries</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     * <p>
     * Performs a case-sensitive search for users with the specified email.
     * Returns Optional.empty() if no user is found with the given email.
     * </p>
     * <p>
     * <strong>Usage:</strong> Authentication, user lookup, transfer validation
     * </p>
     * <p>
     * <strong>Indexing:</strong> Uses database index on email column for fast retrieval
     * </p>
     *
     * @param email The email address to search for (case-sensitive)
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email address.
     * <p>
     * Optimized query that returns boolean without loading full user entity.
     * Used primarily during user registration to prevent duplicate emails.
     * </p>
     * <p>
     * <strong>Usage:</strong> Registration validation, duplicate prevention
     * </p>
     * <p>
     * <strong>Performance:</strong> More efficient than findByEmail() for existence checks
     * </p>
     *
     * @param email The email address to check for existence
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
