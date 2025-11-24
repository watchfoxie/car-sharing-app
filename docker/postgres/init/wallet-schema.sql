\connect wallet;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Base table for wallet-service domain wallets
CREATE TABLE IF NOT EXISTS wallet (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    account_id UUID NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00
);

CREATE UNIQUE INDEX IF NOT EXISTS wallet_account_id_uq ON wallet(account_id);
CREATE INDEX IF NOT EXISTS wallet_active_idx ON wallet(active) WHERE deleted IS FALSE;
CREATE INDEX IF NOT EXISTS wallet_deleted_idx ON wallet(deleted);

-- Credit cards associated with wallets
CREATE TABLE IF NOT EXISTS wallet_credit_card (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    wallet_id UUID NOT NULL REFERENCES wallet(id) ON DELETE CASCADE,
    holder_name VARCHAR(100) NOT NULL,
    alias VARCHAR(255),
    card_brand VARCHAR(50),
    last_four VARCHAR(4) NOT NULL,
    masked_number VARCHAR(255) NOT NULL,
    expiration_date VARCHAR(10) NOT NULL,
    fingerprint VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS wallet_credit_card_fingerprint_uq ON wallet_credit_card(fingerprint);
CREATE INDEX IF NOT EXISTS wallet_credit_card_wallet_id_idx ON wallet_credit_card(wallet_id);
CREATE INDEX IF NOT EXISTS wallet_credit_card_active_idx ON wallet_credit_card(active) WHERE deleted IS FALSE;
CREATE INDEX IF NOT EXISTS wallet_credit_card_deleted_idx ON wallet_credit_card(deleted);

-- Payments associated with wallets
CREATE TABLE IF NOT EXISTS wallet_payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SETTLED',
    payment_type VARCHAR(10) NOT NULL,
    bar_code VARCHAR(255),
    wallet_id UUID NOT NULL REFERENCES wallet(id) ON DELETE CASCADE,
    credit_card_id UUID REFERENCES wallet_credit_card(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS wallet_payment_wallet_id_idx ON wallet_payment(wallet_id);
CREATE INDEX IF NOT EXISTS wallet_payment_credit_card_id_idx ON wallet_payment(credit_card_id);
CREATE INDEX IF NOT EXISTS wallet_payment_status_idx ON wallet_payment(status);
CREATE INDEX IF NOT EXISTS wallet_payment_active_idx ON wallet_payment(active) WHERE deleted IS FALSE;
CREATE INDEX IF NOT EXISTS wallet_payment_deleted_idx ON wallet_payment(deleted);
