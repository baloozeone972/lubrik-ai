-- ═══════════════════════════════════════════════════════════════
-- V1__create_users_table.sql
-- Migration initiale : Table users
-- ═══════════════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    role VARCHAR(20) DEFAULT 'USER' NOT NULL 
        CHECK (role IN ('USER', 'MODERATOR', 'ADMIN')),
    email_verified BOOLEAN DEFAULT FALSE NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    failed_login_attempts INTEGER DEFAULT 0 NOT NULL,
    account_locked BOOLEAN DEFAULT FALSE NOT NULL,
    lock_time TIMESTAMP,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_active ON users(active);
CREATE INDEX idx_users_created_at ON users(created_at);

COMMENT ON TABLE users IS 'Utilisateurs de la plateforme NexusAI';

-- ═══════════════════════════════════════════════════════════════
-- V2__create_subscriptions_table.sql
-- Table subscriptions
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan VARCHAR(20) DEFAULT 'FREE' NOT NULL 
        CHECK (plan IN ('FREE', 'STANDARD', 'PREMIUM', 'VIP', 'VIP_PLUS')),
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    auto_renewal BOOLEAN DEFAULT FALSE NOT NULL,
    monthly_price DECIMAL(10,2),
    stripe_subscription_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_plan ON subscriptions(plan);
CREATE INDEX idx_subscriptions_end_date ON subscriptions(end_date);
CREATE INDEX idx_subscriptions_stripe ON subscriptions(stripe_subscription_id);

COMMENT ON TABLE subscriptions IS 'Abonnements utilisateurs';

-- ═══════════════════════════════════════════════════════════════
-- V3__create_token_wallets_table.sql
-- Tables token_wallets et token_transactions
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE token_wallets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    balance INTEGER DEFAULT 0 NOT NULL CHECK (balance >= 0),
    total_earned INTEGER DEFAULT 0 NOT NULL,
    total_spent INTEGER DEFAULT 0 NOT NULL,
    last_daily_bonus TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_token_wallets_user ON token_wallets(user_id);

CREATE TABLE token_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_id UUID NOT NULL REFERENCES token_wallets(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL 
        CHECK (type IN ('PURCHASE', 'EARN', 'SPEND', 'REFUND', 'EXPIRE', 
                        'DAILY_BONUS', 'WELCOME_GIFT', 'REFERRAL_REWARD', 
                        'ADMIN_ADJUSTMENT')),
    amount INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    description TEXT,
    reference_id UUID,
    reference_type VARCHAR(50),
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_token_tx_wallet ON token_transactions(wallet_id);
CREATE INDEX idx_token_tx_created ON token_transactions(created_at DESC);
CREATE INDEX idx_token_tx_type ON token_transactions(type);
CREATE INDEX idx_token_tx_reference ON token_transactions(reference_type, reference_id);

COMMENT ON TABLE token_wallets IS 'Portefeuilles de jetons utilisateurs';
COMMENT ON TABLE token_transactions IS 'Historique des transactions de jetons';

-- ═══════════════════════════════════════════════════════════════
-- V4__create_auth_tables.sql
-- Tables d'authentification
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE NOT NULL,
    revoked_at TIMESTAMP,
    revoke_reason VARCHAR(255),
    created_from_ip VARCHAR(45),
    user_agent VARCHAR(500),
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_refresh_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_revoked ON refresh_tokens(revoked);

CREATE TABLE email_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(256) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL 
        CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'INVALID', 
                          'ALREADY_VERIFIED', 'CANCELLED')),
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    request_ip VARCHAR(45),
    verified_ip VARCHAR(45),
    send_attempts INTEGER DEFAULT 0 NOT NULL,
    last_sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_email_verif_token ON email_verifications(token);
CREATE INDEX idx_email_verif_user ON email_verifications(user_id);
CREATE INDEX idx_email_verif_expires ON email_verifications(expires_at);
CREATE INDEX idx_email_verif_status ON email_verifications(status);

CREATE TABLE password_resets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(256) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE NOT NULL,
    used_at TIMESTAMP,
    request_ip VARCHAR(45),
    used_ip VARCHAR(45),
    request_user_agent VARCHAR(500),
    used_user_agent VARCHAR(500),
    invalid_attempts INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_pwd_reset_token ON password_resets(token);
CREATE INDEX idx_pwd_reset_user ON password_resets(user_id);
CREATE INDEX idx_pwd_reset_expires ON password_resets(expires_at);
CREATE INDEX idx_pwd_reset_used ON password_resets(used);

-- ═══════════════════════════════════════════════════════════════
-- V5__create_audit_logs_table.sql
-- Table audit_logs
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    description TEXT,
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    result VARCHAR(20),
    error_message TEXT,
    severity VARCHAR(20) DEFAULT 'INFO' NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_ip ON audit_logs(ip_address);
CREATE INDEX idx_audit_severity ON audit_logs(severity);

COMMENT ON TABLE audit_logs IS 'Journal d''audit de toutes les actions importantes';

-- ═══════════════════════════════════════════════════════════════
-- V6__create_functions.sql
-- Fonctions et triggers utilitaires
-- ═══════════════════════════════════════════════════════════════

-- Fonction pour mettre à jour updated_at automatiquement
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers pour updated_at
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_subscriptions_updated_at 
    BEFORE UPDATE ON subscriptions 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_token_wallets_updated_at 
    BEFORE UPDATE ON token_wallets 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Fonction pour nettoyer les anciennes données
CREATE OR REPLACE FUNCTION cleanup_old_data()
RETURNS void AS $$
BEGIN
    -- Supprimer les tokens expirés depuis plus de 30 jours
    DELETE FROM refresh_tokens 
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
    
    -- Supprimer les anciennes vérifications d'email
    DELETE FROM email_verifications 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
    
    -- Supprimer les anciennes réinitialisations de mot de passe
    DELETE FROM password_resets 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
    
    -- Supprimer les anciens logs d'audit (garder 1 an)
    DELETE FROM audit_logs 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 year';
END;
$$ LANGUAGE plpgsql;

-- ═══════════════════════════════════════════════════════════════
-- V7__insert_initial_data.sql
-- Données initiales
-- ═══════════════════════════════════════════════════════════════

-- Utilisateur admin par défaut (mot de passe: Admin@123)
INSERT INTO users (email, username, password_hash, birth_date, role, email_verified, active)
VALUES (
    'admin@nexusai.com',
    'admin',
    '$2a$10$XqZq6RmNqYYJ7DJYgQ5N4.XcQ7YvJYy5qYGJDZjYYfYYYYYYYYYYY', -- À remplacer par un vrai hash
    '1990-01-01',
    'ADMIN',
    true,
    true
) ON CONFLICT (email) DO NOTHING;

-- Créer wallet pour l'admin
INSERT INTO token_wallets (user_id, balance, total_earned, total_spent)
SELECT id, 10000, 10000, 0 
FROM users 
WHERE email = 'admin@nexusai.com'
ON CONFLICT (user_id) DO NOTHING;

-- Créer subscription pour l'admin
INSERT INTO subscriptions (user_id, plan, start_date)
SELECT id, 'VIP_PLUS', CURRENT_TIMESTAMP
FROM users 
WHERE email = 'admin@nexusai.com'
ON CONFLICT (user_id) DO NOTHING;

COMMENT ON DATABASE nexusai_auth IS 'Base de données NexusAI - Module Authentification';
