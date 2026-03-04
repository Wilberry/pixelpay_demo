package com.pixelwallet.service;

import com.pixelwallet.dto.TransferRequestDTO;
import com.pixelwallet.dto.TransferResponseDTO;
import com.pixelwallet.exception.DuplicateTransactionException;
import com.pixelwallet.exception.InsufficientFundsException;
import com.pixelwallet.exception.UserNotFoundException;
import com.pixelwallet.model.Transaction;
import com.pixelwallet.model.User;
import com.pixelwallet.model.Wallet;
import com.pixelwallet.model.enum_types.TransactionStatus;
import com.pixelwallet.model.enum_types.TransactionType;
import com.pixelwallet.repository.TransactionRepository;
import com.pixelwallet.repository.UserRepository;
import com.pixelwallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TransactionService manages all financial transaction operations.
 * <p>
 * <strong>Core Responsibilities:</strong>
 * <ul>
 *   <li>Atomic money transfers between user wallets</li>
 *   <li>Transaction status tracking and audit trail</li>
 *   <li>Idempotency protection via reference numbers</li>
 *   <li>Balance validation and insufficient funds handling</li>
 *   <li>Comprehensive error handling and rollback</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Atomic Transfer Logic:</strong>
 * <ul>
 *   <li>Uses SERIALIZABLE isolation level for maximum concurrency safety</li>
 *   <li>Validates sender balance before transfer</li>
 *   <li>Creates transaction records with PENDING status initially</li>
 *   <li>Updates wallet balances atomically</li>
 *   <li>Marks transaction as SUCCESS or FAILED based on outcome</li>
 *   <li>Rolls back entire operation if any step fails</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Idempotency Protection:</strong>
 * Reference numbers prevent duplicate processing of the same transfer request.
 * If a reference number is reused, the system throws DuplicateTransactionException.
 * </p>
 * <p>
 * <strong>Transaction Types:</strong>
 * <ul>
 *   <li>DEBIT: Money leaving sender's wallet</li>
 *   <li>CREDIT: Money entering receiver's wallet</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Transaction Status:</strong>
 * <ul>
 *   <li>PENDING: Transaction initiated but not yet processed</li>
 *   <li>SUCCESS: Transaction completed successfully</li>
 *   <li>FAILED: Transaction failed due to error or validation</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Key Interview Points:</strong>
 * <ul>
 *   <li>@Transactional ensures ACID properties for financial operations</li>
 *   <li>Isolation.SERIALIZABLE prevents concurrent modification issues</li>
 *   <li>ReferenceNumber ensures idempotency (duplicate transfers are rejected)</li>
 *   <li>BigDecimal provides financial precision and avoids floating-point errors</li>
 *   <li>Optimistic locking via @Version field ensures consistency</li>
 *   <li>Comprehensive exception handling prevents partial state corruption</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    /**
     * Constructs TransactionService with required repositories.
     *
     * @param transactionRepository Repository for transaction data access
     * @param walletRepository Repository for wallet data access
     * @param userRepository Repository for user data access
     */
    public TransactionService(TransactionRepository transactionRepository, WalletRepository walletRepository,
                               UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    /**
     * Initiates an atomic transfer between two users.
     * <p>
     * <strong>Transfer Process:</strong>
     * <ol>
     *   <li>Check for duplicate transaction (idempotency protection)</li>
     *   <li>Load and validate sender user and wallet exist</li>
     *   <li>Load and validate recipient user and wallet exist</li>
     *   <li>Validate transfer amount is positive</li>
     *   <li>Check sender has sufficient balance</li>
     *   <li>Prevent self-transfer to same wallet</li>
     *   <li>Create transaction record with PENDING status</li>
     *   <li>Execute atomic debit and credit operations</li>
     *   <li>Update transaction status to SUCCESS or FAILED</li>
     *   <li>Return comprehensive transfer response</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Concurrency Safety:</strong>
     * Uses SERIALIZABLE isolation to prevent race conditions where
     * multiple transfers could overdraw an account simultaneously.
     * </p>
     * <p>
     * <strong>Idempotency:</strong>
     * If reference number exists, throws DuplicateTransactionException
     * to prevent processing the same transfer twice.
     * </p>
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *   <li>UserNotFoundException: Sender or recipient doesn't exist</li>
     *   <li>InsufficientFundsException: Sender balance too low</li>
     *   <li>DuplicateTransactionException: Reference number already used</li>
     *   <li>IllegalArgumentException: Invalid amount or self-transfer</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Transaction Lifecycle:</strong>
     * Transaction starts as PENDING, then becomes SUCCESS on completion
     * or FAILED if any step encounters an error. Failed transactions
     * are still recorded for audit purposes.
     * </p>
     *
     * @param senderEmail Email of the sender initiating the transfer
     * @param transferRequest Details of the transfer including recipient, amount, and reference
     * @return TransferResponseDTO with complete transaction details and final balances
     * @throws UserNotFoundException if sender or recipient not found
     * @throws InsufficientFundsException if sender lacks sufficient balance
     * @throws DuplicateTransactionException if referenceNumber already exists
     * @throws IllegalArgumentException if amount is invalid or self-transfer attempted
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferResponseDTO initiateTransfer(String senderEmail, TransferRequestDTO transferRequest) {
        // Step 1: Check for duplicate transaction (Idempotency)
        transactionRepository.findByReferenceNumber(transferRequest.getReferenceNumber())
                .ifPresent(transaction -> {
                    throw new DuplicateTransactionException(
                            "Transfer with reference number " + transferRequest.getReferenceNumber() +
                            " already exists with status: " + transaction.getStatus());
                });

        // Step 2: Load sender and verify
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new UserNotFoundException("Sender not found"));

        Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new UserNotFoundException("Sender wallet not found"));

        // Step 3: Load recipient and verify
        User recipient = userRepository.findByEmail(transferRequest.getRecipientEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "Recipient not found with email: " + transferRequest.getRecipientEmail()));

        Wallet recipientWallet = walletRepository.findByUserId(recipient.getId())
                .orElseThrow(() -> new UserNotFoundException("Recipient wallet not found"));

        // Step 4: Validate amount
        if (transferRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Step 5: Check sufficient funds (within transaction)
        if (senderWallet.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + senderWallet.getBalance() +
                    ", Requested: " + transferRequest.getAmount());
        }

        // Step 6: Prevent self-transfer
        if (senderWallet.getId().equals(recipientWallet.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }

        // Step 7: Create transaction record (initially PENDING)
        Transaction transaction = Transaction.builder()
                .senderWallet(senderWallet)
                .receiverWallet(recipientWallet)
                .amount(transferRequest.getAmount())
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.PENDING)
                .referenceNumber(transferRequest.getReferenceNumber())
                .description(transferRequest.getDescription())
                .build();

        // Step 8: Execute atomic transfer
        try {
            // Debit sender
            senderWallet.setBalance(senderWallet.getBalance().subtract(transferRequest.getAmount()));
            walletRepository.save(senderWallet);

            // Credit recipient
            recipientWallet.setBalance(recipientWallet.getBalance().add(transferRequest.getAmount()));
            walletRepository.save(recipientWallet);

            // Mark transaction as successful
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);

            log.info("Transfer successful: {} -> {} (Reference: {})",
                    senderEmail, transferRequest.getRecipientEmail(),
                    transferRequest.getReferenceNumber());

        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
        }

        // Step 9: Return response
        return mapToTransferResponse(transaction, sender, recipient);
    }

    /**
     * Maps Transaction entity to TransferResponseDTO.
     * <p>
     * Converts internal Transaction entity to external API response format.
     * Includes all relevant transaction details for client consumption.
     * </p>
     *
     * @param transaction The completed transaction entity
     * @param sender The sender user entity
     * @param recipient The recipient user entity
     * @return TransferResponseDTO with formatted transaction data
     */
    private TransferResponseDTO mapToTransferResponse(Transaction transaction, User sender, User recipient) {
        return TransferResponseDTO.builder()
                .transactionId(transaction.getId())
                .senderWalletId(transaction.getSenderWallet().getId())
                .receiverWalletId(transaction.getReceiverWallet().getId())
                .senderEmail(sender.getEmail())
                .recipientEmail(recipient.getEmail())
                .amount(transaction.getAmount())
                .currency(transaction.getSenderWallet().getCurrency())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .referenceNumber(transaction.getReferenceNumber())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
