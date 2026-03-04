package com.pixelwallet.repository;

import com.pixelwallet.model.Transaction;
import com.pixelwallet.model.enum_types.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Transaction entity data access operations.
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Transaction CRUD operations and audit trail management</li>
 *   <li>Reference number-based idempotency queries</li>
 *   <li>Wallet-specific transaction history retrieval</li>
 *   <li>Paginated transaction queries for performance</li>
 *   <li>Transaction status analytics and reporting</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Key Query Methods:</strong>
 * <ul>
 *   <li>findByReferenceNumber(): Idempotency protection for duplicate transfers</li>
 *   <li>findByWalletId(): All transactions involving a wallet (sent/received)</li>
 *   <li>findSentTransactions(): Outgoing transactions from a wallet</li>
 *   <li>findReceivedTransactions(): Incoming transactions to a wallet</li>
 *   <li>countByStatus(): Transaction status statistics</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Database Design:</strong>
 * <ul>
 *   <li>Primary key: UUID for global uniqueness</li>
 *   <li>Foreign keys to sender and receiver wallets</li>
 *   <li>Unique constraint on reference_number for idempotency</li>
 *   <li>Amount stored as DECIMAL for financial precision</li>
 *   <li>Status enum for transaction lifecycle tracking</li>
 *   <li>Type enum for debit/credit classification</li>
 *   <li>Timestamps for audit trail and ordering</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Query Optimization:</strong>
 * <ul>
 *   <li>All queries use appropriate database indexes</li>
 *   <li>Pagination support for large transaction histories</li>
 *   <li>Ordered by created_at DESC for chronological display</li>
 *   <li>Efficient joins for wallet relationship queries</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Idempotency Support:</strong>
 * <ul>
 *   <li>Reference numbers prevent duplicate transaction processing</li>
 *   <li>Unique constraint ensures reference number uniqueness</li>
 *   <li>Fast lookup by reference number for duplicate detection</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Finds a transaction by its unique reference number.
     * <p>
     * Used for idempotency protection to detect and prevent duplicate transfers.
     * Reference numbers are unique across all transactions in the system.
     * </p>
     * <p>
     * <strong>Usage:</strong> Transfer validation, duplicate prevention, transaction lookup
     * </p>
     * <p>
     * <strong>Indexing:</strong> Uses unique index on reference_number column for fast lookups
     * </p>
     *
     * @param referenceNumber The unique reference number to search for
     * @return Optional containing the Transaction if found, empty otherwise
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /**
     * Retrieves all transactions involving a specific wallet (sent and received).
     * <p>
     * Returns paginated results of all transactions where the wallet is either
     * the sender or receiver. Ordered by creation time descending (newest first).
     * </p>
     * <p>
     * <strong>Usage:</strong> Complete transaction history for a user's wallet
     * </p>
     * <p>
     * <strong>Query Details:</strong>
     * <pre>
     * SELECT t FROM Transaction t WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId)
     * ORDER BY t.createdAt DESC
     * </pre>
     * </p>
     *
     * @param walletId The UUID of the wallet to find transactions for
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of transactions involving the specified wallet
     */
    @Query("SELECT t FROM Transaction t WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId) ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    /**
     * Retrieves transactions sent from a specific wallet (outgoing transfers).
     * <p>
     * Returns paginated results of all debit transactions where the wallet
     * is the sender. Ordered by creation time descending (newest first).
     * </p>
     * <p>
     * <strong>Usage:</strong> Outgoing transaction history, spending analysis
     * </p>
     * <p>
     * <strong>Query Details:</strong>
     * <pre>
     * SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId ORDER BY t.createdAt DESC
     * </pre>
     * </p>
     *
     * @param walletId The UUID of the wallet to find sent transactions for
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of transactions sent from the specified wallet
     */
    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findSentTransactions(@Param("walletId") UUID walletId, Pageable pageable);

    /**
     * Retrieves transactions received by a specific wallet (incoming transfers).
     * <p>
     * Returns paginated results of all credit transactions where the wallet
     * is the receiver. Ordered by creation time descending (newest first).
     * </p>
     * <p>
     * <strong>Usage:</strong> Incoming transaction history, income analysis
     * </p>
     * <p>
     * <strong>Query Details:</strong>
     * <pre>
     * SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId ORDER BY t.createdAt DESC
     * </pre>
     * </p>
     *
     * @param walletId The UUID of the wallet to find received transactions for
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of transactions received by the specified wallet
     */
    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findReceivedTransactions(@Param("walletId") UUID walletId, Pageable pageable);

    /**
     * Counts transactions by their current status.
     * <p>
     * Provides statistics on transaction status distribution.
     * Useful for monitoring system health and transaction success rates.
     * </p>
     * <p>
     * <strong>Usage:</strong> Analytics, monitoring, reporting dashboards
     * </p>
     * <p>
     * <strong>Query Details:</strong>
     * <pre>
     * SELECT COUNT(t) FROM Transaction t WHERE t.status = :status
     * </pre>
     * </p>
     *
     * @param status The transaction status to count (PENDING, SUCCESS, FAILED)
     * @return Number of transactions with the specified status
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);
}
