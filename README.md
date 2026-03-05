# PixelWallet API 💳

A **professional-grade wallet and transaction API** built with Spring Boot, demonstrating **atomic transfers**, **transactional consistency**, and **JWT-based security**. Designed for fintech applications requiring strict ACID guarantees.

---

## 📋 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Setup & Installation](#-setup--installation)
- [API Endpoints](#-api-endpoints)
- [Screenshots](#-screenshots)
- [Demonstration Highlights](#-demonstration-highlights)
- [Database Schema](#-database-schema)
- [Security & Transactions](#-security--transactions)

---

## 🏗️ Architecture Overview

The application follows a layered architecture with clear separation of concerns:

```
CLIENT (Web/Mobile)
        ↓ HTTP/REST
Spring Boot Application
        ↓
JWT Authentication Filter
(Validates & Extracts Claims)
        ↓
REST Controllers
├─ AuthController (Login/Register)
└─ TransferController (Transfers)
        ↓
Service Layer
├─ AuthService
└─ TransactionService (@Transactional)
        ↓
Repository Layer (JPA)
├─ UserRepository
├─ WalletRepository
└─ TransactionRepository
        ↓
PostgreSQL Database
├─ users
├─ wallets (1:1 with users)
└─ transactions (audit trail)
```

---

## ✨ Key Features

### 1. **Atomic Transfers** ⚛️
- `@Transactional(isolation = Isolation.SERIALIZABLE)` ensures all-or-nothing semantics
- Balance debit and credit happen in a single transaction
- On failure, entire transaction rolls back (no partial updates)

### 2. **Idempotency** 🔄
- Each transfer has a unique `referenceNumber`
- Duplicate transfers detected and rejected
- Safe for retries without creating duplicate transactions

### 3. **Complete Audit Trail** 📊
- Every transaction recorded with sender, receiver, amount, status
- Enables reconciliation and dispute resolution
- Transactional history maintains financial accountability

### 4. **JWT-Based Security** 🔐
- Token-based authentication (stateless)
- Encrypted payload with expiration
- Custom UserDetailsService for Spring Security

### 5. **Optimistic Locking** 🔒
- `@Version` field on Wallet prevents concurrent modifications
- Automatic version increment on updates
- Detects conflicts in high-concurrency scenarios

### 6. **Comprehensive Error Handling** ⚠️
- Global exception handler with structured responses
- Validation of all inputs (Jakarta Validation)
- Custom exceptions for business logic errors

---

## 🛠️ Technology Stack

| Category | Technology |
|----------|-----------|
| **Framework** | Spring Boot 3.2.0 |
| **JPA/ORM** | Hibernate + Spring Data JPA |
| **Database** | PostgreSQL 14+ |
| **Security** | Spring Security + JWT (JJWT 0.12.3) |
| **Authentication** | JWT Token-Based |
| **Validation** | Jakarta Bean Validation |
| **Build Tool** | Maven 3.8+ |
| **Java Version** | Java 17+ |
| **Additional** | Lombok |

---

## 📁 Project Structure

```
pixelpay_demo/
├── src/main/java/com/pixelwallet/
│   ├── PixelwalletApplication.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Wallet.java
│   │   ├── Transaction.java
│   │   └── enum_types/
│   │       ├── TransactionType.java
│   │       └── TransactionStatus.java
│   ├── dto/
│   │   ├── AuthRequestDTO.java
│   │   ├── AuthResponseDTO.java
│   │   ├── TransferRequestDTO.java
│   │   └── TransferResponseDTO.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── WalletRepository.java
│   │   └── TransactionRepository.java
│   ├── service/
│   │   ├── AuthService.java
│   │   └── TransactionService.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── TransferController.java
│   ├── security/
│   │   ├── JwtProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── CustomUserDetailsService.java
│   ├── config/
│   │   └── SecurityConfig.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── InsufficientFundsException.java
│       ├── UserNotFoundException.java
│       └── DuplicateTransactionException.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── README.md
```

---

## 🚀 Setup & Installation

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Step 1: Create PostgreSQL Database
```bash
psql -U postgres -c "CREATE DATABASE pixelwallet;"
```

### Step 2: Update Credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### Step 3: Build
```bash
mvn clean install
```

### Step 4: Run
```bash
mvn spring-boot:run
```

Application starts on `http://localhost:8080`

---

## 📡 API Endpoints

### Authentication - Register
```http
POST /api/auth/register?firstName=John&lastName=Doe
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response (201 Created):
{
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Authentication - Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Transfers - Initiate Transfer
```http
POST /api/transfers
Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "recipientEmail": "jane@example.com",
  "amount": 100.50,
  "referenceNumber": "TRN-2024-001",
  "description": "Payment for services"
}

Response (201 Created):
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "senderWalletId": "550e8400-e29b-41d4-a716-446655440001",
  "receiverWalletId": "550e8400-e29b-41d4-a716-446655440002",
  "senderEmail": "john@example.com",
  "recipientEmail": "jane@example.com",
  "amount": 100.50,
  "currency": "USD",
  "type": "DEBIT",
  "status": "SUCCESS",
  "referenceNumber": "TRN-2024-001",
  "createdAt": "2024-03-04T10:30:00"
}
```

---

## 📸 Screenshots

Screenshots of the PixelWallet frontend demonstrating key features:

### Registration & Login
![Registration Form](screenshots/registration.png)
*User registration with password visibility toggle*

![Login Form](screenshots/login.png)
*Secure login with JWT authentication*

### Wallet Dashboard
![Dashboard](screenshots/dashboard.png)
*Main dashboard showing balance and transaction options*

### Fund Wallet
![Fund Wallet](screenshots/fund-wallet.png)
*Add funds to wallet for testing transfers*

### Make Transfer
![Transfer Form](screenshots/make-transfer.png)
*Atomic transfer between users with reference number*

### Transaction History
![Transaction History](screenshots/transaction-history.png)
*Complete audit trail of all transactions*

---

## 🎯 Demonstration Highlights

### 1. Atomic Transactions
**Q: How do you ensure the transfer is atomic?**

A: Using `@Transactional(isolation = Isolation.SERIALIZABLE)`:
- Wraps entire transfer logic in a database transaction
- Prevents dirty reads, phantom reads, and non-repeatable reads
- If any step fails, entire transaction rolls back
- Database guarantees All-or-Nothing semantics

### 2. Idempotency
**Q: How do you prevent duplicate transfers?**

A: Each API request requires a unique `referenceNumber`. Before processing:
- Check if transaction with this reference already exists
- If found, return existing transaction status
- Makes API safe for retries without side effects

### 3. Financial Precision
**Q: Why BigDecimal instead of Double?**

A: Double has rounding errors (0.1 + 0.2 ≠ 0.3). BigDecimal:
- Provides arbitrary precision arithmetic
- Critical for financial calculations
- Database precision set to (19, 2) for accuracy

### 4. Security
**Q: How is authentication implemented?**

A: JWT-based stateless authentication:
- On login, generate signed token with user's email
- Token includes expiration (24 hours)
- Client sends `Authorization: Bearer <token>` header
- JwtAuthenticationFilter validates on each request

### 5. Audit Trail
**Q: How do you maintain transaction history?**

A: Every transaction stored with:
- Sender, receiver, amount, status, timestamp
- Separate SENT and RECEIVED lists per wallet
- Enables dispute resolution and reconciliation

---

## 🗄️ Database Schema

### Users Table
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
```

### Wallets Table
```sql
CREATE TABLE wallets (
  id UUID PRIMARY KEY,
  user_id UUID UNIQUE NOT NULL,
  balance DECIMAL(19,2) NOT NULL DEFAULT 0,
  currency VARCHAR(3) NOT NULL DEFAULT 'USD',
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);
```

### Transactions Table
```sql
CREATE TABLE transactions (
  id UUID PRIMARY KEY,
  sender_wallet_id UUID,
  receiver_wallet_id UUID,
  amount DECIMAL(19,2) NOT NULL,
  type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  reference_number VARCHAR(255) UNIQUE NOT NULL,
  description VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  FOREIGN KEY (sender_wallet_id) REFERENCES wallets(id),
  FOREIGN KEY (receiver_wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_transactions_sender ON transactions(sender_wallet_id);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_wallet_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
```

---

## 🔐 Security & Transactions

### Transaction Isolation Levels

| Level | Dirty Reads | Non-Repeatable Reads | Phantom Reads |
|-------|------------|----------------------|---------------|
| **SERIALIZABLE** | ✅ No | ✅ No | ✅ No |
| REPEATABLE_READ | ✅ No | ✅ No | ❌ Yes |
| READ_COMMITTED | ✅ No | ❌ Yes | ❌ Yes |

**Why SERIALIZABLE for transfers?**
- Prevents phantom reads (ensures data consistency)
- Highest consistency guarantee
- Acceptable performance cost for financial correctness

### JWT Flow
1. User sends credentials to /api/auth/login
2. AuthenticationManager validates credentials
3. JwtProvider.generateToken() creates signed JWT
4. Client stores token
5. Client sends token in Authorization header
6. JwtAuthenticationFilter validates and extracts user
7. Request proceeds to protected endpoints

---

## 📝 License

Educational and demonstration purposes.

Contact info
Email: wilsonnkwa@gmail.com
+234 810 702 4396

**Created**: March 2026