package com.pixelwallet.dto;

import com.pixelwallet.model.enum_types.TransactionStatus;
import com.pixelwallet.model.enum_types.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponseDTO {

    /**
     * Unique identifier of the transaction.
     */
    private UUID transactionId;

    /**
     * UUID of the sender's wallet.
     */
    private UUID senderWalletId;

    /**
     * UUID of the receiver's wallet.
     */
    private UUID receiverWalletId;

    /**
     * Email address of the sender.
     */
    private String senderEmail;

    /**
     * Email address of the recipient.
     */
    private String recipientEmail;

    /**
     * Amount being transferred.
     */
    private BigDecimal amount;

    /**
     * Currency code for the transfer.
     */
    private String currency;

    /**
     * Type of transaction.
     */
    private TransactionType type;

    /**
     * Current status of the transaction.
     */
    private TransactionStatus status;

    /**
     * Unique reference number for idempotency.
     */
    private String referenceNumber;

    /**
     * Optional description of the transaction.
     */
    private String description;

    /**
     * Timestamp when the transaction was created.
     */
    private LocalDateTime createdAt;

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
            dto.transactionId = this.transactionId;
            dto.senderWalletId = this.senderWalletId;
            dto.receiverWalletId = this.receiverWalletId;
            dto.senderEmail = this.senderEmail;
            dto.recipientEmail = this.recipientEmail;
            dto.amount = this.amount;
            dto.currency = this.currency;
            dto.type = this.type;
            dto.status = this.status;
            dto.referenceNumber = this.referenceNumber;
            dto.description = this.description;
            dto.createdAt = this.createdAt;
            return dto;
        }
    }
}
