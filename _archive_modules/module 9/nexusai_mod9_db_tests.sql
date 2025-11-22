-- =============================================================================
-- V1__create_moderation_tables.sql
-- Migration Flyway pour créer les tables de modération
-- =============================================================================

-- Table des incidents de modération
CREATE TABLE IF NOT EXISTS moderation_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content_type VARCHAR(50) NOT NULL CHECK (content_type IN ('TEXT', 'IMAGE', 'VIDEO', 'AUDIO')),
    content_hash VARCHAR(64),
    conversation_id VARCHAR(255),
    message_id VARCHAR(255),
    incident_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    confidence DOUBLE PRECISION,
    moderation_scores JSONB,
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    automated BOOLEAN DEFAULT TRUE,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    action_taken VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_incidents_user_id ON moderation_incidents(user_id);
CREATE INDEX idx_incidents_status ON moderation_incidents(status);
CREATE INDEX idx_incidents_severity ON moderation_incidents(severity);
CREATE INDEX idx_incidents_created_at ON moderation_incidents(created_at DESC);
CREATE INDEX idx_incidents_type ON moderation_incidents(incident_type);

-- Index composé pour les requêtes complexes
CREATE INDEX idx_incidents_user_status ON moderation_incidents(user_id, status);
CREATE INDEX idx_incidents_severity_status ON moderation_incidents(severity, status);

-- Table des avertissements utilisateurs
CREATE TABLE IF NOT EXISTS user_warnings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    incident_id UUID REFERENCES moderation_incidents(id) ON DELETE SET NULL,
    warning_type VARCHAR(50) NOT NULL,
    description TEXT,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_warnings_user_id ON user_warnings(user_id);
CREATE INDEX idx_warnings_incident_id ON user_warnings(incident_id);
CREATE INDEX idx_warnings_expires_at ON user_warnings(expires_at);

-- Table des règles de modération
CREATE TABLE IF NOT EXISTS moderation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moderation_level VARCHAR(20) NOT NULL CHECK (moderation_level IN ('STRICT', 'LIGHT', 'OPTIONAL')),
    content_category VARCHAR(50) NOT NULL,
    threshold DOUBLE PRECISION NOT NULL CHECK (threshold >= 0.0 AND threshold <= 1.0),
    action VARCHAR(50) NOT NULL CHECK (action IN ('BLOCK', 'WARN', 'ALLOW')),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_rules_level_category ON moderation_rules(moderation_level, content_category);
CREATE INDEX idx_rules_active ON moderation_rules(active);

-- Contrainte unique: une seule règle par (niveau, catégorie)
CREATE UNIQUE INDEX idx_rules_unique_level_category 
    ON moderation_rules(moderation_level, content_category) 
    WHERE active = TRUE;

-- Table des consentements pour contenu adulte
CREATE TABLE IF NOT EXISTS adult_content_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    consent_type VARCHAR(50) NOT NULL,
    version VARCHAR(10) NOT NULL,
    ip_address INET NOT NULL,
    user_agent TEXT,
    digital_signature TEXT NOT NULL,
    signed_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_consents_user_id ON adult_content_consents(user_id);
CREATE INDEX idx_consents_user_type ON adult_content_consents(user_id, consent_type);
CREATE INDEX idx_consents_expires_at ON adult_content_consents(expires_at);

-- Table des bannissements
CREATE TABLE IF NOT EXISTS user_bans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    reason TEXT NOT NULL,
    banned_by UUID,
    banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    permanent BOOLEAN DEFAULT FALSE,
    related_incident_id UUID REFERENCES moderation_incidents(id),
    active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_bans_user_id ON user_bans(user_id);
CREATE INDEX idx_bans_active ON user_bans(active);

-- Commentaires sur les tables (documentation)
COMMENT ON TABLE moderation_incidents IS 'Incidents de modération détectés automatiquement ou signalés';
COMMENT ON TABLE user_warnings IS 'Avertissements émis aux utilisateurs suite à des incidents';
COMMENT ON TABLE moderation_rules IS 'Règles de modération configurables par niveau';
COMMENT ON TABLE adult_content_consents IS 'Consentements explicites pour contenu adulte (VIP+ uniquement)';
COMMENT ON TABLE user_bans IS 'Historique des bannissements utilisateurs';

-- =============================================================================
-- V2__insert_default_rules.sql
-- Insertion des règles par défaut
-- =============================================================================

-- Règles pour niveau STRICT (FREE/STANDARD)
INSERT INTO moderation_rules (moderation_level, content_category, threshold, action) VALUES
    ('STRICT', 'sexual', 0.3, 'BLOCK'),
    ('STRICT', 'sexual/explicit', 0.2, 'BLOCK'),
    ('STRICT', 'violence', 0.5, 'BLOCK'),
    ('STRICT', 'violence/graphic', 0.3, 'BLOCK'),
    ('STRICT', 'hate', 0.4, 'BLOCK'),
    ('STRICT', 'harassment', 0.4, 'BLOCK'),
    ('STRICT', 'self-harm', 0.2, 'WARN');  -- Warn mais ne bloque pas (pour détecter détresse)

-- Règles pour niveau LIGHT (PREMIUM)
INSERT INTO moderation_rules (moderation_level, content_category, threshold, action) VALUES
    ('LIGHT', 'sexual', 0.6, 'BLOCK'),
    ('LIGHT', 'sexual/explicit', 0.5, 'BLOCK'),
    ('LIGHT', 'violence', 0.7, 'BLOCK'),
    ('LIGHT', 'violence/graphic', 0.6, 'BLOCK'),
    ('LIGHT', 'hate', 0.6, 'BLOCK'),
    ('LIGHT', 'harassment', 0.6, 'BLOCK'),
    ('LIGHT', 'self-harm', 0.3, 'WARN');

-- Règles pour niveau OPTIONAL (VIP+ avec KYC Level 3)
-- Modération minimale, seulement contenu illégal
INSERT INTO moderation_rules (moderation_level, content_category, threshold, action) VALUES
    ('OPTIONAL', 'sexual', 0.95, 'ALLOW'),  -- Permet presque tout
    ('OPTIONAL', 'violence', 0.9, 'ALLOW'),
    ('OPTIONAL', 'hate', 0.85, 'WARN'),
    ('OPTIONAL', 'self-harm', 0.4, 'WARN');

-- NOTES IMPORTANTES:
-- 1. Les règles pour 'sexual/minors' et 'terrorism' ne sont PAS dans cette table
--    car elles sont TOUJOURS appliquées dans le code (seuil ultra-bas)
-- 2. Les seuils sont ajustables via l'API d'administration
-- 3. Les règles inactives (active=false) ne sont pas appliquées

-- =============================================================================
-- V3__create_moderation_stats_view.sql
-- Vue pour les statistiques de modération
-- =============================================================================

CREATE OR REPLACE VIEW moderation_statistics AS
SELECT 
    DATE_TRUNC('day', created_at) AS date,
    incident_type,
    severity,
    COUNT(*) AS incident_count,
    COUNT(CASE WHEN automated = TRUE THEN 1 END) AS automated_count,
    COUNT(CASE WHEN automated = FALSE THEN 1 END) AS manual_count,
    AVG(confidence) AS avg_confidence
FROM moderation_incidents
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', created_at), incident_type, severity
ORDER BY date DESC, incident_count DESC;

COMMENT ON VIEW moderation_statistics IS 'Statistiques quotidiennes de modération (30 derniers jours)';

-- Vue pour les utilisateurs à risque
CREATE OR REPLACE VIEW high_risk_users AS
SELECT 
    user_id,
    COUNT(*) AS total_incidents,
    COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) AS critical_incidents,
    COUNT(CASE WHEN severity = 'HIGH' THEN 1 END) AS high_incidents,
    MAX(created_at) AS last_incident_date,
    ARRAY_AGG(DISTINCT incident_type) AS incident_types
FROM moderation_incidents
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY user_id
HAVING COUNT(CASE WHEN severity IN ('CRITICAL', 'HIGH') THEN 1 END) >= 3
ORDER BY critical_incidents DESC, high_incidents DESC;

COMMENT ON VIEW high_risk_users IS 'Utilisateurs avec 3+ incidents HIGH/CRITICAL dans les 7 derniers jours';

-- =============================================================================
-- TESTS JAVA
-- =============================================================================
