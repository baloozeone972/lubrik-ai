-- V5: Payment and Analytics Schema
-- Subscription, payment, and analytics tables for NexusAI platform

-- ============================================
-- PAYMENT & SUBSCRIPTION TABLES
-- ============================================

-- Subscription plans
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    description TEXT,
    tier VARCHAR(30) NOT NULL,
    price_monthly DECIMAL(10,2) NOT NULL,
    price_yearly DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'EUR',
    stripe_price_id_monthly VARCHAR(100),
    stripe_price_id_yearly VARCHAR(100),
    features JSONB NOT NULL DEFAULT '[]',
    limits JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    is_popular BOOLEAN DEFAULT FALSE,
    trial_days INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User subscriptions
CREATE TABLE user_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    stripe_subscription_id VARCHAR(100) UNIQUE,
    stripe_customer_id VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'active',
    billing_cycle VARCHAR(20) DEFAULT 'monthly',
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    cancel_reason VARCHAR(255),
    trial_start TIMESTAMP WITH TIME ZONE,
    trial_end TIMESTAMP WITH TIME ZONE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Payment methods
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stripe_payment_method_id VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    card_brand VARCHAR(30),
    card_last4 VARCHAR(4),
    card_exp_month INTEGER,
    card_exp_year INTEGER,
    is_default BOOLEAN DEFAULT FALSE,
    billing_address JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Payment transactions
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES user_subscriptions(id),
    stripe_payment_intent_id VARCHAR(100) UNIQUE,
    stripe_invoice_id VARCHAR(100),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    status VARCHAR(30) NOT NULL,
    payment_method_id UUID REFERENCES payment_methods(id),
    description VARCHAR(255),
    failure_code VARCHAR(100),
    failure_message TEXT,
    receipt_url VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Invoices
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES user_subscriptions(id),
    stripe_invoice_id VARCHAR(100) UNIQUE,
    invoice_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount_due DECIMAL(10,2) NOT NULL,
    amount_paid DECIMAL(10,2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'EUR',
    due_date TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    invoice_pdf_url VARCHAR(500),
    hosted_invoice_url VARCHAR(500),
    billing_reason VARCHAR(50),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Usage records (for metered billing)
CREATE TABLE usage_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES user_subscriptions(id),
    usage_type VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    unit VARCHAR(30) DEFAULT 'tokens',
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    reported_to_stripe BOOLEAN DEFAULT FALSE,
    stripe_usage_record_id VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- ANALYTICS TABLES
-- ============================================

-- Analytics events
CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    session_id VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    event_action VARCHAR(100),
    event_label VARCHAR(255),
    event_value DECIMAL(15,4),
    properties JSONB,
    device_info JSONB,
    geo_info JSONB,
    referrer VARCHAR(500),
    page_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User sessions
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    device_type VARCHAR(30),
    browser VARCHAR(50),
    os VARCHAR(50),
    ip_address INET,
    country VARCHAR(2),
    city VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    page_views INTEGER DEFAULT 0,
    events_count INTEGER DEFAULT 0,
    is_bounce BOOLEAN DEFAULT FALSE
);

-- Daily metrics (aggregated)
CREATE TABLE daily_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date DATE NOT NULL,
    metric_type VARCHAR(100) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    dimension VARCHAR(100),
    dimension_value VARCHAR(255),
    value_count BIGINT DEFAULT 0,
    value_sum DECIMAL(20,4) DEFAULT 0,
    value_avg DECIMAL(20,4) DEFAULT 0,
    value_min DECIMAL(20,4),
    value_max DECIMAL(20,4),
    unique_users INTEGER DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(metric_date, metric_type, metric_name, dimension, dimension_value)
);

-- User activity summary
CREATE TABLE user_activity_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary_date DATE NOT NULL,
    conversations_started INTEGER DEFAULT 0,
    messages_sent INTEGER DEFAULT 0,
    messages_received INTEGER DEFAULT 0,
    tokens_used INTEGER DEFAULT 0,
    companions_created INTEGER DEFAULT 0,
    time_spent_seconds INTEGER DEFAULT 0,
    most_used_companion_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, summary_date)
);

-- Companion analytics
CREATE TABLE companion_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    companion_id UUID NOT NULL REFERENCES companions(id) ON DELETE CASCADE,
    analytics_date DATE NOT NULL,
    total_conversations INTEGER DEFAULT 0,
    total_messages INTEGER DEFAULT 0,
    unique_users INTEGER DEFAULT 0,
    avg_conversation_length DECIMAL(10,2) DEFAULT 0,
    avg_response_time_ms INTEGER DEFAULT 0,
    avg_rating DECIMAL(3,2) DEFAULT 0,
    tokens_consumed INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(companion_id, analytics_date)
);

-- Feature usage tracking
CREATE TABLE feature_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    feature_name VARCHAR(100) NOT NULL,
    feature_category VARCHAR(50),
    usage_count INTEGER DEFAULT 1,
    first_used_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- A/B Test experiments
CREATE TABLE ab_experiments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(20) DEFAULT 'draft',
    variants JSONB NOT NULL,
    traffic_allocation JSONB,
    target_audience JSONB,
    primary_metric VARCHAR(100),
    secondary_metrics JSONB,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    winner_variant VARCHAR(50),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- A/B Test assignments
CREATE TABLE ab_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_id UUID NOT NULL REFERENCES ab_experiments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    variant VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    converted BOOLEAN DEFAULT FALSE,
    converted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(experiment_id, user_id)
);

-- ============================================
-- INDEXES
-- ============================================

-- Payment indexes
CREATE INDEX idx_subscriptions_user ON user_subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON user_subscriptions(status);
CREATE INDEX idx_subscriptions_stripe ON user_subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscriptions_period ON user_subscriptions(current_period_end);

CREATE INDEX idx_payment_methods_user ON payment_methods(user_id);
CREATE INDEX idx_transactions_user ON payment_transactions(user_id);
CREATE INDEX idx_transactions_status ON payment_transactions(status);
CREATE INDEX idx_transactions_created ON payment_transactions(created_at DESC);

CREATE INDEX idx_invoices_user ON invoices(user_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_usage_records_user ON usage_records(user_id);
CREATE INDEX idx_usage_records_period ON usage_records(period_start, period_end);

-- Analytics indexes
CREATE INDEX idx_events_user ON analytics_events(user_id);
CREATE INDEX idx_events_type ON analytics_events(event_type);
CREATE INDEX idx_events_category ON analytics_events(event_category);
CREATE INDEX idx_events_created ON analytics_events(created_at DESC);
CREATE INDEX idx_events_session ON analytics_events(session_id);

CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_started ON user_sessions(started_at DESC);

CREATE INDEX idx_daily_metrics_date ON daily_metrics(metric_date DESC);
CREATE INDEX idx_daily_metrics_type ON daily_metrics(metric_type, metric_name);

CREATE INDEX idx_activity_user_date ON user_activity_summary(user_id, summary_date DESC);
CREATE INDEX idx_companion_analytics_date ON companion_analytics(companion_id, analytics_date DESC);

CREATE INDEX idx_feature_usage_feature ON feature_usage(feature_name);
CREATE INDEX idx_ab_assignments_experiment ON ab_assignments(experiment_id);

-- ============================================
-- INSERT DEFAULT SUBSCRIPTION PLANS
-- ============================================

INSERT INTO subscription_plans (name, display_name, description, tier, price_monthly, price_yearly, features, limits, sort_order) VALUES
('free', 'Free', 'Get started with basic features', 'free', 0, 0,
 '["1 AI Companion", "100 messages/day", "Basic personality", "Community support"]',
 '{"companions": 1, "messages_per_day": 100, "tokens_per_month": 10000}', 1),

('starter', 'Starter', 'Perfect for individuals', 'starter', 9.99, 99.99,
 '["3 AI Companions", "1000 messages/day", "Custom personalities", "Voice messages", "Email support"]',
 '{"companions": 3, "messages_per_day": 1000, "tokens_per_month": 100000, "voice_enabled": true}', 2),

('pro', 'Professional', 'For power users', 'pro', 24.99, 249.99,
 '["10 AI Companions", "Unlimited messages", "Advanced AI models", "API access", "Priority support"]',
 '{"companions": 10, "messages_per_day": -1, "tokens_per_month": 500000, "api_access": true}', 3),

('enterprise', 'Enterprise', 'For teams and businesses', 'enterprise', 99.99, 999.99,
 '["Unlimited Companions", "Unlimited messages", "Custom AI training", "Dedicated support", "SLA guarantee", "SSO integration"]',
 '{"companions": -1, "messages_per_day": -1, "tokens_per_month": -1, "custom_training": true, "sso": true}', 4);
