package com.pixelwallet.model;

import com.pixelwallet.model.enum_types.TransactionStatus;
import com.pixelwallet.model.enum_types.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction represents a monetary transfer between two wallets.
 * <p>
 * <strong>Entity Characteristics:</strong>
 * <ul>
 *   <li>Immutable: Once created, core fields (sender, receiver, amount) cannot be changed</li>
 *   <li>Auditable: Has createdAt, updatedAt timestamps for compliance</li>
 *   <li>Idempotent: referenceNumber (unique) enables detection/prevention of duplicate transfers</li>
 *   <li>State-tracked: Status field (PENDING, SUCCESS, FAILED) provides visibility into transfer lifecycle</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Database Design:</strong>
 * <ul>
 *   <li>Primary Key: UUID id (auto-generated)</li>
 *   <li>Constraints: referenceNumber is UNIQUE (prevents duplicate transfers)</li>
 *   <li>Indexes: On sender_wallet_id, receiver_wallet_id, created_at for query performance</li>
 *   <li>Amounts: BigDecimal(19,2) for financial precision (up to 17 digits + 2 decimal places)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Lifecycle Lifecycle:</strong>
 * <ol>
 *   <li>Created with PENDING status</li>
 *   <li>Reference number auto-generated if not provided (for idempotency)</li>
 *   <li>On completion, status → SUCCESS or FAILED, updatedAt timestamp set</li>
 * </ol>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_sender_wallet", columnList = "sender_wallet_id"),
    @Index(name = "idx_receiver_wallet", columnList = "receiver_wallet_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    /**
     * Unique identifier for this transaction.
     * <p>
     * Auto-generated UUID ensures global uniqueness across all deployments.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Source wallet for this transfer.
     * <p>
     * Lazy-loaded to optimize query performance. The sending user's wallet
     * from which funds are debited.
     * </p>
     * <p>
     * Null indicates system-generated funds (e.g., interest, rewards).
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet_id")
    private Wallet senderWallet;

    /**
     * Destination wallet for this transfer.
     * <p>
     * Lazy-loaded to optimize query performance. The receiving user's wallet
     * to which funds are credited.
     * </p>
     * <p>
     * Null indicates wallet-to-system funds operation (e.g., withdrawal).
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet_id")
    private Wallet receiverWallet;

    /**
     * Amount being transferred.
     * <p>
     * Stored as BigDecimal(19,2) for financial precision:
     * <ul>
     *   <li>Precision: 19 digits total (up to 17 before decimal)</li>
     *   <li>Scale: 2 decimal places (cents)</li>
     *   <li>Max value: 99,999,999,999,999,999.99</li>
     * </ul>
     * </p>
     * <p>
     * Never null. Always validated to be positive (> 0) at application level.
     * </p>
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Type of transaction (TRANSFER, WITHDRAWAL, DEPOSIT, etc).
     * <p>
     * Enum value determines processing rules and audit trail categorization.
     * Immutable: set at creation and never changed.
     * </p>
     *
     * @see com.pixelwallet.model.enum_types.TransactionType
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /**
     * Current status of the transaction.
     * <p>
     * Possible states:
     * <ul>
     *   <li>PENDING: Awaiting processing (initial state)</li>
     *   <li>SUCCESS: Completed successfully, funds transferred</li>
     *   <li>FAILED: Processing failed, funds not transferred</li>
     * </ul>
     * </p>
     * <p>
     * Defaults to PENDING if not explicitly set during creation.
     * </p>
     *
     * @see com.pixelwallet.model.enum_types.TransactionStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /**
     * Optional description of the transaction purpose.
     * <p>
     * Max 500 characters. Can contain customer notes, payment memo, or reason.
     * Useful for users to understand why the transfer occurred.
     * </p>
     */
    @Column(length = 500)
    private String description;

    /**
     * Server timestamp when this transaction was created.
     * <p>
     * Immutable: Set automatically in @PrePersist, never updated.
     * Useful for audit trails and chronological ordering of transactions.
     * </p>
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Server timestamp of last update to this transaction.
     * <p>
     * Updated automatically in @PreUpdate. Changes when transaction status
     * is updated from PENDING to final state (SUCCESS or FAILED).
     * </p>
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Unique reference number for idempotency and deduplication.
     * <p>
     * <strong>Purpose:</strong> Prevents duplicate/redundant transfers when:
     * <ul>
     *   <li>Network request is retried by client</li>
     *   <li>Client connection drops and reconnects</li>
     *   <li>User accidentally submits form twice</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Uniqueness Guarantee:</strong> Database UNIQUE constraint ensures
     * only one transaction per reference number. Attempting to create a second
     * transfer with same reference throws DuplicateTransactionException.
     * </p>
     * <p>
     * <strong>Auto-generation:</strong> If not provided during creation,
     * a UUID is generated automatically in @PrePersist.
     * </p>
     * <p>
     * <strong>Client Use:</strong> Clients should generate unique reference
     * numbers (UUIDs, sequential IDs, timestamps) and send on each request
     * to enable exactly-once semantics.
     * </p>
     */
    @Column(unique = true, nullable = false)
    private String referenceNumber;

    /**
     * Automatically called by JPA before first insert.
     * <p>
     * Sets immutable fields:
     * <ul>
     *   <li>createdAt = current server time</li>
     *   <li>updatedAt = current server time</li>
     *   <li>status = PENDING (if not explicitly set)</li>
     *   <li>referenceNumber = UUID (if not explicitly set)</li>
     * </ul>
     * </p>
     * <p>
     * This ensures every transaction has a unique reference number and proper
     * timestamps for audit purposes.
     * </p>
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
        if (this.referenceNumber == null) {
            this.referenceNumber = UUID.randomUUID().toString();
        }
    }

    /**
     * Automatically called by JPA before update.
     * <p>
     * Updates only the updatedAt timestamp. Used to track when transaction
     * status changed from PENDING to SUCCESS/FAILED.
     * </p>
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    /**
     * Gets the transaction's unique identifier.
     * @return UUID id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the transaction's unique identifier.
     * @param id UUID id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the sender's wallet.
     * @return Sending wallet (null for system-generated transactions)
     */
    public Wallet getSenderWallet() {
        return senderWallet;
    }

    /**
     * Sets the sender's wallet.
     * @param senderWallet Wallet that sends funds
     */
    public void setSenderWallet(Wallet senderWallet) {
        this.senderWallet = senderWallet;
    }

    /**
     * Gets the receiver's wallet.
     * @return Receiving wallet (null for system withdrawal transactions)
     */
    public Wallet getReceiverWallet() {
        return receiverWallet;
    }

    /**
     * Sets the receiver's wallet.
     * @param receiverWallet Wallet that receives funds
     */
    public void setReceiverWallet(Wallet receiverWallet) {
        this.receiverWallet = receiverWallet;
    }

    /**
     * Gets the transfer amount.
     * @return Amount as BigDecimal
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transfer amount.
     * @param amount Amount to transfer
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Gets the transaction type.
     * @return TransactionType enum value
     */
    public TransactionType getType() {
        return type;
    }

    /**
     * Sets the transaction type.
     * @param type TransactionType to set
     */
    public void setType(TransactionType type) {
        this.type = type;
    }

    /**
     * Gets the transaction status.
     * @return TransactionStatus enum value
     */
    public TransactionStatus getStatus() {
        return status;
    }

    /**
     * Sets the transaction status.
     * @param status TransactionStatus to set
     */
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    /**
     * Gets the transaction description.
     * @return Description string (may be null)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * @param description Description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the creation timestamp.
     * @return LocalDateTime of creation
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
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
     * Gets the reference number for idempotency.
     * @return Reference number string
     */
    public String getReferenceNumber() {
        return referenceNumber;
    }

    /**
     * Sets the reference number for idempotency.
     * @param referenceNumber Reference number to set
     */
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    /**
     * Creates a TransactionBuilder for fluent construction of Transaction objects.
     * <p>
     * Example:
     * <pre>
     * Transaction transaction = Transaction.builder()
     *     .senderWallet(senderWallet)
     *     .receiverWallet(receiverWallet)
     *     .amount(new BigDecimal("100.00"))
     *     .type(TransactionType.TRANSFER)
     *     .referenceNumber("REF-001")
     *     .build();
     * </pre>
     * </p>
     * @return TransactionBuilder instance
     */
    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    /**
     * Builder class for constructing Transaction objects fluently.
     * <p>
     * Simplifies creation of Transaction entities with optional field setting.
     * </p>
     */
    public static class TransactionBuilder {
        private UUID id;
        private Wallet senderWallet;
        private Wallet receiverWallet;
        private BigDecimal amount;
        private TransactionType type;
        private TransactionStatus status;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String referenceNumber;

        /**
         * Sets the transaction ID.
         * @param id UUID to set
         * @return This builder for chaining
         */
        public TransactionBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the sender wallet.
         * @param senderWallet Wallet that sends funds
         * @return This builder for chaining
         */
        public TransactionBuilder senderWallet(Wallet senderWallet) {
            this.senderWallet = senderWallet;
            return this;
        }

        /**
         * Sets the receiver wallet.
         * @param receiverWallet Wallet that receives funds
         * @return This builder for chaining
         */
        public TransactionBuilder receiverWallet(Wallet receiverWallet) {
            this.receiverWallet = receiverWallet;
            return this;
        }

        /**
         * Sets the transfer amount.
         * @param amount Amount to transfer
         * @return This builder for chaining
         */
        public TransactionBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the transaction type.
         * @param type TransactionType to set
         * @return This builder for chaining
         */
        public TransactionBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the transaction status.
         * @param status TransactionStatus to set
         * @return This builder for chaining
         */
        public TransactionBuilder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the transaction description.
         * @param description Description to set
         * @return This builder for chaining
         */
        public TransactionBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the creation timestamp.
         * @param createdAt Creation timestamp
         * @return This builder for chaining
         */
        public TransactionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last update timestamp.
         * @param updatedAt Last update timestamp
         * @return This builder for chaining
         */
        public TransactionBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Sets the reference number for idempotency.
         * @param referenceNumber Reference number to set
         * @return This builder for chaining
         */
        public TransactionBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        /**
         * Constructs a Transaction instance from builder state.
         * @return Built Transaction object
         */
        public Transaction build() {
            Transaction transaction = new Transaction();
            transaction.id = this.id;
            transaction.senderWallet = this.senderWallet;
            transaction.receiverWallet = this.receiverWallet;
            transaction.amount = this.amount;
            transaction.type = this.type;
            transaction.status = this.status;
            transaction.description = this.description;
            transaction.createdAt = this.createdAt;
            transaction.updatedAt = this.updatedAt;
            transaction.referenceNumber = this.referenceNumber;
            return transaction;
        }
    }
}
