\connect driver_location;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Base table for driver-location-service domain driver locations
CREATE TABLE IF NOT EXISTS driver_location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    driver_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    available BOOLEAN NOT NULL DEFAULT FALSE,
    car_id VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS driver_location_driver_id_uq ON driver_location(driver_id);
CREATE INDEX IF NOT EXISTS driver_location_active_idx ON driver_location(active) WHERE deleted IS FALSE;
CREATE INDEX IF NOT EXISTS driver_location_deleted_idx ON driver_location(deleted);

-- Locations associated with driver locations
CREATE TABLE IF NOT EXISTS driver_location_entity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version INTEGER,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    preferred BOOLEAN NOT NULL DEFAULT FALSE,
    geo_id VARCHAR(255),
    ip_address VARCHAR(255),
    country VARCHAR(255),
    city VARCHAR(255),
    latitude VARCHAR(255),
    longitude VARCHAR(255),
    driver_location_id UUID NOT NULL REFERENCES driver_location(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS driver_location_entity_driver_location_id_idx ON driver_location_entity(driver_location_id);
CREATE INDEX IF NOT EXISTS driver_location_entity_active_idx ON driver_location_entity(active) WHERE deleted IS FALSE;
CREATE INDEX IF NOT EXISTS driver_location_entity_deleted_idx ON driver_location_entity(deleted);