-- Testcontainers bootstrap script for PostgreSQL integration tests
-- Ensures the identity schema exists and citext extension is available

CREATE SCHEMA IF NOT EXISTS identity;
CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;
