-- =====================================================================
-- SUCHARU PRO — MIGRATION V20260830
-- CANONICAL AUTHENTICATION, IDENTITY, SESSION & AUDIT SCHEMA
-- =====================================================================

-- 1. AUTHENTICATION ACCOUNTS
CREATE TABLE IF NOT EXISTS auth_accounts (
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    user_id VARCHAR(50) NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(100) NOT NULL,
    password_algorithm VARCHAR(50) NOT NULL DEFAULT 'PBKDF2_SHA256',
    role VARCHAR(50) NOT NULL CHECK (role IN ('GUEST', 'CUSTOMER', 'AFFILIATE', 'STAFF', 'MANAGER', 'ADMIN', 'ACCOUNTS', 'WAREHOUSE', 'QC_INSPECTOR', 'LOGISTICS', 'VENDOR', 'SUPER_ADMIN')),
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOCKED', 'PENDING_VERIFICATION', 'DELETED')),
    failed_login_count INT NOT NULL DEFAULT 0,
    lock_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, user_id),
    UNIQUE (project_id, username)
);

CREATE INDEX IF NOT EXISTS idx_auth_accounts_email ON auth_accounts(project_id, email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_auth_accounts_phone ON auth_accounts(project_id, phone) WHERE phone IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_auth_accounts_status ON auth_accounts(project_id, account_status);

-- 2. AUTHENTICATION SESSIONS
CREATE TABLE IF NOT EXISTS auth_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    user_id VARCHAR(50) NOT NULL,
    session_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (session_status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    refresh_token_hash VARCHAR(64) NOT NULL,
    device_name VARCHAR(100),
    client_ip VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 1,
    FOREIGN KEY (project_id, user_id) REFERENCES auth_accounts(project_id, user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_user ON auth_sessions(project_id, user_id, session_status);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_refresh_hash ON auth_sessions(refresh_token_hash);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_expires ON auth_sessions(expires_at) WHERE session_status = 'ACTIVE';

-- 3. AUTHENTICATION AUDIT EVENTS (APPEND-ONLY)
CREATE TABLE IF NOT EXISTS auth_audit_events (
    event_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    user_id VARCHAR(50),
    session_id VARCHAR(64),
    event_type VARCHAR(50) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    correlation_id VARCHAR(64),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_auth_audit_tenant_user ON auth_audit_events(project_id, user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_auth_audit_type ON auth_audit_events(project_id, event_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_auth_audit_correlation ON auth_audit_events(correlation_id) WHERE correlation_id IS NOT NULL;

-- 4. ROW-LEVEL SECURITY ENFORCEMENT
ALTER TABLE auth_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE auth_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE auth_audit_events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_auth_accounts ON auth_accounts;
CREATE POLICY tenant_isolation_auth_accounts ON auth_accounts
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));

DROP POLICY IF EXISTS tenant_isolation_auth_sessions ON auth_sessions;
CREATE POLICY tenant_isolation_auth_sessions ON auth_sessions
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));

DROP POLICY IF EXISTS tenant_isolation_auth_audit ON auth_audit_events;
CREATE POLICY tenant_isolation_auth_audit ON auth_audit_events
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));
