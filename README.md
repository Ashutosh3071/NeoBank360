# 🏦 NeoBank — Digital Banking Backend

<div align="center">

![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

A full-featured, production-ready **digital banking REST API** built with Spring Boot. NeoBank supports complete banking workflows — from customer onboarding and KYC verification to UPI payments, NEFT transfers, budgeting, bill management, and a rewards system.

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [API Reference](#-api-reference)
- [Security](#-security)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

---

## ✨ Features

### 👤 Authentication & User Management
- OTP-based email verification for registration
- JWT-based stateless authentication
- CAPTCHA protection on sensitive endpoints
- Forgot username / forgot password flows with OTP
- Role-based access control (`CUSTOMER`, `ADMIN`)

### 📝 Account Onboarding (KYC)
- Full application submission with KYC documents (Aadhaar, PAN, photo)
- Multipart file upload support
- Admin review and approval/rejection workflow
- Application status tracking via OTP-verified email

### 🏦 Account Management
- Multiple account types: `SAVINGS`, `CURRENT`
- Account categories: `REGULAR`, `PREMIUM`
- Account status control: `ACTIVE`, `FROZEN`, `CLOSED`
- Nominee management
- Admin can open additional accounts for existing customers

### 💸 Transactions
| Type | Description |
|------|-------------|
| UPI | Pay via Virtual Payment Address (VPA) |
| NEFT | Bank-to-bank transfers using account & IFSC |
| Self Transfer | Move funds between own accounts |
| Admin Deposit | Admin-initiated deposit to customer account |
| Admin Withdrawal | Admin-initiated withdrawal |

- Full paginated transaction history per account
- Admin view of all system transactions

### 📲 UPI System
- Create & manage multiple UPI IDs per account
- Set / change UPI PIN (OTP-verified)
- VPA lookup to verify recipient
- Per-transaction and daily spending limits
- Block / delete UPI IDs
- Set a primary UPI ID

### 📊 Budget & Bills
- Category-wise monthly budgets (Food, Rent, Entertainment, etc.)
- Budget summary with spend tracking
- Bill reminders with due-date management and status updates (`PENDING`, `PAID`, `OVERDUE`)

### 🏆 Rewards System
- Points awarded on transactions
- Reward history log with action types
- Redeem rewards

### 🔍 AOP Logging
- Aspect-oriented logging across all service methods for full auditability

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8 |
| Mail | Spring Boot Mail (OTP delivery) |
| Validation | Spring Boot Starter Validation |
| Utilities | Lombok |
| Build Tool | Maven |

---

## 🏗 Architecture

```
com.neobank
├── aop/                  # Cross-cutting concerns (LoggingAspect)
├── config/               # Security, JWT filter, CORS configuration
├── controller/           # REST controllers (API layer)
├── dto/                  # Request/Response Data Transfer Objects
│   ├── auth/
│   ├── account/
│   ├── application/
│   ├── transaction/
│   ├── upi/
│   ├── bill/
│   ├── budget/
│   └── reward/
├── entity/               # JPA entities (domain model)
├── exception/            # Global exception handler + custom exceptions
├── repository/           # Spring Data JPA repositories
├── service/              # Business logic layer
└── util/                 # ID / reference number generators
```

The application follows a standard layered architecture: **Controller → Service → Repository → Database**, with cross-cutting AOP logging and a stateless JWT security layer.

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- A mail server / SMTP credentials (for OTP emails)

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/neobank-backend.git
cd neobank-backend/Backend/neobank_backend_v_1
```

### 2. Set Up the Database

```sql
CREATE DATABASE neobank;
```

### 3. Configure Environment

Copy `src/main/resources/application.properties` (or `application.yml`) and fill in your values (see [Configuration](#-configuration) below).

### 4. Build & Run

```bash
./mvnw clean install
./mvnw spring-boot:run
```

The server starts at `http://localhost:8080`.

---

## ⚙️ Configuration

Set the following properties in `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/neobank
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# JWT
jwt.secret=YOUR_SECRET_KEY_MIN_256_BITS
jwt.expiration=86400000

# Mail (SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

> ⚠️ **Never commit real credentials.** Use environment variables or a secrets manager in production.

---

## 📡 API Reference

All endpoints are prefixed with `/api`. Protected routes require an `Authorization: Bearer <token>` header.

### Auth — `/api/auth`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/captcha` | Public | Generate captcha |
| GET | `/check-username` | Public | Check username availability |
| POST | `/send-registration-otp` | Public | Send OTP to email |
| POST | `/register` | Public | Register new user |
| POST | `/login` | Public | Login, returns JWT |
| POST | `/resend-otp` | Public | Resend OTP |
| POST | `/forgot-username` | Public | Trigger username recovery |
| POST | `/forgot-password` | Public | Trigger password reset |
| POST | `/reset-password` | Public | Reset password via OTP |
| POST | `/change-password` | Customer/Admin | Change current password |

### Application (KYC) — `/api/application`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/send-otp` | Public | Send OTP before applying |
| POST | `/submit` | Public | Submit KYC application (multipart) |
| POST | `/submit-auth` | Customer | Submit application (authenticated) |
| POST | `/status/send-otp` | Public | Check application status via OTP |
| POST | `/status/verify` | Public | Verify OTP to see status |
| GET | `/admin/all` | Admin | List all applications |
| GET | `/admin/{id}` | Admin | Get application detail |
| POST | `/admin/approve` | Admin | Approve application |
| POST | `/admin/reject` | Admin | Reject application |

### Accounts — `/api/accounts`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/my` | Customer | Get my accounts |
| GET | `/admin/all` | Admin | Get all accounts |
| PUT | `/admin/status` | Admin | Update account status |

### Transactions — `/api/transaction`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/upi` | Customer | UPI payment |
| POST | `/neft` | Customer | NEFT transfer |
| POST | `/self` | Customer | Self transfer |
| GET | `/my/{accountNumber}` | Customer | Get account transactions |
| POST | `/admin/deposit` | Admin | Deposit to account |
| POST | `/admin/withdraw` | Admin | Withdraw from account |
| GET | `/admin/all` | Admin | All transactions |

### UPI — `/api/upi`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/create` | Customer | Create UPI ID |
| GET | `/my` | Customer | List all my UPI IDs |
| GET | `/account/{accountNumber}` | Customer | UPI IDs for account |
| POST | `/pin/send-otp` | Customer | OTP to set PIN |
| POST | `/pin/set` | Customer | Set UPI PIN |
| POST | `/pin/change` | Customer | Change UPI PIN |
| GET | `/lookup` | Customer | Lookup VPA |
| POST | `/pay` | Customer | Pay via UPI |
| GET | `/transactions/{vpa}` | Customer | UPI transaction history |
| POST | `/set-primary` | Customer | Set primary UPI ID |
| POST | `/block` | Customer | Block UPI ID |
| DELETE | `/delete` | Customer | Delete UPI ID |
| PUT | `/limits` | Customer | Update UPI limits |

### Budgets — `/api/budgets`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/` | Customer | Create budget |
| GET | `/` | Customer | Get my budgets |
| GET | `/summary` | Customer | Budget summary |
| PUT | `/{id}` | Customer | Update budget |
| DELETE | `/{id}` | Customer | Delete budget |

### Bills — `/api/bills`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/` | Customer | Add bill |
| GET | `/` | Customer | Get my bills |
| PUT | `/{id}/status` | Customer | Update bill status |
| DELETE | `/{id}` | Customer | Delete bill |

### Rewards — `/api/rewards`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | Customer | Get reward balance |
| GET | `/history` | Customer | Reward history |
| POST | `/redeem` | Customer | Redeem points |

---

## 🔐 Security

- **Stateless JWT** — no server-side sessions; every request is independently verified.
- **BCrypt** password hashing.
- **CAPTCHA** on registration and account recovery endpoints.
- **OTP verification** (10-minute expiry) for sensitive operations: registration, UPI PIN setup, password reset, and KYC status checks.
- **Role-based route guarding** — `ADMIN` and `CUSTOMER` routes are strictly separated at the security filter chain level.
- **CORS** configured to allow cross-origin requests (set your allowed origins in production).

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

Please follow standard Java/Spring conventions and ensure all new endpoints are covered by the security configuration.

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

<div align="center">
  Built with ❤️ using Spring Boot
</div>
