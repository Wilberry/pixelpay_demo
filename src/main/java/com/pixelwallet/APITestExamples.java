package com.pixelwallet;

/**
 * CURL EXAMPLES FOR TESTING THE API
 * 
 * Follow these steps to test the entire workflow:
 */
public class APITestExamples {

    /**
     * STEP 1: Register First User (Alice)
     * 
     * curl -X POST http://localhost:8080/api/auth/register \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "email": "alice@example.com",
     *     "password": "AliceSecure123!"
     *   }' \
     *   -G --data-urlencode "firstName=Alice" --data-urlencode "lastName=Smith"
     *
     * Response (201):
     * {
     *   "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 86400
     * }
     * 
     * Save accessToken as: ALICE_TOKEN
     */

    /**
     * STEP 2: Register Second User (Bob)
     * 
     * curl -X POST http://localhost:8080/api/auth/register \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "email": "bob@example.com",
     *     "password": "BobSecure123!"
     *   }' \
     *   -G --data-urlencode "firstName=Bob" --data-urlencode "lastName=Jones"
     *
     * Save accessToken as: BOB_TOKEN
     */

    /**
     * STEP 3: Check Alice's Balance
     * 
     * curl -X GET http://localhost:8080/api/wallets/balance \
     *   -H "Authorization: Bearer $ALICE_TOKEN"
     *
     * Response (200):
     * {
     *   "userEmail": "alice@example.com",
     *   "balance": 1000.00,
     *   "currency": "USD"
     * }
     */

    /**
     * STEP 4: Transfer from Alice to Bob
     * 
     * curl -X POST http://localhost:8080/api/transfers \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer $ALICE_TOKEN" \
     *   -d '{
     *     "recipientEmail": "bob@example.com",
     *     "amount": 250.50,
     *     "referenceNumber": "TRN-001-'$(date +%s%N)'",
     *     "description": "Salary payment"
     *   }'
     *
     * Response (201):
     * {
     *   "transactionId": "550e8400-e29b-41d4-a716-446655440000",
     *   "senderWalletId": "550e8400-e29b-41d4-a716-446655440001",
     *   "receiverWalletId": "550e8400-e29b-41d4-a716-446655440002",
     *   "senderEmail": "alice@example.com",
     *   "recipientEmail": "bob@example.com",
     *   "amount": 250.50,
     *   "currency": "USD",
     *   "type": "DEBIT",
     *   "status": "SUCCESS",
     *   "referenceNumber": "TRN-001-...",
     *   "createdAt": "2024-03-04T10:30:00"
     * }
     */

    /**
     * STEP 5: Try Duplicate Transfer (Should Fail)
     * 
     * curl -X POST http://localhost:8080/api/transfers \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer $ALICE_TOKEN" \
     *   -d '{
     *     "recipientEmail": "bob@example.com",
     *     "amount": 250.50,
     *     "referenceNumber": "TRN-001-... (same as above)",
     *     "description": "Salary payment"
     *   }'
     *
     * Response (409 Conflict):
     * {
     *   "status": 409,
     *   "message": "Transfer with reference number TRN-001-... already exists with status: SUCCESS",
     *   "timestamp": "2024-03-04T10:32:00",
     *   "path": "/api/transfers"
     * }
     */

    /**
     * STEP 6: Try Insufficient Funds Transfer (Should Fail)
     * 
     * curl -X POST http://localhost:8080/api/transfers \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer $ALICE_TOKEN" \
     *   -d '{
     *     "recipientEmail": "bob@example.com",
     *     "amount": 10000.00,
     *     "referenceNumber": "TRN-002-'$(date +%s%N)'",
     *     "description": "This will fail"
     *   }'
     *
     * Response (400 Bad Request):
     * {
     *   "status": 400,
     *   "message": "Insufficient funds. Available: 749.50, Requested: 10000.00",
     *   "timestamp": "2024-03-04T10:33:00",
     *   "path": "/api/transfers"
     * }
     */

    /**
     * STEP 7: Check Updated Balances
     * 
     * # Alice's balance should be 749.50
     * curl -X GET http://localhost:8080/api/wallets/balance \
     *   -H "Authorization: Bearer $ALICE_TOKEN"
     *
     * # Bob's balance should be 1250.50
     * curl -X GET http://localhost:8080/api/wallets/balance \
     *   -H "Authorization: Bearer $BOB_TOKEN"
     */

    /**
     * INTERVIEW QUESTIONS & EXPECTED ANSWERS:
     * 
     * Q1: How do you ensure atomic transfers?
     * A: @Transactional(isolation = Isolation.SERIALIZABLE) wraps the transfer in a DB transaction.
     *    Debit and credit both succeed or both fail - no partial updates possible.
     * 
     * Q2: What prevents race conditions with concurrent transfers?
     * A: Wallet has @Version field for optimistic locking. Each update increments version.
     *    If two concurrent transactions modify the same wallet, the second gets an 
     *    OptimisticLockingFailureException and can retry.
     * 
     * Q3: How do you handle duplicate requests?
     * A: Each transfer requires a unique referenceNumber (UUID).
     *    Before processing, we check if a transaction with this reference exists.
     *    If found, we return the existing transaction status (idempotency).
     * 
     * Q4: Why use BigDecimal for money?
     * A: Double has precision errors (0.1 + 0.2 != 0.3 in decimal arithmetic).
     *    BigDecimal provides arbitrary precision. Database precision is (19,2).
     * 
     * Q5: How does JWT authentication work?
     * A: On login, server generates an encrypted, signed token containing the user's email.
     *    Client includes token in Authorization header for protected requests.
     *    JwtAuthenticationFilter validates the signature and extracts the user.
     *    Token expires after 24 hours.
     * 
     * Q6: How do you maintain an audit trail?
     * A: Every transaction is stored in the database with sender, receiver, amount, 
     *    status, and timestamp. This enables reconciliation and dispute resolution.
     * 
     * Q7: How would you scale this to millions of users?
     * A: - Use connection pooling (HikariCP)
     *    - Horizontal scaling with load balancer
     *    - Database sharding by user ID
     *    - Redis caching for user lookups
     *    - Message queue (RabbitMQ) for async settlement
     */
}
