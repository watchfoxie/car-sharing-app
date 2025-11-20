\connect customer;

-- Base table for customer-service domain users
CREATE TABLE IF NOT EXISTS customer (
    id UUID PRIMARY KEY,
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    bank_account_id VARCHAR(255),
    driver_id VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS customer_email_uq ON customer(email);

-- Notifications awaiting acknowledgement by the owning customer
CREATE TABLE IF NOT EXISTS notification_customer (
    id UUID PRIMARY KEY,
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    driver_id VARCHAR(255) NOT NULL,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS notification_customer_customer_id_idx ON notification_customer(customer_id);
