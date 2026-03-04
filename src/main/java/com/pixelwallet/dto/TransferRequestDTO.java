package com.pixelwallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Data Transfer Object for money transfer requests.
 * <p>
 * <strong>Purpose:</strong>
 * Contains all information required to initiate a money transfer between two user wallets.
 * Supports idempotent transfers through reference numbers to prevent duplicate processing.
 * </p>
 * <p>
 * <strong>Validation Rules:</strong>
 * <ul>
 *   <li>Recipient email must be valid email format and non-blank</li>
 *   <li>Amount must be non-null and greater than 0.01</li>
 *   <li>Reference number is required for idempotency protection</li>
 *   <li>Description is optional but recommended for transaction clarity</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Usage:</strong>
 * <ul>
 *   <li>POST /api/transfers - Initiate money transfer</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Idempotency:</strong>
 * Reference numbers ensure that duplicate requests with the same reference
 * are rejected, preventing accidental double transfers. Clients should generate
 * unique reference numbers (e.g., UUIDs) for each transfer attempt.
 * </p>
 * <p>
 * <strong>Financial Precision:</strong>
 * Uses BigDecimal for amount to ensure precise financial calculations
 * and avoid floating-point arithmetic errors.
 * </p>
 * <p>
 * <strong>Builder Pattern:</strong>
 * Provides fluent builder API for convenient object construction:
 * <pre>
 * TransferRequestDTO request = TransferRequestDTO.builder()
 *     .recipientEmail("bob@example.com")
 *     .amount(new BigDecimal("150.50"))
 *     .referenceNumber(UUID.randomUUID().toString())
 *     .description("Payment for services")
 *     .build();
 * </pre>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
public class TransferRequestDTO {

    /**
     * Email address of the transfer recipient.
     * <p>
     * Must be a registered user in the system. The recipient must have
     * an active wallet to receive the transfer.
     * </p>
     */
    @Email(message = "Recipient email should be valid")
    @NotBlank(message = "Recipient email is required")
    private String recipientEmail;

    /**
     * Transfer amount in the wallet's currency.
     * <p>
     * Must be greater than zero. Currently supports USD currency.
     * Uses BigDecimal for precise financial calculations.
     * </p>
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Unique reference number for idempotency protection.
     * <p>
     * Prevents duplicate processing of the same transfer request.
     * Must be unique across all transfers in the system.
     * Clients should generate unique values (e.g., UUIDs).
     * </p>
     */
    @NotBlank(message = "Reference number is required for idempotency")
    private String referenceNumber;

    /**
     * Optional description of the transfer purpose.
     * <p>
     * Human-readable description that appears in transaction history.
     * Helps users understand the nature of the transfer.
     * </p>
     */
    private String description;

    /**
     * Default constructor for frameworks and deserialization.
     */
    public TransferRequestDTO() {}

    /**
     * Constructor with required fields.
     *
     * @param recipientEmail Email of the transfer recipient
     * @param amount Transfer amount
     * @param referenceNumber Unique reference for idempotency
     */
    public TransferRequestDTO(String recipientEmail, BigDecimal amount, String referenceNumber) {
        this.recipientEmail = recipientEmail;
        this.amount = amount;
        this.referenceNumber = referenceNumber;
    }

    /**
     * Constructor with all fields.
     *
     * @param recipientEmail Email of the transfer recipient
     * @param amount Transfer amount
     * @param referenceNumber Unique reference for idempotency
     * @param description Optional transfer description
     */
    public TransferRequestDTO(String recipientEmail, BigDecimal amount, String referenceNumber, String description) {
        this.recipientEmail = recipientEmail;
        this.amount = amount;
        this.referenceNumber = referenceNumber;
        this.description = description;
    }

    /**
     * Gets the recipient's email address.
     *
     * @return The recipient email
     */
    public String getRecipientEmail() {
        return recipientEmail;
    }

    /**
     * Sets the recipient's email address.
     *
     * @param recipientEmail The recipient email to set
     */
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    /**
     * Gets the transfer amount.
     *
     * @return The transfer amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transfer amount.
     *
     * @param amount The transfer amount to set
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Gets the reference number for idempotency.
     *
     * @return The reference number
     */
    public String getReferenceNumber() {
        return referenceNumber;
    }

    /**
     * Sets the reference number for idempotency.
     *
     * @param referenceNumber The reference number to set
     */
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    /**
     * Gets the transfer description.
     *
     * @return The transfer description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transfer description.
     *
     * @param description The transfer description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Creates a new builder instance for fluent construction.
     *
     * @return A new TransferRequestDTOBuilder instance
     */
    public static TransferRequestDTOBuilder builder() {
        return new TransferRequestDTOBuilder();
    }

    /**
     * Builder class for fluent TransferRequestDTO construction.
     * <p>
     * Provides a fluent API for creating TransferRequestDTO instances
     * with method chaining and optional field setting.
     * </p>
     */
    public static class TransferRequestDTOBuilder {
        private String recipientEmail;
        private BigDecimal amount;
        private String referenceNumber;
        private String description;

        /**
         * Sets the recipient email for the builder.
         *
         * @param recipientEmail The recipient's email address
         * @return This builder instance for method chaining
         */
        public TransferRequestDTOBuilder recipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        /**
         * Sets the amount for the builder.
         *
         * @param amount The transfer amount
         * @return This builder instance for method chaining
         */
        public TransferRequestDTOBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the reference number for the builder.
         *
         * @param referenceNumber The unique reference number
         * @return This builder instance for method chaining
         */
        public TransferRequestDTOBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        /**
         * Sets the description for the builder.
         *
         * @param description The transfer description
         * @return This builder instance for method chaining
         */
        public TransferRequestDTOBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the TransferRequestDTO instance.
         *
         * @return A new TransferRequestDTO with the configured values
         */
        public TransferRequestDTO build() {
            return new TransferRequestDTO(recipientEmail, amount, referenceNumber, description);
        }
    }
}
