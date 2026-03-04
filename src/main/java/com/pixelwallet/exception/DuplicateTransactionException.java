package com.pixelwallet.exception;

/**
 * DuplicateTransactionException is thrown when attempting to create a transfer
 * with a reference number that has already been used.
 * <p>
 * <strong>Purpose - Idempotency Protection:</strong>
 * <ul>
 *   <li>Prevents duplicate transfers due to network retries or client errors</li>
 *   <li>Ensures exactly-once semantics for transfer operations</li>
 *   <li>Uses unique reference number as idempotency key</li>
 * </ul>
 * </p>
 * <p>
 * <strong>When It Occurs:</strong>
 * <ul>
 *   <li>Client submits transfer with reference number already present in database</li>
 *   <li>Indicates potential retry or duplicate submission</li>
 * </ul>
 * </p>
 * <p>
 * The application handles this exception and returns HTTP 409 Conflict,
 * allowing clients to safely retry with different reference numbers.
 * </p>
 * <p>
 * This is an unchecked exception (extends RuntimeException) as it represents
 * a recoverable business logic error.
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
public class DuplicateTransactionException extends RuntimeException {
    /**
     * Constructs exception with error message describing the duplicate reference.
     *
     * @param message Human-readable message (e.g., "Transaction with reference REF-12345 already exists")
     */
    public DuplicateTransactionException(String message) {
        super(message);
    }
}
