-- ============================================================================
-- Database Initialization Script
-- ============================================================================
-- This script runs before the main schema creation
-- It sets up extensions and configurations needed for the Document Hub

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Enable full-text search (if not already enabled)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Set timezone
SET timezone = 'UTC';

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Document Hub database initialization started';
    RAISE NOTICE 'PostgreSQL version: %', version();
    RAISE NOTICE 'Current database: %', current_database();
    RAISE NOTICE 'Current user: %', current_user;
END $$;
