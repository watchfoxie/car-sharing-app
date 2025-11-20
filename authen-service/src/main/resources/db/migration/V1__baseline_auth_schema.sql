CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'role_type_enum') THEN
        CREATE TYPE role_type_enum AS ENUM ('CLIENT', 'DRIVER', 'RESTAURANT', 'SHOP');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS auth_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(25),
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS auth_user_email_uq ON auth_user (LOWER(email));
CREATE INDEX IF NOT EXISTS auth_user_active_idx ON auth_user (active) WHERE deleted IS FALSE;
CREATE INDEX IF NOT EXISTS auth_user_deleted_idx ON auth_user (deleted);

CREATE TABLE IF NOT EXISTS auth_role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
    role_type role_type_enum NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS auth_role_user_uq ON auth_role (user_id);
CREATE INDEX IF NOT EXISTS auth_role_type_idx ON auth_role (role_type);
