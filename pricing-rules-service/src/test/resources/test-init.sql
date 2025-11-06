-- Test database initialization script for Testcontainers
-- Creates pricing schema and required PostgreSQL extensions

-- Create pricing schema
CREATE SCHEMA IF NOT EXISTS pricing;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS citext;

-- Set search_path for test
SET search_path TO pricing, public;
