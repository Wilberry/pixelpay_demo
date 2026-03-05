package com.pixelwallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pixelwallet.model.enum_types.TransactionStatus;
import com.pixelwallet.model.enum_types.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TransferResponseDTO represents the response data for transfer operations.
 * <p>
 * <strong>Purpose:</strong> Contains all details of a completed transfer for API responses.
 * Used by both transfer creation and transaction history endpoints.
 * </p>
 * <p>
 * <strong>Fields:</strong>
 * <ul>
 *   <li>transactionId: UUID of the transaction</li>
 *   <li>senderWalletId/receiverWalletId: Wallet UUIDs</li>
 *   <li>senderEmail/recipientEmail: User emails for display</li>
 *   <li>amount: Transfer amount as BigDecimal</li>
 *   <li>currency: Currency code (e.g., "USD")</li>
 *   <li>type: Transaction type (TRANSFER, etc.)</li>
 *   <li>status: Transaction status (SUCCESS, PENDING, FAILED)</li>
 *   <li>referenceNumber: Unique reference for idempotency</li>
 *   <li>description: Optional transaction description</li>
 *   <li>createdAt: Timestamp when transaction was created</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
public class TransferResponseDTO {

    /**
     * Unique identifier of the transaction.
     */
    @JsonProperty("transactionId")
    private UUID transactionId;

    /**
     * UUID of the sender's wallet.
     */
    @JsonProperty("senderWalletId")
    private UUID senderWalletId;

    /**
     * UUID of the receiver's wallet.
     */
    @JsonProperty("receiverWalletId")
    private UUID receiverWalletId;

    /**
     * Email address of the sender.
     */
    @JsonProperty("senderEmail")
    private String senderEmail;

    /**
     * Email address of the recipient.
     */
    @JsonProperty("recipientEmail")
    private String recipientEmail;

    /**
     * Amount being transferred.
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Currency code for the transfer.
     */
    @JsonProperty("currency")
    private String currency;

    /**
     * Type of transaction.
     */
    @JsonProperty("type")
    private TransactionType type;

    /**
     * Current status of the transaction.
     */
    @JsonProperty("status")
    private TransactionStatus status;

    /**
     * Unique reference number for idempotency.
     */
    @JsonProperty("referenceNumber")
    private String referenceNumber;

    /**
     * Optional description of the transaction.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Timestamp when the transaction was created.
     */
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    public TransferResponseDTO() {
    }

    /**
     * Constructor with all fields.
     */
    public TransferResponseDTO(UUID transactionId, UUID senderWalletId, UUID receiverWalletId,
                               String senderEmail, String recipientEmail, BigDecimal amount,
                               String currency, TransactionType type, TransactionStatus status,
                               String referenceNumber, String description, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.status = status;
        this.referenceNumber = referenceNumber;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getSenderWalletId() {
        return senderWalletId;
    }

    public void setSenderWalletId(UUID senderWalletId) {
        this.senderWalletId = senderWalletId;
    }

    public UUID getReceiverWalletId() {
        return receiverWalletId;
    }

    public void setReceiverWalletId(UUID receiverWalletId) {
        this.receiverWalletId = receiverWalletId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Creates a TransferResponseDTOBuilder for fluent construction.
     * <p>
     * Example:
     * <pre>
     * TransferResponseDTO response = TransferResponseDTO.builder()
     *     .transactionId(transaction.getId())
     *     .senderEmail("alice@example.com")
     *     .recipientEmail("bob@example.com")
     *     .amount(new BigDecimal("100.00"))
     *     .status(TransactionStatus.SUCCESS)
     *     .build();
     * </pre>
     * </p>
     * @return TransferResponseDTOBuilder instance
     */
    public static TransferResponseDTOBuilder builder() {
        return new TransferResponseDTOBuilder();
    }

    /**
     * Builder class for constructing TransferResponseDTO objects fluently.
     */
    public static class TransferResponseDTOBuilder {
        private UUID transactionId;
        private UUID senderWalletId;
        private UUID receiverWalletId;
        private String senderEmail;
        private String recipientEmail;
        private BigDecimal amount;
        private String currency;
        private TransactionType type;
        private TransactionStatus status;
        private String referenceNumber;
        private String description;
        private LocalDateTime createdAt;

        /**
         * Sets the transaction ID.
         * @param transactionId UUID to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder transactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        /**
         * Sets the sender wallet ID.
         * @param senderWalletId UUID to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder senderWalletId(UUID senderWalletId) {
            this.senderWalletId = senderWalletId;
            return this;
        }

        /**
         * Sets the receiver wallet ID.
         * @param receiverWalletId UUID to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder receiverWalletId(UUID receiverWalletId) {
            this.receiverWalletId = receiverWalletId;
            return this;
        }

        /**
         * Sets the sender email.
         * @param senderEmail Email to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder senderEmail(String senderEmail) {
            this.senderEmail = senderEmail;
            return this;
        }

        /**
         * Sets the recipient email.
         * @param recipientEmail Email to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder recipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        /**
         * Sets the transfer amount.
         * @param amount Amount to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the currency code.
         * @param currency Currency to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        /**
         * Sets the transaction type.
         * @param type TransactionType to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the transaction status.
         * @param status TransactionStatus to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the reference number.
         * @param referenceNumber Reference number to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        /**
         * Sets the transaction description.
         * @param description Description to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the creation timestamp.
         * @param createdAt Timestamp to set
         * @return This builder for chaining
         */
        public TransferResponseDTOBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Constructs a TransferResponseDTO instance from builder state.
         * @return Built TransferResponseDTO object
         */
        public TransferResponseDTO build() {
            TransferResponseDTO dto = new TransferResponseDTO();
            dto.setTransactionId(this.transactionId);
            dto.setSenderWalletId(this.senderWalletId);
            dto.setReceiverWalletId(this.receiverWalletId);
            dto.setSenderEmail(this.senderEmail);
            dto.setRecipientEmail(this.recipientEmail);
            dto.setAmount(this.amount);
            dto.setCurrency(this.currency);
            dto.setType(this.type);
            dto.setStatus(this.status);
            dto.setReferenceNumber(this.referenceNumber);
            dto.setDescription(this.description);
            dto.setCreatedAt(this.createdAt);
            return dto;
        }
    }
}
