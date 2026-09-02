-- =====================================================================
-- SUCHARU PRO — MIGRATION V20260901
-- PRODUCTION USER IDENTITY LIFECYCLE, PROFILE & VERIFICATION SCHEMA
-- =====================================================================

-- 1. USER PROFILES
CREATE TABLE IF NOT EXISTS user_profiles (
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    user_id VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    legal_name VARCHAR(150),
    email VARCHAR(150),
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    contact_preferences JSONB NOT NULL DEFAULT '{"email": true, "sms": false, "push": true}'::jsonb,
    email_verified_at TIMESTAMPTZ,
    phone_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id, user_id) REFERENCES auth_accounts(project_id, user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON user_profiles(project_id, email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_profiles_phone ON user_profiles(project_id, phone) WHERE phone IS NOT NULL;

-- 2. USER VERIFICATION TOKENS
CREATE TABLE IF NOT EXISTS user_verification_tokens (
    token_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    user_id VARCHAR(50) NOT NULL,
    verification_type VARCHAR(30) NOT NULL CHECK (verification_type IN ('EMAIL', 'PHONE', 'PASSWORD_RESET')),
    token_hash VARCHAR(64) NOT NULL,
    token_state VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (token_state IN ('PENDING', 'USED', 'EXPIRED', 'REVOKED')),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    FOREIGN KEY (project_id, user_id) REFERENCES auth_accounts(project_id, user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_verification_tokens_lookup ON user_verification_tokens(project_id, user_id, verification_type, token_state);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_hash ON user_verification_tokens(token_hash);

-- 3. PASSWORD HISTORY
CREATE TABLE IF NOT EXISTS password_history (
    history_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    user_id VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (project_id, user_id) REFERENCES auth_accounts(project_id, user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_history_user ON password_history(project_id, user_id, created_at DESC);

-- 4. ROW-LEVEL SECURITY ENFORCEMENT
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_verification_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_history ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_user_profiles ON user_profiles;
CREATE POLICY tenant_isolation_user_profiles ON user_profiles
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));

DROP POLICY IF EXISTS tenant_isolation_user_verification ON user_verification_tokens;
CREATE POLICY tenant_isolation_user_verification ON user_verification_tokens
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));

DROP POLICY IF EXISTS tenant_isolation_password_history ON password_history;
CREATE POLICY tenant_isolation_password_history ON password_history
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));
