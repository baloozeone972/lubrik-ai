-- ══════════════════════════════════════════════════════════════
-- NexusAI - Database Initialization Script
-- This script runs on PostgreSQL container first start
-- ══════════════════════════════════════════════════════════════

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create audit schema
CREATE SCHEMA IF NOT EXISTS audit;

-- Grant permissions
GRANT ALL ON SCHEMA audit TO nexusai;
GRANT ALL ON SCHEMA public TO nexusai;

-- Add comment to database
COMMENT ON DATABASE nexusai_auth IS 'NexusAI - AI Companion Platform Database';

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'NexusAI database initialized successfully at %', NOW();
END $$;
