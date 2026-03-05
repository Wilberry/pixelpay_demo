package com.pixelwallet.controller;

import com.pixelwallet.dto.TransferResponseDTO;
import com.pixelwallet.model.Transaction;
import com.pixelwallet.model.Wallet;
import com.pixelwallet.repository.TransactionRepository;
import com.pixelwallet.repository.UserRepository;
import com.pixelwallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionController handles HTTP requests for querying transaction history.
 * <p>
 * <strong>Endpoints:</strong>
 * <ul>
 *   <li>GET /api/transactions/recent - Get recent transactions for authenticated user</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Security:</strong> All endpoints require valid JWT token.
 * Users can only view transactions for their own wallet.
 * </p>
 * <p>
 * <strong>Transaction History:</strong> Returns recently completed transactions with:
 * <ul>
 *   <li>Sender and recipient email addresses</li>
 *   <li>Transaction amount, currency, type, status</li>
 *   <li>Reference number, description, creation timestamp</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    /**
     * Constructs controller with required repository dependencies.
     *
     * @param transactionRepository TransactionRepository for database queries
     * @param walletRepository WalletRepository for wallet lookups
     * @param userRepository UserRepository (unused, kept for consistency)
     */
    public TransactionController(TransactionRepository transactionRepository,
                                 WalletRepository walletRepository,
                                 UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    /**
     * Gets the most recent transactions involving the authenticated user's wallet.
     * <p>
     * <strong>Endpoint:</strong> GET /api/transactions/recent
     * </p>
     * <p>
     * <strong>Authentication:</strong> Requires valid JWT Bearer token.
     * Returns transactions where user's wallet is either sender or receiver.
     * </p>
     * <p>
     * <strong>Pagination:</strong> Returns up to 10 most recent transactions
     * (ordered by creation time descending).
     * </p>
     * <p>
     * <strong>Transaction Details:</strong> Each transaction includes:
     * <ul>
     *   <li>transactionId: UUID for transaction identification</li>
     *   <li>senderEmail: Email of sender (null for system deposits)</li>
     *   <li>recipientEmail: Email of recipient (null for system withdrawals)</li>
     *   <li>amount: Transfer amount as BigDecimal</li>
     *   <li>currency: Currency code (e.g., "USD")</li>
     *   <li>status: Transaction status (SUCCESS, PENDING, FAILED)</li>
     *   <li>referenceNumber: Unique reference for idempotency</li>
     *   <li>description: Optional transaction description/memo</li>
     *   <li>createdAt: Timestamp when transaction was created</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Response:</strong> HTTP 200 OK with list of TransferResponseDTO JSON objects:
     * <pre>
     * [
     *   {
     *     "transactionId": "550e8400-e29b-41d4-a716-446655440000",
     *     "senderEmail": "alice@example.com",
     *     "recipientEmail": "bob@example.com",
     *     "amount": "100.50",
     *     "currency": "USD",
     *     "status": "SUCCESS",
     *     "referenceNumber": "REF-001",
     *     "description": "Payment for services",
     *     "createdAt": "2025-01-15T10:30:45"
     *   }
     * ]
     * </pre>
     * </p>
     * <p>
     * <strong>Error Scenarios:</strong>
     * <ul>
     *   <li>401 Unauthorized: Missing or invalid JWT token</li>
     *   <li>200 OK with empty list: User has no transactions yet</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Database Transaction:</strong> Read-only transaction (readOnly=true)
     * to optimize query performance and prevent accidental modifications.
     * </p>
     *
     * @param userDetails Spring Security UserDetails of authenticated user
     * @return ResponseEntity with list of recent transactions
     */
    @GetMapping(value = "/recent", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<TransferResponseDTO>> recentTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElse(null);

        if (wallet == null) {
            return ResponseEntity.ok(List.of());
        }

        Pageable pageable = PageRequest.of(0, 10);
        List<Transaction> page = transactionRepository.findByWalletId(wallet.getId(), pageable).getContent();

        List<TransferResponseDTO> dto = page.stream().map(t -> {
            // Resolve sender and recipient emails from wallet→user relationships
            String senderEmail = t.getSenderWallet() != null && t.getSenderWallet().getUser() != null
                    ? t.getSenderWallet().getUser().getEmail() : null;
            String recipientEmail = t.getReceiverWallet() != null && t.getReceiverWallet().getUser() != null
                    ? t.getReceiverWallet().getUser().getEmail() : null;

            return TransferResponseDTO.builder()
                    .transactionId(t.getId())
                    .senderWalletId(t.getSenderWallet() != null ? t.getSenderWallet().getId() : null)
                    .receiverWalletId(t.getReceiverWallet() != null ? t.getReceiverWallet().getId() : null)
                    .senderEmail(senderEmail)
                    .recipientEmail(recipientEmail)
                    .amount(t.getAmount())
                    .currency(t.getSenderWallet() != null ? t.getSenderWallet().getCurrency() : "")
                    .type(t.getType())
                    .status(t.getStatus())
                    .referenceNumber(t.getReferenceNumber())
                    .description(t.getDescription())
                    .createdAt(t.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        // log each DTO at info level to help diagnose serialization issues
        dto.forEach(d -> log.info("Transaction DTO for user {}: {}", email, d));

        return ResponseEntity.ok(dto);
    }
}
