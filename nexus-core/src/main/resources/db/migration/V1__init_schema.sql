-- ══════════════════════════════════════════════════════════════
-- NexusAI - Initial Database Schema Migration
-- Version: V1
-- Description: Creates core tables for users, subscriptions, and audit
-- ══════════════════════════════════════════════════════════════

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create audit schema
CREATE SCHEMA IF NOT EXISTS audit;

-- ────────────────────────────────────────────────────────────────
-- ENUM Types
-- ────────────────────────────────────────────────────────────────

CREATE TYPE user_role AS ENUM ('USER', 'MODERATOR', 'ADMIN');
CREATE TYPE subscription_type AS ENUM ('FREE', 'STANDARD', 'PREMIUM', 'VIP', 'VIP_PLUS');
CREATE TYPE account_status AS ENUM ('PENDING', 'ACTIVE', 'SUSPENDED', 'DELETED');

-- ────────────────────────────────────────────────────────────────
-- Users Table
-- ────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'USER',
    subscription_type subscription_type NOT NULL DEFAULT 'FREE',
    account_status account_status NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    tokens_remaining INTEGER NOT NULL DEFAULT 100,
    profile_image_url VARCHAR(500),
    locale VARCHAR(10) DEFAULT 'fr_FR',
    timezone VARCHAR(50) DEFAULT 'Europe/Paris',
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(account_status) WHERE account_status != 'DELETED';
CREATE INDEX idx_users_subscription ON users(subscription_type);

-- ────────────────────────────────────────────────────────────────
-- Subscriptions Table
-- ────────────────────────────────────────────────────────────────

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_type subscription_type NOT NULL,
    stripe_subscription_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for subscriptions
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_stripe ON subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

-- ────────────────────────────────────────────────────────────────
-- Refresh Tokens Table
-- ────────────────────────────────────────────────────────────────

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for refresh_tokens
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked = FALSE;

-- ────────────────────────────────────────────────────────────────
-- Email Verification Tokens Table
-- ────────────────────────────────────────────────────────────────

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for email_verification_tokens
CREATE INDEX idx_email_tokens_user ON email_verification_tokens(user_id);
CREATE INDEX idx_email_tokens_token ON email_verification_tokens(token);

-- ────────────────────────────────────────────────────────────────
-- Password Reset Tokens Table
-- ────────────────────────────────────────────────────────────────

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for password_reset_tokens
CREATE INDEX idx_password_tokens_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_tokens_token ON password_reset_tokens(token);

-- ────────────────────────────────────────────────────────────────
-- Audit Logs Table
-- ────────────────────────────────────────────────────────────────

CREATE TABLE audit.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for audit_logs
CREATE INDEX idx_audit_logs_user ON audit.audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit.audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit.audit_logs(created_at DESC);

-- ────────────────────────────────────────────────────────────────
-- Token Transactions Table (for token purchases and usage)
-- ────────────────────────────────────────────────────────────────

CREATE TABLE token_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- PURCHASE, USAGE, BONUS, REFUND
    amount INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    description VARCHAR(500),
    reference_id VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for token_transactions
CREATE INDEX idx_token_trans_user ON token_transactions(user_id);
CREATE INDEX idx_token_trans_type ON token_transactions(type);
CREATE INDEX idx_token_trans_created ON token_transactions(created_at DESC);

-- ────────────────────────────────────────────────────────────────
-- Updated At Trigger Function
-- ────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to tables
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ────────────────────────────────────────────────────────────────
-- Comments
-- ────────────────────────────────────────────────────────────────

COMMENT ON TABLE users IS 'NexusAI user accounts';
COMMENT ON TABLE subscriptions IS 'User subscription plans and Stripe integration';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for authentication';
COMMENT ON TABLE email_verification_tokens IS 'Email verification tokens';
COMMENT ON TABLE password_reset_tokens IS 'Password reset tokens';
COMMENT ON TABLE audit.audit_logs IS 'System-wide audit trail for GDPR compliance';
COMMENT ON TABLE token_transactions IS 'User token balance transactions';
