\connect driver;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS driver_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS driver (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    card_id VARCHAR(64),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    driver_status_id UUID NOT NULL UNIQUE REFERENCES driver_status(id) ON DELETE CASCADE,
    bank_account_id VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS driver_active_idx ON driver(active) WHERE deleted IS FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS driver_card_id_uq ON driver(card_id) WHERE card_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS address (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    driver_id UUID NOT NULL REFERENCES driver(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS address_driver_id_idx ON address(driver_id);

CREATE TABLE IF NOT EXISTS notification_driver (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    customer_id VARCHAR(255) NOT NULL,
    driver_id UUID NOT NULL REFERENCES driver(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS notification_driver_driver_id_idx ON notification_driver(driver_id);
CREATE INDEX IF NOT EXISTS notification_driver_customer_id_idx ON notification_driver(customer_id);
