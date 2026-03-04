package com.pixelwallet.exception;

/**
 * InsufficientFundsException is thrown when a wallet cannot perform a transfer
 * due to insufficient balance.
 * <p>
 * This is a business logic exception, not a system error. It occurs when:
 * <ul>
 *   <li>Transfer amount exceeds wallet balance</li>
 *   <li>Wallet does not exist</li>
 *   <li>Wallet is in an invalid state</li>
 * </ul>
 * </p>
 * <p>
 * The application handles this exception and returns HTTP 400 Bad Request
 * with an appropriate error message to the client.
 * </p>
 * <p>
 * This is an unchecked exception (extends RuntimeException) as it represents
 * a recoverable business logic error.
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
public class InsufficientFundsException extends RuntimeException {
    /**
     * Constructs exception with error message describing the insufficient funds situation.
     *
     * @param message Human-readable message (e.g., "Wallet balance 50.00 insufficient for transfer 100.00")
     */
    public InsufficientFundsException(String message) {
        super(message);
    }
}
