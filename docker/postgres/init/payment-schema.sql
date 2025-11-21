\connect payment;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS bank_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    account_number VARCHAR(64) NOT NULL UNIQUE,
    account_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    currency VARCHAR(16) NOT NULL,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS credit_card (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    holder_name VARCHAR(150) NOT NULL,
    card_alias VARCHAR(150) NOT NULL,
    last_four VARCHAR(4) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    expiration_date VARCHAR(10) NOT NULL,
    card_token VARCHAR(255) NOT NULL UNIQUE,
    bank_account_id UUID NOT NULL REFERENCES bank_account(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS credit_card_account_idx ON credit_card(bank_account_id);

CREATE TABLE IF NOT EXISTS payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    amount NUMERIC(19,2) NOT NULL,
    bar_code VARCHAR(255),
    status VARCHAR(40) NOT NULL,
    payment_type VARCHAR(40) NOT NULL,
    bank_account_id UUID NOT NULL REFERENCES bank_account(id) ON DELETE CASCADE,
    credit_card_id UUID REFERENCES credit_card(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS payment_account_idx ON payment(bank_account_id);
CREATE INDEX IF NOT EXISTS payment_status_idx ON payment(status);
