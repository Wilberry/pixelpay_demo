package com.pixelwallet.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler provides centralized exception handling for the entire application.
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Intercepts exceptions thrown by controllers and services</li>
 *   <li>Translates exceptions to appropriate HTTP status codes</li>
 *   <li>Returns consistent JSON error response format</li>
 *   <li>Logs errors for monitoring and debugging</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Response Format:</strong> All errors return JSON ErrorResponse with:
 * <pre>
 * {
 *   "status": 400,
 *   "message": "Error description",
 *   "timestamp": "2025-01-15T10:30:45.123",
 *   "path": "/api/transfers",
 *   "validationErrors": { "field": "error message" }
 * }
 * </pre>
 * </p>
 * <p>
 * <strong>Handled Exceptions:</strong>
 * <ul>
 *   <li>UserNotFoundException → 404 NOT_FOUND</li>
 *   <li>InsufficientFundsException → 400 BAD_REQUEST</li>
 *   <li>DuplicateTransactionException → 409 CONFLICT</li>
 *   <li>MethodArgumentNotValidException → 400 BAD_REQUEST (with field errors)</li>
 *   <li>Exception (catch-all) → 500 INTERNAL_SERVER_ERROR</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles UserNotFoundException when a user is not found in the database.
     * <p>
     * Returns HTTP 404 Not Found. This occurs when attempting to fetch
     * a user that does not exist in the system.
     * </p>
     *
     * @param ex UserNotFoundException containing user not found message
     * @param request WebRequest for extracting request path and context
     * @return ResponseEntity with ErrorResponse containing 404 status and error details
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles InsufficientFundsException when wallet balance is not sufficient.
     * <p>
     * Returns HTTP 400 Bad Request. This is a business logic error that occurs
     * when a transfer amount exceeds the payer's available wallet balance.
     * This is an expected error condition, not a system error.
     * </p>
     *
     * @param ex InsufficientFundsException with details about insufficient funds
     * @param request WebRequest for extracting request path and context
     * @return ResponseEntity with ErrorResponse containing 400 status and error details
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles DuplicateTransactionException when duplicate reference number is used.
     * <p>
     * Returns HTTP 409 Conflict. This occurs when attempting to create a transfer
     * with a reference number that has already been used (idempotency protection).
     * The unique reference number is used to detect and prevent duplicate transfers
     * in case of network retries or client errors.
     * </p>
     *
     * @param ex DuplicateTransactionException with duplicate reference number details
     * @param request WebRequest for extracting request path and context
     * @return ResponseEntity with ErrorResponse containing 409 status and error details
     */
    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTransaction(DuplicateTransactionException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles MethodArgumentNotValidException for request validation failures.
     * <p>
     * Returns HTTP 400 Bad Request with field-level validation error details.
     * Triggered when @Valid validation on request DTOs fails (e.g., missing required fields,
     * invalid email format, amount <= 0, etc.).
     * </p>
     * <p>
     * Response includes a validationErrors map with field names as keys and
     * validation error messages as values.
     * </p>
     *
     * @param ex MethodArgumentNotValidException containing field validation error details
     * @param request WebRequest for extracting request path and context
     * @return ResponseEntity with ErrorResponse containing 400 status and field-level errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .validationErrors(errors)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles all other unexpected exceptions not explicitly handled.
     * <p>
     * Returns HTTP 500 Internal Server Error. This is a catch-all handler for
     * any unhandled exceptions. The full stack trace is logged for debugging.
     * A generic error message is returned to the client to avoid leaking
     * sensitive implementation details.
     * </p>
     *
     * @param ex Exception that was not explicitly handled by other handlers
     * @param request WebRequest for extracting request path and context
     * @return ResponseEntity with ErrorResponse containing 500 status and generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unhandled exception occurred", ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * ErrorResponse is the standardized error response JSON structure.
     * <p>
     * Returned by GlobalExceptionHandler for all error conditions.
     * Contains HTTP status code, error message, timestamp, request path,
     * and optional field-level validation errors.
     * </p>
     * <p>
     * JSON Example:
     * <pre>
     * {
     *   "status": 400,
     *   "message": "Validation failed",
     *   "timestamp": "2025-01-15T10:30:45.123456",
     *   "path": "/api/transfers",
     *   "validationErrors": {
     *     "amount": "must be greater than 0",
     *     "toEmail": "must not be blank"
     *   }
     * }
     * </pre>
     * </p>
     */
    public static class ErrorResponse {
        /**
         * HTTP status code (e.g., 400, 404, 409, 500).
         * Matches the response HTTP status code.
         */
        private int status;

        /**
         * Human-readable error message describing the problem.
         * Should be safe to display to clients.
         */
        private String message;

        /**
         * Server timestamp when the error occurred.
         * Helps with debugging and correlating errors.
         */
        private LocalDateTime timestamp;

        /**
         * Request path that resulted in the error.
         * Useful for identifying which endpoint failed.
         */
        private String path;

        /**
         * Field-level validation errors for validation failures.
         * Map format: field name → error message.
         * Null for non-validation errors.
         */
        private Map<String, String> validationErrors;

        /**
         * Default constructor for JSON serialization.
         */
        public ErrorResponse() {}

        /**
         * Constructor with all fields.
         *
         * @param status HTTP status code
         * @param message Error message
         * @param timestamp When the error occurred
         * @param path Request path
         * @param validationErrors Field-level errors (can be null)
         */
        public ErrorResponse(int status, String message, LocalDateTime timestamp, String path,
                            Map<String, String> validationErrors) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.path = path;
            this.validationErrors = validationErrors;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Map<String, String> getValidationErrors() {
            return validationErrors;
        }

        public void setValidationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors;
        }

        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        /**
         * Builder class for constructing ErrorResponse objects fluently.
         * <p>
         * Simplifies creation of error responses with optional fields.
         * </p>
         * <p>
         * Example:
         * <pre>
         * ErrorResponse response = ErrorResponse.builder()
         *     .status(400)
         *     .message("Validation failed")
         *     .timestamp(LocalDateTime.now())
         *     .path("/api/transfers")
         *     .validationErrors(errors)
         *     .build();
         * </pre>
         * </p>
         */
        public static class ErrorResponseBuilder {
            private int status;
            private String message;
            private LocalDateTime timestamp;
            private String path;
            private Map<String, String> validationErrors;

            /**
             * Sets HTTP status code.
             * @param status HTTP status code
             * @return This builder for chaining
             */
            public ErrorResponseBuilder status(int status) {
                this.status = status;
                return this;
            }

            /**
             * Sets error message.
             * @param message Human-readable error message
             * @return This builder for chaining
             */
            public ErrorResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            /**
             * Sets error timestamp.
             * @param timestamp When the error occurred
             * @return This builder for chaining
             */
            public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            /**
             * Sets request path.
             * @param path Request URI path that caused the error
             * @return This builder for chaining
             */
            public ErrorResponseBuilder path(String path) {
                this.path = path;
                return this;
            }

            /**
             * Sets field-level validation errors.
             * @param validationErrors Map of field name → error message
             * @return This builder for chaining
             */
            public ErrorResponseBuilder validationErrors(Map<String, String> validationErrors) {
                this.validationErrors = validationErrors;
                return this;
            }

            /**
             * Constructs ErrorResponse from builder state.
             * @return Built ErrorResponse instance
             */
            public ErrorResponse build() {
                return new ErrorResponse(status, message, timestamp, path, validationErrors);
            }
        }
    }
}

