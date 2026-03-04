package com.pixelwallet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wallet represents a user's account balance and transaction history.
 * <p>
 * <strong>Entity Characteristics:</strong>
 * <ul>
 *   <li>OneToOne relationship with User: Each user has exactly one wallet</li>
 *   <li>Financial precision: Balance stored as BigDecimal(19,2) for exact currency calculations</li>
 *   <li>Concurrency control: @Version field implements optimistic locking</li>
 *   <li>Audit trail: createdAt and updatedAt timestamps for compliance</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Concurrency Control - Optimistic Locking:</strong>
 * <p>
 * Uses JPA @Version annotation for non-blocking concurrent access:
 * </p>
 * <ul>
 *   <li>Version field auto-incremented on each update</li>
 *   <li>When two threads update same wallet, second update fails with OptimisticLockException</li>
 *   <li>Caller must retry transaction (exponential backoff recommended)</li>
 *   <li>Prevents lost updates and ensures data consistency without table locks</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Balance Modifications:</strong> Always done within @Transactional service methods
 * with SERIALIZABLE isolation level to prevent race conditions.
 * </p>
 * <p>
 * <strong>Transaction History:</strong> sentTransactions and receivedTransactions
 * lists store the complete transaction audit trail for both parties.
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class Wallet {

    /**
     * Unique identifier for this wallet.
     * <p>
     * Auto-generated UUID provides global uniqueness.
     * Primary key for wallet lookup and relationship management.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The user who owns this wallet.
     * <p>
     * OneToOne relationship: Each user has exactly one wallet.
     * UNIQUE constraint ensures user_id appears only once across all wallets.
     * Non-lazy to simplify user profile queries.
     * </p>
     */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Current available balance in the wallet.
     * <p>
     * Stored as BigDecimal(19,2) for financial precision:
     * <ul>
     *   <li>Precision: 19 digits total (up to 17 before decimal)</li>
     *   <li>Scale: 2 decimal places (cents for USD)</li>
     *   <li>Max value: 99,999,999,999,999,999.99</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Default:</strong> 0.00 USD (set in @PrePersist if null)
     * </p>
     * <p>
     * <strong>Balance Rules:</strong>
     * <ul>
     *   <li>Never negative (enforced by service layer validation)</li>
     *   <li>Updated atomically with transaction status</li>
     *   <li>Read within SERIALIZABLE transaction for accurate balance check</li>
     * </ul>
     * </p>
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /**
     * Currency code for this wallet (e.g., "USD", "EUR", "GBP").
     * <p>
     * ISO 4217 currency code. Default: "USD" (set in @PrePersist if null).
     * Currently, all operations assume single currency per wallet.
     * </p>
     */
    @Column(nullable = false)
    private String currency;

    /**
     * List of transactions sent by this wallet.
     * <p>
     * OneToMany relationship: Wallet can send multiple transactions.
     * Lazy-loaded by default (only loaded when accessed by @OneToMany).
     * Mapped from Transaction.senderWallet.
     * </p>
     * <p>
     * <strong>Cascade Behavior:</strong> CascadeType.ALL means transactions
     * Operations are cascaded, but orphanRemoval=false allows transactions
     * to remain when wallet is deleted (audit trail preservation).
     * </p>
     */
    @OneToMany(mappedBy = "senderWallet", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Transaction> sentTransactions = new ArrayList<>();

    /**
     * List of transactions received by this wallet.
     * <p>
     * OneToMany relationship: Wallet can receive multiple transactions.
     * Lazy-loaded by default (only loaded when accessed by @OneToMany).
     * Mapped from Transaction.receiverWallet.
     * </p>
     * <p>
     * <strong>Cascade Behavior:</strong> CascadeType.ALL means transactions
     * Operations are cascaded, but orphanRemoval=false allows transactions
     * to remain when wallet is deleted (audit trail preservation).
     * </p>
     */
    @OneToMany(mappedBy = "receiverWallet", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Transaction> receivedTransactions = new ArrayList<>();

    /**
     * Server timestamp when this wallet was created.
     * <p>
     * Immutable: Set automatically in @PrePersist, never updated.
     * Typically when the user registered for the first time.
     * </p>
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Server timestamp of last update to wallet balance or metadata.
     * <p>
     * Updated automatically in @PreUpdate each time wallet is modified.
     * Useful for tracking wallet activity and activity reconciliation.
     * </p>
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Version number for optimistic locking (concurrency control).
     * <p>
     * <strong>Purpose:</strong> Prevent lost updates when multiple threads
     * concurrently modify the same wallet.
     * </p>
     * <p>
     * <strong>How It Works:</strong>
     * <ul>
     *   <li>JPA auto-increments version on each UPDATE</li>
     *   <li>SET clause includes: WHERE version = oldVersion</li>
     *   <li>If concurrent update occurred, WHERE matches 0 rows</li>
     *   <li>Hibernate throws OptimisticLockException</li>
     *   <li>Caller retries transaction (business logic layer handles retry)</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Example Scenario:</strong>
     * <ol>
     *   <li>Thread A reads wallet (balance=100, version=1)</li>
     *   <li>Thread B reads wallet (balance=100, version=1)</li>
     *   <li>Thread A updates wallet: UPDATE SET balance=80 WHERE version=1 ✓ (version→2)</li>
     *   <li>Thread B updates wallet: UPDATE SET balance=75 WHERE version=1 ✗ (0 rows affected)</li>
     *   <li>Thread B throws OptimisticLockException, retries with fresh data</li>
     * </ol>
     * </p>
     */
    @Version
    private Long version;

    /**
     * Default constructor for JPA.
     */
    public Wallet() {}

    /**
     * Constructor with all fields.
     *
     * @param id Wallet unique identifier
     * @param user Associated user
     * @param balance Current balance
     * @param currency Currency code
     * @param sentTransactions Outgoing transactions
     * @param receivedTransactions Incoming transactions
     * @param createdAt Creation timestamp
     * @param updatedAt Last update timestamp
     * @param version Optimistic lock version
     */
    public Wallet(UUID id, User user, BigDecimal balance, String currency,
                  List<Transaction> sentTransactions, List<Transaction> receivedTransactions,
                  LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        this.id = id;
        this.user = user;
        this.balance = balance;
        this.currency = currency;
        this.sentTransactions = sentTransactions;
        this.receivedTransactions = receivedTransactions;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /**
     * Automatically called by JPA before first insert.
     * <p>
     * Sets default values if not explicitly provided:
     * <ul>
     *   <li>createdAt = current server time</li>
     *   <li>updatedAt = current server time</li>
     *   <li>balance = 0.00 (if null)</li>
     *   <li>currency = "USD" (if null)</li>
     * </ul>
     * </p>
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
        if (this.currency == null) {
            this.currency = "USD";
        }
    }

    /**
     * Automatically called by JPA before update.
     * <p>
     * Updates the updatedAt timestamp to track last modification time.
     * </p>
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    /**
     * Gets the wallet's unique identifier.
     * @return UUID id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the wallet's unique identifier.
     * @param id UUID id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the user who owns this wallet.
     * @return Associated User object
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user who owns this wallet.
     * @param user User object to associate
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the current balance of this wallet.
     * @return Balance as BigDecimal
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Sets the balance of this wallet.
     * <p>
     * Note: In production, updates should go through service layer
     * which enforces SERIALIZABLE isolation and validates business rules.
     * </p>
     * @param balance BigDecimal balance to set
     */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    /**
     * Gets the currency code of this wallet.
     * @return Currency code (e.g., "USD")
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the currency code of this wallet.
     * @param currency Currency code to set
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Gets the list of transactions sent from this wallet.
     * @return List of Transaction objects sent
     */
    public List<Transaction> getSentTransactions() {
        return sentTransactions;
    }

    /**
     * Sets the list of sent transactions.
     * @param sentTransactions List of Transaction objects
     */
    public void setSentTransactions(List<Transaction> sentTransactions) {
        this.sentTransactions = sentTransactions;
    }

    /**
     * Gets the list of transactions received by this wallet.
     * @return List of Transaction objects received
     */
    public List<Transaction> getReceivedTransactions() {
        return receivedTransactions;
    }

    /**
     * Sets the list of received transactions.
     * @param receivedTransactions List of Transaction objects
     */
    public void setReceivedTransactions(List<Transaction> receivedTransactions) {
        this.receivedTransactions = receivedTransactions;
    }

    /**
     * Gets the creation timestamp of this wallet.
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
     * Gets the last update timestamp of this wallet.
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
     * Gets the optimistic lock version number.
     * @return Current version for concurrency control
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the optimistic lock version number.
     * <p>
     * Note: Version is normally managed automatically by Hibernate.
     * Do not set manually in application code.
     * </p>
     * @param version Version number
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Creates a WalletBuilder for fluent construction of Wallet objects.
     * <p>
     * Example:
     * <pre>
     * Wallet wallet = Wallet.builder()
     *     .user(user)
     *     .balance(new BigDecimal("1000.00"))
     *     .currency("USD")
     *     .build();
     * </pre>
     * </p>
     * @return WalletBuilder instance
     */
    public static WalletBuilder builder() {
        return new WalletBuilder();
    }

    /**
     * Builder class for constructing Wallet objects fluently.
     * <p>
     * Simplifies creation of Wallet entities with optional field setting.
     * </p>
     */
    public static class WalletBuilder {
        private UUID id;
        private User user;
        private BigDecimal balance;
        private String currency;
        private List<Transaction> sentTransactions = new ArrayList<>();
        private List<Transaction> receivedTransactions = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long version;

        /**
         * Sets the wallet ID.
         * @param id UUID to set
         * @return This builder for chaining
         */
        public WalletBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the associated user.
         * @param user User to associate
         * @return This builder for chaining
         */
        public WalletBuilder user(User user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the balance.
         * @param balance Balance amount to set
         * @return This builder for chaining
         */
        public WalletBuilder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        /**
         * Sets the currency code.
         * @param currency Currency code to set
         * @return This builder for chaining
         */
        public WalletBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        /**
         * Sets the sent transactions list.
         * @param sentTransactions List of outgoing transactions
         * @return This builder for chaining
         */
        public WalletBuilder sentTransactions(List<Transaction> sentTransactions) {
            this.sentTransactions = sentTransactions;
            return this;
        }

        /**
         * Sets the received transactions list.
         * @param receivedTransactions List of incoming transactions
         * @return This builder for chaining
         */
        public WalletBuilder receivedTransactions(List<Transaction> receivedTransactions) {
            this.receivedTransactions = receivedTransactions;
            return this;
        }

        /**
         * Sets the creation timestamp.
         * @param createdAt Creation timestamp
         * @return This builder for chaining
         */
        public WalletBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last update timestamp.
         * @param updatedAt Last update timestamp
         * @return This builder for chaining
         */
        public WalletBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Sets the optimistic lock version.
         * @param version Version number
         * @return This builder for chaining
         */
        public WalletBuilder version(Long version) {
            this.version = version;
            return this;
        }

        /**
         * Constructs a Wallet instance from builder state.
         * @return Built Wallet object
         */
        public Wallet build() {
            Wallet wallet = new Wallet();
            wallet.id = this.id;
            wallet.user = this.user;
            wallet.balance = this.balance;
            wallet.currency = this.currency;
            wallet.sentTransactions = this.sentTransactions;
            wallet.receivedTransactions = this.receivedTransactions;
            wallet.createdAt = this.createdAt;
            wallet.updatedAt = this.updatedAt;
            wallet.version = this.version;
            return wallet;
        }
    }
}
