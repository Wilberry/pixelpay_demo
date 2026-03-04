package com.pixelwallet.controller;

import com.pixelwallet.dto.TransferRequestDTO;
import com.pixelwallet.dto.TransferResponseDTO;
import com.pixelwallet.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * TransferController handles wallet-to-wallet money transfer operations.
 * <p>
 * Manages atomic financial transactions between users with full audit trails,
 * idempotency guarantees, and comprehensive error handling. All endpoints require
 * JWT authentication.
 * </p>
 * <p>
 * <strong>Key Guarantees:</strong>
 * <ul>
 *   <li><strong>Atomicity:</strong> Transfer succeeds completely or fails entirely (SERIALIZABLE isolation)</li>
 *   <li><strong>Idempotency:</strong> Duplicate requests with same reference number are safely rejected</li>
 *   <li><strong>Consistency:</strong> Wallet balances always reconcile; sender debit equals receiver credit</li>
 *   <li><strong>Audit Trail:</strong> Every transaction recorded with timestamps, status, and reference number</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private static final Logger log = LoggerFactory.getLogger(TransferController.class);

    private final TransactionService transactionService;

    /**
     * Constructs TransferController with required TransactionService dependency.
     *
     * @param transactionService Service layer for transaction operations
     */
    public TransferController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Initiates an atomic money transfer between two wallets.
     * <p>
     * <strong>Request:</strong> POST /api/transfers<br>
     * <strong>Authentication:</strong> Bearer token (required)<br>
     * <strong>Response:</strong> 201 Created with transfer confirmation<br>
     * <strong>Errors:</strong> 400 validation error, 401 unauthorized, 409 duplicate reference
     * </p>
     * <p>
     * <strong>Business Rules:</strong>
     * <ul>
     *   <li>Sender must have sufficient funds</li>
     *   <li>Transfer amount must be positive</li>
     *   <li>Reference number must be unique (idempotency protection)</li>
     *   <li>Cannot transfer to self</li>
     *   <li>Recipient email must exist in system</li>
     * </ul>
     * </p>
     *
     * @param transferRequest DTO containing transfer details:
     *        - recipientEmail (String): Target user's email
     *        - amount (BigDecimal): Transfer amount (positive, up to 2 decimal places)
     *        - referenceNumber (String): Unique transaction identifier for idempotency
     *        - description (String): Optional transaction memo
     * @param authentication Spring Security authentication object containing authenticated user's email
     * @return ResponseEntity with {@link TransferResponseDTO} containing:
     *         - transactionId: UUID for tracking
     *         - status: SUCCESS, PENDING, or FAILED
     *         - both wallet IDs and user emails
     *         - exact amount transferred
     *         - creation timestamp
     * @throws com.pixelwallet.exception.InsufficientFundsException if sender balance insufficient
     * @throws com.pixelwallet.exception.UserNotFoundException if sender or recipient not found
     * @throws com.pixelwallet.exception.DuplicateTransactionException if reference number already used
     *
     * @see TransferRequestDTO
     * @see TransferResponseDTO
     */
    @PostMapping
    public ResponseEntity<TransferResponseDTO> initiateTransfer(
            @Valid @RequestBody TransferRequestDTO transferRequest,
            Authentication authentication) {

        String senderEmail = authentication.getName();
        log.info("Transfer request from {} to {} (Reference: {})",
                senderEmail, transferRequest.getRecipientEmail(), transferRequest.getReferenceNumber());

        TransferResponseDTO response = transactionService.initiateTransfer(senderEmail, transferRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Health check endpoint for monitoring service availability.
     * <p>
     * Returns 200 OK if service is operational. Can be called without authentication.
     * </p>
     *
     * @return Simple success message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transfer service is healthy");
    }
}
