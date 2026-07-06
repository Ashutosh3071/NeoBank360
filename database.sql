-- http://localhost:8080/api/swagger-ui/index.html

CREATE DATABASE neobank_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'neobank_user'@'localhost' IDENTIFIED BY 'NeoBank@1234Secure!';

GRANT ALL PRIVILEGES ON neobank_db.* TO 'neobank_user'@'localhost';

USE neobank_db;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'CUSTOMER') DEFAULT 'CUSTOMER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    balance DECIMAL(15 , 2 ) DEFAULT 0.00,
    account_type ENUM('SAVINGS', 'CURRENT') NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_accounts_users FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE TABLE transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT,
    type ENUM('DEBIT', 'CREDIT', 'TRANSFER') NOT NULL,
    amount DECIMAL(15 , 2 ) NOT NULL CHECK (amount > 0),
    description VARCHAR(500),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    balance_after DECIMAL(15 , 2 ) NOT NULL,
    CONSTRAINT fk_transactions_accounts FOREIGN KEY (account_id)
        REFERENCES accounts (id)
        ON DELETE RESTRICT
);


ALTER TABLE users
ADD COLUMN aadhaar_number VARCHAR(12) NOT NULL UNIQUE,
ADD COLUMN pan_number VARCHAR(10) NOT NULL UNIQUE;

ALTER TABLE accounts
ADD COLUMN account_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL';

ALTER TABLE accounts
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT FALSE;

-- =============================================
-- SPRINT 2: Budgets, Bills, Rewards
-- =============================================

CREATE TABLE budgets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category ENUM('GROCERIES', 'UTILITIES', 'RENT', 'ENTERTAINMENT', 'TRANSFER', 'OTHER') NOT NULL,
    budget_month DATE NOT NULL,
    limit_amount DECIMAL(15,2) NOT NULL CHECK (limit_amount > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budgets_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_budget (user_id, category, budget_month)
);

CREATE TABLE bills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    biller_name VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    status ENUM('PENDING', 'PAID', 'OVERDUE') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bills_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_bill (user_id, biller_name, due_date)
);

CREATE TABLE rewards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    points_balance INT NOT NULL DEFAULT 0 CHECK (points_balance >= 0),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rewards_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================
-- SPRINT 3: Loan Management
-- =============================================
CREATE TABLE loan_products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(255) NOT NULL UNIQUE,
    min_amount DECIMAL(15,2) NOT NULL CHECK (min_amount > 0),
    max_amount DECIMAL(15,2) NOT NULL,
    annual_interest_rate DECIMAL(5,2) NOT NULL CHECK (annual_interest_rate > 0),
    allowed_tenures VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_max_amount CHECK (max_amount > min_amount)
);
CREATE TABLE loan_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    loan_product_id BIGINT NOT NULL,
    requested_amount DECIMAL(15,2) NOT NULL,
    requested_tenure_months INT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    admin_remarks VARCHAR(500),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP NULL,
    CONSTRAINT fk_loan_apps_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_apps_products FOREIGN KEY (loan_product_id) REFERENCES loan_products(id) ON DELETE RESTRICT
);
CREATE TABLE loan_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_application_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    annual_interest_rate DECIMAL(5,2) NOT NULL,
    tenure_months INT NOT NULL,
    emi_amount DECIMAL(15,2) NOT NULL,
    disbursed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_accounts_apps FOREIGN KEY (loan_application_id) REFERENCES loan_applications(id) ON DELETE RESTRICT,
    CONSTRAINT fk_loan_accounts_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE loan_repayments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_account_id BIGINT NOT NULL,
    instalment_number INT NOT NULL,
    due_date DATE NOT NULL,
    emi_amount DECIMAL(15,2) NOT NULL,
    principal_component DECIMAL(15,2) NOT NULL,
    interest_component DECIMAL(15,2) NOT NULL,
    payment_status ENUM('PENDING', 'PAID', 'OVERDUE') DEFAULT 'PENDING',
    paid_at TIMESTAMP NULL,
    CONSTRAINT fk_loan_repayments_accounts FOREIGN KEY (loan_account_id) REFERENCES loan_accounts(id) ON DELETE CASCADE,
    UNIQUE KEY uq_repayment (loan_account_id, instalment_number)
);

-- =============================================
-- SPRINT 5: Advanced Analytics & Logs
-- =============================================
CREATE TABLE system_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    response_status INT NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    acting_user_id BIGINT NULL,
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message VARCHAR(1000) NULL,
    INDEX idx_audit_status (response_status),
    INDEX idx_audit_timestamp (event_timestamp)
);

-- Performance Indexes (Sprint 5 NFR-1)
ALTER TABLE transactions ADD INDEX idx_txn_account_date (account_id, transaction_date);
ALTER TABLE transactions ADD INDEX idx_txn_date (transaction_date);

