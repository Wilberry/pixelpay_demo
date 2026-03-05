package com.pixelwallet.controller;

import com.pixelwallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * WalletController handles HTTP requests related to wallet management.
 * <p>
 * <strong>Endpoints:</strong>
 * <ul>
 *   <li>GET /api/wallets/balance - Get balance of authenticated user's wallet</li>
 *   <li>POST /api/wallets/fund - Fund (add money to) authenticated user's wallet</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Security:</strong> All endpoints require valid JWT token in Authorization header.
 * The authenticated user can only view their own wallet balance.
 * </p>
 * <p>
 * <strong>Response Format:</strong> Returns BalanceResponse JSON with:
 * <ul>
 *   <li>userEmail: Email of the authenticated user</li>
 *   <li>balance: Current wallet balance as BigDecimal</li>
 *   <li>currency: Currency code (currently always "USD")</li>
 * </ul>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);
    private final WalletService walletService;

    /**
     * Constructs controller with required service dependency.
     *
     * @param walletService WalletService for business logic
     */
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Gets the current authenticated user's wallet balance.
     * <p>
     * <strong>Endpoint:</strong> GET /api/wallets/balance
     * </p>
     * <p>
     * <strong>Authentication:</strong> Requires valid JWT Bearer token.
     * User can only view their own balance.
     * </p>
     * <p>
     * <strong>Response:</strong> HTTP 200 OK with BalanceResponse containing:
     * <pre>
     * {
     *   "userEmail": "alice@example.com",
     *   "balance": "1500.50",
     *   "currency": "USD"
     * }
     * </pre>
     * </p>
     * <p>
     * <strong>Error Scenarios:</strong>
     * <ul>
     *   <li>401 Unauthorized: Missing or invalid JWT token</li>
     *   <li>404 Not Found: User or wallet does not exist (rare)</li>
     * </ul>
     * </p>
     *
     * @param authentication Spring Security Authentication object containing authenticated user
     * @return ResponseEntity with BalanceResponse containing wallet balance
     * @throws com.pixelwallet.exception.UserNotFoundException if user not found
     */
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(Authentication authentication) {
        String email = authentication.getName();
        BigDecimal balance = walletService.getWalletBalance(email);
        
        log.info("Balance queried for user: {}", email);
        
        return ResponseEntity.ok(BalanceResponse.builder()
                .userEmail(email)
                .balance(balance)
                .currency("USD")
                .build());
    }

    /**
     * Funds (adds money to) the authenticated user's wallet.
     * <p>
     * <strong>Endpoint:</strong> POST /api/wallets/fund
     * </p>
     * <p>
     * <strong>Authentication:</strong> Requires valid JWT Bearer token.
     * User can only fund their own wallet.
     * </p>
     * <p>
     * <strong>Request:</strong> JSON body with FundRequest containing:
     * <pre>
     * {
     *   "amount": "100.50",
     *   "currency": "USD"
     * }
     * </pre>
     * </p>
     * <p>
     * <strong>Response:</strong> HTTP 200 OK with BalanceResponse containing:
     * <pre>
     * {
     *   "userEmail": "alice@example.com",
     *   "balance": "1500.50",
     *   "currency": "USD"
     * }
     * </pre>
     * </p>
     *
     * @param fundRequest Request containing amount and currency to fund
     * @param authentication Spring Security Authentication object containing authenticated user
     * @return ResponseEntity with updated BalanceResponse
     */
    @PostMapping("/fund")
    public ResponseEntity<BalanceResponse> fundWallet(
            @RequestBody FundRequest fundRequest,
            Authentication authentication) {
        String email = authentication.getName();
        
        log.info("Fund request for user: {} with amount: {}", email, fundRequest.getAmount());
        
        // Add the requested amount to the wallet (persists to database)
        BigDecimal newBalance = walletService.fundWallet(email, fundRequest.getAmount());
        
        return ResponseEntity.ok(BalanceResponse.builder()
                .userEmail(email)
                .balance(newBalance)
                .currency(fundRequest.getCurrency())
                .build());
    }

    /**
     * FundRequest represents a wallet funding request.
     */
    public static class FundRequest {
        /**
         * Amount to add to the wallet.
         */
        private BigDecimal amount;

        /**
         * Currency code for the funding.
         */
        private String currency;

        /**
         * Default constructor.
         */
        public FundRequest() {}

        /**
         * Constructor with amount and currency.
         * @param amount Amount to fund
         * @param currency Currency code
         */
        public FundRequest(BigDecimal amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }

        /**
         * Gets the amount.
         * @return Amount to fund
         */
        public BigDecimal getAmount() {
            return amount;
        }

        /**
         * Sets the amount.
         * @param amount Amount to set
         */
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        /**
         * Gets the currency.
         * @return Currency code
         */
        public String getCurrency() {
            return currency;
        }

        /**
         * Sets the currency.
         * @param currency Currency code to set
         */
        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }


    /**
     * BalanceResponse represents the response JSON for wallet balance query.
     * <p>
     * Contains the current balance of a user's wallet in a specific currency.
     * </p>
     */
    public static class BalanceResponse {
        /**
         * Email address of the wallet owner.
         */
        private String userEmail;

        /**
         * Current balance amount in the specified currency.
         */
        private BigDecimal balance;

        /**
         * Currency code for the balance (e.g., "USD", "EUR").
         */
        private String currency;

        /**
         * Default constructor for JSON serialization.
         */
        public BalanceResponse() {}

        /**
         * Constructor with all fields.
         *
         * @param userEmail Email of wallet owner
         * @param balance Current balance
         * @param currency Currency code
         */
        public BalanceResponse(String userEmail, BigDecimal balance, String currency) {
            this.userEmail = userEmail;
            this.balance = balance;
            this.currency = currency;
        }

        /**
         * Gets the user email.
         * @return Email address
         */
        public String getUserEmail() {
            return userEmail;
        }

        /**
         * Sets the user email.
         * @param userEmail Email address to set
         */
        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        /**
         * Gets the balance.
         * @return Balance amount
         */
        public BigDecimal getBalance() {
            return balance;
        }

        /**
         * Sets the balance.
         * @param balance Balance amount to set
         */
        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        /**
         * Gets the currency code.
         * @return Currency code
         */
        public String getCurrency() {
            return currency;
        }

        /**
         * Sets the currency code.
         * @param currency Currency code to set
         */
        public void setCurrency(String currency) {
            this.currency = currency;
        }

        /**
         * Creates a BalanceResponseBuilder for fluent construction.
         * @return BalanceResponseBuilder instance
         */
        public static BalanceResponseBuilder builder() {
            return new BalanceResponseBuilder();
        }

        /**
         * Builder class for constructing BalanceResponse objects fluently.
         */
        public static class BalanceResponseBuilder {
            private String userEmail;
            private BigDecimal balance;
            private String currency;

            /**
             * Sets the user email.
             * @param userEmail Email to set
             * @return This builder for chaining
             */
            public BalanceResponseBuilder userEmail(String userEmail) {
                this.userEmail = userEmail;
                return this;
            }

            /**
             * Sets the balance.
             * @param balance Balance to set
             * @return This builder for chaining
             */
            public BalanceResponseBuilder balance(BigDecimal balance) {
                this.balance = balance;
                return this;
            }

            /**
             * Sets the currency code.
             * @param currency Currency code to set
             * @return This builder for chaining
             */
            public BalanceResponseBuilder currency(String currency) {
                this.currency = currency;
                return this;
            }

            /**
             * Constructs BalanceResponse from builder state.
             * @return Built BalanceResponse instance
             */
            public BalanceResponse build() {
                BalanceResponse response = new BalanceResponse();
                response.userEmail = this.userEmail;
                response.balance = this.balance;
                response.currency = this.currency;
                return response;
            }
        }
    }
}
