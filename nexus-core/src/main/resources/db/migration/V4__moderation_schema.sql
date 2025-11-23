-- V4: Moderation Schema
-- Content moderation and safety tables for NexusAI platform

-- Content flags (reports)
CREATE TABLE content_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    content_type VARCHAR(50) NOT NULL,
    content_id UUID NOT NULL,
    flag_reason VARCHAR(100) NOT NULL,
    flag_category VARCHAR(50) NOT NULL,
    description TEXT,
    severity VARCHAR(20) DEFAULT 'medium',
    status VARCHAR(30) DEFAULT 'pending',
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    resolution VARCHAR(50),
    resolution_notes TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Moderation actions
CREATE TABLE moderation_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moderator_id UUID REFERENCES users(id) ON DELETE SET NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    details TEXT,
    duration_hours INTEGER,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    revoked_by UUID REFERENCES users(id),
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoke_reason VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User warnings
CREATE TABLE user_warnings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issued_by UUID REFERENCES users(id) ON DELETE SET NULL,
    warning_type VARCHAR(50) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    severity VARCHAR(20) DEFAULT 'minor',
    points INTEGER DEFAULT 1,
    expires_at TIMESTAMP WITH TIME ZONE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    related_content_id UUID,
    related_content_type VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User bans
CREATE TABLE user_bans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    ban_type VARCHAR(30) NOT NULL DEFAULT 'temporary',
    reason VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    appeal_status VARCHAR(30),
    appeal_text TEXT,
    appeal_response TEXT,
    unbanned_by UUID REFERENCES users(id),
    unbanned_at TIMESTAMP WITH TIME ZONE,
    unban_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Content filter rules
CREATE TABLE content_filter_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    rule_type VARCHAR(50) NOT NULL,
    pattern TEXT NOT NULL,
    action VARCHAR(50) NOT NULL DEFAULT 'flag',
    severity VARCHAR(20) DEFAULT 'medium',
    is_active BOOLEAN DEFAULT TRUE,
    is_regex BOOLEAN DEFAULT FALSE,
    case_sensitive BOOLEAN DEFAULT FALSE,
    applies_to JSONB DEFAULT '["message", "companion", "profile"]',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Auto-moderation logs
CREATE TABLE auto_moderation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_type VARCHAR(50) NOT NULL,
    content_id UUID NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    rule_id UUID REFERENCES content_filter_rules(id) ON DELETE SET NULL,
    detection_type VARCHAR(50) NOT NULL,
    confidence_score DECIMAL(5,4),
    action_taken VARCHAR(50) NOT NULL,
    matched_pattern TEXT,
    original_content TEXT,
    filtered_content TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Moderation queue
CREATE TABLE moderation_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_type VARCHAR(50) NOT NULL,
    content_id UUID NOT NULL,
    priority INTEGER DEFAULT 5,
    queue_type VARCHAR(50) DEFAULT 'review',
    assigned_to UUID REFERENCES users(id),
    assigned_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) DEFAULT 'pending',
    auto_flag_reason VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Incident reports
CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    affected_users JSONB,
    affected_content JSONB,
    reported_by UUID REFERENCES users(id),
    assigned_to UUID REFERENCES users(id),
    status VARCHAR(30) DEFAULT 'open',
    resolution TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID REFERENCES users(id),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- GDPR data requests
CREATE TABLE gdpr_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) DEFAULT 'pending',
    requested_data JSONB,
    processed_by UUID REFERENCES users(id),
    processed_at TIMESTAMP WITH TIME ZONE,
    download_url VARCHAR(500),
    download_expires_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_flags_status ON content_flags(status);
CREATE INDEX idx_flags_content ON content_flags(content_type, content_id);
CREATE INDEX idx_flags_reporter ON content_flags(reporter_id);
CREATE INDEX idx_flags_severity ON content_flags(severity);
CREATE INDEX idx_flags_created ON content_flags(created_at DESC);

CREATE INDEX idx_actions_target ON moderation_actions(target_type, target_id);
CREATE INDEX idx_actions_active ON moderation_actions(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_actions_expires ON moderation_actions(expires_at) WHERE expires_at IS NOT NULL;

CREATE INDEX idx_warnings_user ON user_warnings(user_id);
CREATE INDEX idx_warnings_expires ON user_warnings(expires_at) WHERE expires_at IS NOT NULL;

CREATE INDEX idx_bans_user ON user_bans(user_id);
CREATE INDEX idx_bans_active ON user_bans(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_bans_expires ON user_bans(expires_at) WHERE expires_at IS NOT NULL;

CREATE INDEX idx_filter_rules_active ON content_filter_rules(is_active);
CREATE INDEX idx_filter_rules_type ON content_filter_rules(rule_type);

CREATE INDEX idx_auto_mod_content ON auto_moderation_logs(content_type, content_id);
CREATE INDEX idx_auto_mod_user ON auto_moderation_logs(user_id);
CREATE INDEX idx_auto_mod_created ON auto_moderation_logs(created_at DESC);

CREATE INDEX idx_mod_queue_status ON moderation_queue(status);
CREATE INDEX idx_mod_queue_priority ON moderation_queue(priority DESC, created_at);
CREATE INDEX idx_mod_queue_assigned ON moderation_queue(assigned_to);

CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_severity ON incidents(severity);

CREATE INDEX idx_gdpr_user ON gdpr_requests(user_id);
CREATE INDEX idx_gdpr_status ON gdpr_requests(status);
