\connect rating;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Base table for rating-service domain ratings
CREATE TABLE IF NOT EXISTS ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'RATING-SERVICE',
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    driver_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    rating_score INTEGER NOT NULL,
    comment VARCHAR(1000)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_rating_driver_customer ON ratings(driver_id, customer_id);
CREATE INDEX IF NOT EXISTS ratings_active_idx ON ratings(active) WHERE deleted IS FALSE;
CREATE INDEX IF NOT EXISTS ratings_deleted_idx ON ratings(deleted);
CREATE INDEX IF NOT EXISTS ratings_driver_id_idx ON ratings(driver_id);
CREATE INDEX IF NOT EXISTS ratings_customer_id_idx ON ratings(customer_id);
