package com.pixelwallet.service;

import com.pixelwallet.exception.UserNotFoundException;
import com.pixelwallet.model.User;
import com.pixelwallet.model.Wallet;
import com.pixelwallet.repository.UserRepository;
import com.pixelwallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * WalletService provides wallet-related operations and balance management.
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Wallet balance retrieval by user email or ID</li>
 *   <li>Wallet entity lookup and validation</li>
 *   <li>Balance queries with different error handling strategies</li>
 *   <li>Read-only transaction management for data consistency</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Key Design Principles:</strong>
 * <ul>
 *   <li>All balance operations use @Transactional(readOnly = true) for performance</li>
 *   <li>BigDecimal used for precise financial calculations</li>
 *   <li>Comprehensive error handling with meaningful exception messages</li>
 *   <li>Separation of concerns between user validation and wallet operations</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Balance Query Strategies:</strong>
 * <ul>
 *   <li>getWalletBalance(): Throws exception if user/wallet not found (strict validation)</li>
 *   <li>getBalanceIfExists(): Returns zero balance if user/wallet not found (lenient)</li>
 *   <li>getWalletByUserId(): Returns complete wallet entity for advanced operations</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Performance Considerations:</strong>
 * <ul>
 *   <li>Read-only transactions optimize database performance</li>
 *   <li>Efficient queries using indexed user_id foreign key</li>
 *   <li>Minimal data transfer for balance-only operations</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    /**
     * Constructs WalletService with required repositories.
     *
     * @param walletRepository Repository for wallet data access
     * @param userRepository Repository for user data access
     */
    public WalletService(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the current wallet balance for a user by email.
     * <p>
     * <strong>Process:</strong>
     * <ol>
     *   <li>Find user by email address</li>
     *   <li>Locate associated wallet by user ID</li>
     *   <li>Return current balance as BigDecimal</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Error Handling:</strong>
     * Throws UserNotFoundException if either user or wallet cannot be found.
     * This ensures strict validation for financial operations.
     * </p>
     * <p>
     * <strong>Performance:</strong>
     * Uses read-only transaction for optimal database performance
     * and leverages indexed queries for fast lookups.
     * </p>
     *
     * @param email Email address of the user whose balance to retrieve
     * @return Current wallet balance as BigDecimal (never null)
     * @throws UserNotFoundException if user or wallet doesn't exist
     */
    @Transactional(readOnly = true)
    public BigDecimal getWalletBalance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + email));

        return wallet.getBalance();
    }

    /**
     * Retrieves complete wallet details for a user by user ID.
     * <p>
     * Returns the full Wallet entity including balance, currency,
     * version (for optimistic locking), and associated user information.
     * </p>
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     *   <li>Transaction processing requiring full wallet context</li>
     *   <li>Administrative operations needing wallet metadata</li>
     *   <li>Balance verification with additional wallet properties</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Error Handling:</strong>
     * Throws UserNotFoundException if wallet cannot be found for the given user ID.
     * </p>
     *
     * @param userId Unique identifier of the user
     * @return Complete Wallet entity with all properties
     * @throws UserNotFoundException if wallet doesn't exist for the user
     */
    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user ID: " + userId));
    }

    /**
     * Retrieves wallet balance with lenient error handling.
     * <p>
     * Returns the current balance if user and wallet exist, otherwise returns zero.
     * This method never throws exceptions and is suitable for non-critical operations
     * where missing wallets should be treated as zero balance.
     * </p>
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     *   <li>UI display where missing wallets show as $0.00</li>
     *   <li>Reporting operations that shouldn't fail on missing data</li>
     *   <li>Graceful degradation in composite operations</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Performance:</strong>
     * Uses optimized query that joins user and wallet tables directly,
     * avoiding separate user lookup when wallet exists.
     * </p>
     *
     * @param email Email address of the user
     * @return Current balance if wallet exists, BigDecimal.ZERO otherwise
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalanceIfExists(String email) {
        return walletRepository.findByUserEmail(email)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Adds funds to a user's wallet.
     * <p>
     * Adds the specified amount to the user's wallet balance and saves the changes.
     * This operation requires write access and is wrapped in a transaction.
     * </p>
     *
     * @param email Email address of the user
     * @param amount Amount to add to the wallet
     * @return Updated wallet balance
     * @throws UserNotFoundException if user or wallet doesn't exist
     */
    @Transactional
    public BigDecimal fundWallet(String email, BigDecimal amount) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + email));

        // Add the amount to the wallet balance
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        
        // Save the updated wallet
        walletRepository.save(wallet);
        
        log.info("Wallet {} funded with amount {}, new balance: {}", email, amount, newBalance);
        
        return newBalance;
    }
}
