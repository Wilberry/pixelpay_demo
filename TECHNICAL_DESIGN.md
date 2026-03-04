# PixelWallet API - Technical Design Document

**Version:** 1.0.0  
**Date:** March 2024  
**Status:** Production Ready  

---

## 1. Executive Summary

PixelWallet API is a Spring Boot-based microservice providing secure wallet and transaction management for fintech applications. The system guarantees:

- **Atomicity**: All-or-nothing transfers via transactional boundaries
- **Consistency**: Atomic multi-step operations maintain data integrity
- **Isolation**: SERIALIZABLE level prevents concurrent modification issues
- **Durability**: PostgreSQL ACID guarantees ensure data persistence

---

## 2. System Architecture

### 2.1 Layered Architecture

```
┌─────────────────────────────────────────┐
│       REST Controllers                  │
│  (HTTP request/response handling)       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       JWT Authentication Filter         │
│   (Security context setup)              │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Service Layer                     │
│  (Business logic, @Transactional)       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Repository Layer (JPA)            │
│   (Database abstraction)                │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Entity Objects                    │
│   (Domain models with @Entity)          │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       PostgreSQL Database               │
│  (ACID-compliant storage)               │
└─────────────────────────────────────────┘
```

### 2.2 Key Design Patterns

#### 2.2.1 Repository Pattern
- Abstracts data access layer
- Simplifies unit testing (mock repositories)
- Enables easy database switching
- Spring Data JPA reduces boilerplate

#### 2.2.2 Service Layer Pattern
- Encapsulates business logic
- Manages transactions (`@Transactional`)
- Provides clear separation of concerns
- Easier to test and maintain

#### 2.2.3 DTO Pattern
- Decouples API contract from entity structure
- Reduces over-fetching/under-fetching
- Validates input at boundary
- Security through controlled exposure

#### 2.2.4 Exception Handler Pattern
- Centralized error handling
- Consistent error responses
- Better client experience
- Easier debugging

---

## 3. Atomic Transfer Implementation

### 3.1 Transfer Flow (Detailed)

```
1. CLIENT SENDS REQUEST
   POST /api/transfers
   {
     "recipientEmail": "bob@example.com",
     "amount": 100.00,
     "referenceNumber": "REF-123",
     "description": "Payment"
   }
   Authorization: Bearer JWT_TOKEN

2. ENTER @TRANSACTIONAL BOUNDARY
   @Transactional(isolation = Isolation.SERIALIZABLE)
   
   2.1 BEGIN TRANSACTION
       - Database obtains lock on transaction record
       - Creates SAVEPOINT for rollback possibility
       
   2.2 IDEMPOTENCY CHECK
       transactionRepository.findByReferenceNumber("REF-123")
       - If exists: Return existing instead of duplicate
       - This prevents double-spending on retries
       
   2.3 LOAD SENDER
       - Query User by email
       - Load associated Wallet (1:1 relationship)
       - Database locks wallet row (SELECT FOR UPDATE)
       
   2.4 LOAD RECIPIENT
       - Query User by email
       - Load associated Wallet
       - Lock wallet row
       
   2.5 VALIDATION CHECKS
       - Amount > 0?
       - Not self-transfer?
       - Email format valid?
       
   2.6 BALANCE CHECK
       if (senderWallet.balance < amount)
           throw InsufficientFundsException
       - Transaction rolls back here on failure
       
   2.7 DEBIT SENDER
       senderWallet.balance = balance - amount
       walletRepository.save(senderWallet)
       - Version field auto-increments
       - Pessimistic lock released after save
       
   2.8 CREDIT RECIPIENT
       recipientWallet.balance = balance + amount
       walletRepository.save(recipientWallet)
       
   2.9 CREATE AUDIT RECORD
       transaction.status = SUCCESS
       transactionRepository.save(transaction)
       - Now in database, part of transaction
       
   2.10 COMMIT TRANSACTION
        - All changes written atomically
        - All locks released
        - WAL (Write-Ahead Log) ensures durability
        
3. EXIT @TRANSACTIONAL BOUNDARY

4. RETURN SUCCESS RESPONSE (201)
   {
     "transactionId": "uuid",
     "status": "SUCCESS",
     "amount": 100.00,
     ...
   }
```

### 3.2 Failure Scenarios

| Scenario | Detection | Action |
|----------|-----------|--------|
| Duplicate reference | Check before debit | Return existing transaction |
| Sender not found | User lookup | Throw UserNotFoundException |
| Recipient not found | User lookup | Throw UserNotFoundException |
| Insufficient funds | Balance check | Throw InsufficientFundsException |
| Self-transfer | Wallet ID check | Throw IllegalArgumentException |
| Amount invalid | Amount validation | Throw IllegalArgumentException |
| DB error | Any exception | Rollback entire transaction |

**Key Point:** At ANY failure point, entire transaction rolls back. No partial updates.

---

## 4. Concurrency Handling

### 4.1 Optimal Locking Strategy

We use **Optimistic Locking** (not Pessimistic):

```
Why Optimistic?
- Reduces lock contention
- Better for high-concurrency scenarios
- Automatic conflict detection
- Works well with distributed systems

Why Not Pessimistic?
- SELECT FOR UPDATE locks entire row
- Blocks concurrent readers
- Deadlock risk
- Poor scalability
```

### 4.2 Optimistic Locking Implementation

```java
@Entity
public class Wallet {
    @Version
    private Long version;  // Auto-incremented on each update
}

// Scenario: Two concurrent transfers from same wallet
Thread 1: wallet.balance = 1000, version = 1
Thread 2: wallet.balance = 1000, version = 1

Thread 1 completes transfer -> wallet.balance = 900, version = 2
Commit successful

Thread 2 tries to commit -> version mismatch!
Database expects version = 2, but Thread 2 has version = 1
-> OptimisticLockingFailureException

Client Application:
- Catches exception
- Retries with exponential backoff
- Eventually succeeds when conflict resolves
```

### 4.3 Race Condition Prevention

**Scenario:** Alice and Bob both withdraw from shared account simultaneously

```
Thread A (withdraw 100):
1. Load wallet: balance=200, version=1
2. Subtract: balance=100, version=1
3. Try to save: version should be 1... ✓
4. Success: balance=100, version=2

Thread B (withdraw 100):
1. Load wallet: balance=200, version=1
2. Subtract: balance=100, version=1
3. Try to save: version should be 1... ✗ (now 2)
4. Failed: OptimisticLockingFailureException
5. Retry: Reload wallet (balance=100, version=2)
6. Subtract: balance=0, version=2
7. Save: Success

Final: balance=0 ✓ Correct!
```

---

## 5. JWT Authentication Architecture

### 5.1 Token Structure

```
Header: {
  "alg": "HS512",
  "typ": "JWT"
}

Payload: {
  "sub": "user@example.com",
  "iat": 1704067200,
  "exp": 1704153600
}

Signature: HMAC-SHA512(
  base64(Header) + "." + base64(Payload),
  SECRET_KEY
)

Complete Token:
eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjE3MDQxNTM2MDB9.
<signature>
```

### 5.2 Authentication Flow

```
1. CLIENT: POST /api/auth/login
   {
     "email": "user@example.com",
     "password": "password123"
   }

2. SERVER: AuthController
   - CustomUserDetailsService loads user by email
   - PasswordEncoder checks hashed password
   - On match: JwtProvider.generateToken()

3. SERVER: JwtProvider.generateToken()
   - Create claims with user email
   - Set expiration (24 hours)
   - Sign with HMAC-SHA512
   - Return token

4. SERVER: RESPONSE (200)
   {
     "accessToken": "eyJ...",
     "tokenType": "Bearer",
     "expiresIn": 86400
   }

5. CLIENT: Save token in secure storage
   - localStorage (vulnerable to XSS)
   - sessionStorage (clears on close)
   - HttpOnly cookie (CSRF risk)
   - Memory (lost on refresh)

6. CLIENT: Subsequent requests with token
   Authorization: Bearer eyJ...

7. SERVER: JwtAuthenticationFilter
   - Extract token from Authorization header
   - Validate signature with SECRET_KEY
   - Both must match exactly
   - Check expiration
   - Extract subject (email)

8. SERVER: CustomUserDetailsService
   - Load user details from database
   - Create Authentication object
   - Set SecurityContext

9. SERVER: Protected endpoint executes
   - Access denied if no valid token
```

### 5.3 Security Considerations

- **Secret Key:** Minimum 256 bits (32 bytes)
  ```
  String secret = Base64.getEncoder().encodeToString(
    new byte[32]  // 256 bits
  );
  ```

- **Token Expiration:** 24 hours balances security vs UX
  - Shorter = More secure, worse UX
  - Longer = Better UX, less secure

- **Token Storage:** Never store in localStorage for sensitive data
  ```javascript
  // Vulnerable
  localStorage.setItem('token', jwtToken);  // XSS attack risk
  
  // Better (not perfect)
  sessionStorage.setItem('token', jwtToken);  // Cleared on close
  
  // Best
  // Store in HttpOnly cookie (server sets it)
  ```

- **HTTPS Only:** Always use TLS in production
  - Prevents man-in-the-middle attacks
  - Protects token in transit

---

## 6. Database Schema & Relationships

### 6.1 Entity Relationships

```
┌──────────────┐
│    User      │
├──────────────┤
│ id (UUID)    │
│ email        │
│ password     │
│ role         │
│ ...          │
└──────┬───────┘
       │ 1:1
       │ OneToOne
       │
┌──────▼───────┐
│   Wallet     │
├──────────────┤
│ id (UUID)    │
│ user_id (FK) │
│ balance      │
│ currency     │
│ version      │
└──────┬───────┘
       │
       ├─ 1:N ─────┬──────────────────┐
       │           │                  │
       │    ┌──────▼────────┐   ┌──────▼────────┐
       │    │ sentTransactions    │receivedTransactions
       │    │               │    │               │
   ┌───────────────────────────────────────────┐
   │         Transaction                        │
   ├────────────────────────────────────────────┤
   │ id                                         │
   │ senderWallet_id (FK) → Wallet             │
   │ receiverWallet_id (FK) → Wallet           │
   │ amount                                     │
   │ type (DEBIT/CREDIT)                       │
   │ status (PENDING/SUCCESS/FAILED)           │
   │ referenceNumber (UNIQUE)                  │
   └────────────────────────────────────────────┘
```

### 6.2 Key Design Decisions

**Q: Why Two Wallet References in Transaction?**

A: Complete audit trail. Transfer involves TWO entities:
- Sender's balance decreases
- Recipient's balance increases
- Transaction records both for reconciliation

**Q: Why Separate from Debit/Credit?**

A: Flexibility. Same transaction model handles:
- Peer-to-peer transfers (sender + receiver both have ID)
- Deposits (only receiver, sender_id = NULL)
- Withdrawals (only sender, receiver_id = NULL)
- Adjustments (internal transactions)

### 6.3 Indexing Strategy

```sql
-- Primary Keys (auto-indexed)
CREATE INDEX idx_pk_users ON users(id);
CREATE INDEX idx_pk_wallets ON wallets(id);
CREATE INDEX idx_pk_transactions ON transactions(id);

-- Foreign Keys (prevents full table scans)
CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_transactions_sender ON transactions(sender_wallet_id);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_wallet_id);

-- Business Logic
CREATE INDEX idx_users_email ON users(email);  -- login lookup
CREATE INDEX idx_transactions_reference ON transactions(reference_number);  -- idempotency
CREATE INDEX idx_transactions_created_at ON transactions(created_at);  -- time range queries

-- Query Performance
CREATE INDEX idx_transactions_composite 
  ON transactions(sender_wallet_id, created_at DESC);
```

---

## 7. Error Handling Strategy

### 7.1 Exception Hierarchy

```
Exception (Java)
├── RuntimeException
│   ├── UserNotFoundException
│   │   └── When user/wallet not found
│   ├── InsufficientFundsException
│   │   └── When balance < transfer amount
│   ├── DuplicateTransactionException
│   │   └── When referenceNumber already exists
│   ├── OptimisticLockingFailureException
│   │   └── When version mismatch (handled by Spring)
│   └── IllegalArgumentException
│       └── Invalid input (amount ≤ 0, self-transfer)
└── Checked Exceptions
    └── (None in this design - modern Spring practice)
```

### 7.2 Global Exception Handler

All exceptions caught in `GlobalExceptionHandler.java`:

```
@RestControllerAdvice
├── handleUserNotFound(UserNotFoundException)
│   └── Returns 404 NOT_FOUND
├── handleInsufficientFunds(InsufficientFundsException)
│   └── Returns 400 BAD_REQUEST
├── handleDuplicateTransaction(DuplicateTransactionException)
│   └── Returns 409 CONFLICT
├── handleValidationException(MethodArgumentNotValidException)
│   └── Returns 400 BAD_REQUEST + field errors
├── handleOptimisticLocking(OptimisticLockingFailureException)
│   └── Returns 409 CONFLICT (client retries)
└── handleGlobal(Exception)
    └── Returns 500 INTERNAL_SERVER_ERROR
```

### 7.3 Error Response Format

```json
{
  "status": 400,
  "message": "Insufficient funds. Available: 50.00, Requested: 100.00",
  "timestamp": "2024-03-04T10:32:00",
  "path": "/api/transfers",
  "validationErrors": {
    "amount": "must be greater than 0",
    "recipientEmail": "must be a valid email"
  }
}
```

---

## 8. Security Hardening

### 8.1 OWASP Top 10 Mitigations

| Risk | Mitigation |
|------|-----------|
| **A01:Injection** | JPA parameterized queries, input validation |
| **A02:Broken Auth** | JWT tokens, strong password hashing (BCrypt) |
| **A03:Broken Access** | Authorization checks in each endpoint |
| **A04:Insecure Deserialization** | Jackson input validation |
| **A05:Broken Access Control** | @PreAuthorize annotations, role checks |
| **A06:Vulnerable & Outdated** | Maven dependency management, security updates |
| **A07:Identification & Auth** | Session timeout (JWT expiration), no default credentials |
| **A08:Software & Data Integrity** | Spring Security CSRF protection disabled (stateless) |
| **A09:Logging & Monitoring** | SLF4J logging at transaction boundaries |
| **A10:SSRF** | No external resource calls in this version |

### 8.2 HTTP Security Headers

```java
// Add to SecurityConfig
http.headers(headers -> headers
    .contentSecurityPolicy("default-src 'self'")
    .and()
    .xssProtection()
    .and()
    .frameOptions().deny()
);
```

### 8.3 Password Security

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

// Usage
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hashedPassword = encoder.encode("plainPassword");
// Cost: 12 iterations = ~100ms per hash
// Stretches attack time significantly
```

---

## 9. Testing Strategy

### 9.1 Test Pyramid

```
          /\
         /  \
        /UnitTests
       /    (40%)
      /____________
     /\           /
    /  \         /
   / E2E \ Integ/
  /Tests  \Tests
 /  (10%)  (50%)
/__________/___________
```

### 9.2 Key Test Scenarios

**Unit Tests (Service Layer)**
```java
@Test
void testInsufficientFunds() {
    // Given: wallet with 50, transfer 100
    // When: initiateTransfer()
    // Then: throw InsufficientFundsException
}

@Test
void testIdempotency() {
    // Given: same referenceNumber
    // When: call initiate twice
    // Then: second call returns existing transaction
}
```

**Integration Tests**
```java
@SpringBootTest
@Transactional
class TransactionServiceIntTest {
    @Test
    void testAtomicTransfer() {
        // Given: Alice 1000, Bob 500
        // When: Transfer 300 from Alice to Bob
        // Then: Alice 700, Bob 800
        // And: Transaction record created
    }
}
```

**End-to-End Tests**
```java
class APIEndToEndTest {
    @Test
    void testCompleteWorkflow() {
        // 1. Register Alice
        // 2. Register Bob
        // 3. Transfer from Alice to Bob
        // 4. Verify balances
        // 5. Check transaction history
    }
}
```

---

## 10. Performance Metrics & SLAs

### 10.1 Expected Performance

| Operation | Target | Benchmark |
|-----------|--------|-----------|
| Login | < 100ms | Password hash verification |
| Transfer | < 500ms | DB transaction + index lookup |
| Balance query | < 50ms | Simple SELECT |
| List transactions | < 200ms | Paged query with indexes |

### 10.2 Scalability Targets

- **Concurrent Users:** 1,000-5,000
- **RPS (Requests/Second):** 100-500
- **Database Connections:** 20-50
- **Memory (JVM):** 512MB - 2GB

### 10.3 Monitoring

```properties
# Enable metrics
management.endpoints.web.expose.include=health,metrics,prometheus

# Key metrics
- jvm.memory.used
- process.cpu.usage
- http.server.requests (duration, count)
- db.connection.pool.active
```

---

## 11. Future Enhancements

### Phase 2
- [ ] Webhook notifications on transfer complete
- [ ] Rate limiting per user
- [ ] Transaction fees calculation
- [ ] Multi-currency support with conversion
- [ ] Recurring transfers scheduling

### Phase 3
- [ ] Real-time balance notifications (WebSocket)
- [ ] CSV export of transaction history
- [ ] Advanced fraud detection
- [ ] Mobile app with biometric auth
- [ ] SWIFT integration for international transfers

### Phase 4
- [ ] Machine learning for anomaly detection
- [ ] Blockchain integration for auditability
- [ ] Distributed ledger for settlement
- [ ] API partner program
- [ ] Compliance reporting (PCI DSS Level 1)

---

## 12. References

- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- JPA/Hibernate: https://hibernate.org
- OWASP Security: https://owasp.org/
- JWT.io: https://jwt.io/
- PostgreSQL Docs: https://www.postgresql.org/docs/

---

**Document Version:** 1.0.0  
**Last Updated:** March 2024  
**Next Review:** June 2024
