package com.pixelwallet.repository;

import com.pixelwallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Wallet entity data access operations.
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Wallet CRUD operations (Create, Read, Update, Delete)</li>
 *   <li>User-wallet relationship queries</li>
 *   <li>Balance retrieval and wallet lookups</li>
 *   <li>Optimistic locking support via version field</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Key Query Methods:</strong>
 * <ul>
 *   <li>findByUserId(): Direct lookup by user foreign key</li>
 *   <li>findByUserEmail(): Join query through user relationship</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Database Design:</strong>
 * <ul>
 *   <li>Primary key: UUID for global uniqueness</li>
 *   <li>Foreign key to User table (one-to-one relationship)</li>
 *   <li>Version column for optimistic locking</li>
 *   <li>Balance stored as DECIMAL for financial precision</li>
 *   <li>Currency field for multi-currency support</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Query Optimization:</strong>
 * <ul>
 *   <li>findByUserId(): Uses indexed foreign key for fast direct lookup</li>
 *   <li>findByUserEmail(): JPQL join query for email-based wallet access</li>
 *   <li>Both methods support lazy loading to prevent N+1 query issues</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Concurrency Control:</strong>
 * <ul>
 *   <li>Optimistic locking via @Version field prevents concurrent modification</li>
 *   <li>Balance updates use atomic operations in service layer</li>
 *   <li>Serializable isolation used for transfer operations</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Finds a wallet by the associated user ID.
     * <p>
     * Direct lookup using the foreign key relationship between Wallet and User.
     * This is the most efficient way to find a user's wallet when the user ID is known.
     * </p>
     * <p>
     * <strong>Usage:</strong> Transaction processing, balance queries, wallet operations
     * </p>
     * <p>
     * <strong>Performance:</strong> Uses indexed user_id foreign key for optimal performance
     * </p>
     *
     * @param userId The UUID of the user whose wallet to find
     * @return Optional containing the Wallet if found, empty otherwise
     */
    Optional<Wallet> findByUserId(UUID userId);

    /**
     * Finds a wallet by the associated user's email address.
     * <p>
     * Performs a join query through the user relationship to find wallet by email.
     * Useful when only the email address is available (e.g., API requests).
     * </p>
     * <p>
     * <strong>Usage:</strong> API balance queries, authentication workflows, UI operations
     * </p>
     * <p>
     * <strong>Query Details:</strong>
     * <pre>
     * SELECT w FROM Wallet w WHERE w.user.email = :email
     * </pre>
     * </p>
     * <p>
     * <strong>Performance:</strong> Requires join operation but still efficient with proper indexing
     * </p>
     *
     * @param email The email address of the user whose wallet to find
     * @return Optional containing the Wallet if found, empty otherwise
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.email = :email")
    Optional<Wallet> findByUserEmail(@Param("email") String email);
}
