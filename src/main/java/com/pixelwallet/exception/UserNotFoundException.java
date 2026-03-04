package com.pixelwallet.exception;

/**
 * UserNotFoundException is thrown when a user cannot be found in the database.
 * <p>
 * This is a business logic exception, not a system error. It occurs when:
 * <ul>
 *   <li>User does not exist with given email</li>
 *   <li>User ID does not exist</li>
 *   <li>User was deleted or deactivated</li>
 * </ul>
 * </p>
 * <p>
 * The application handles this exception and returns HTTP 404 Not Found
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
public class UserNotFoundException extends RuntimeException {
    /**
     * Constructs exception with error message describing which user was not found.
     *
     * @param message Human-readable message (e.g., "User with email alice@example.com not found")
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
