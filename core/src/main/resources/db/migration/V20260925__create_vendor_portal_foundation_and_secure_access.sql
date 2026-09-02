-- ======================================================================================
-- SUCHARU PRO ERP: MODULE 13 STEP 01
-- Flyway Migration: V20260925__create_vendor_portal_foundation_and_secure_access.sql
-- Subsystem: Vendor Portal Foundation & Secure Access
-- ======================================================================================

-- 1. Table: vendor_portal_accounts
CREATE TABLE IF NOT EXISTS vendor_portal_accounts (
    portal_account_id VARCHAR(64) PRIMARY KEY,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INVITED',
    portal_code VARCHAR(100) NOT NULL,
    primary_contact_email VARCHAR(255),
    primary_contact_phone VARCHAR(100),
    activated_at BIGINT,
    activated_by VARCHAR(64),
    suspended_at BIGINT,
    suspended_by VARCHAR(64),
    suspension_reason TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uk_v_portal_account_vendor UNIQUE (tenant_id, vendor_id),
    CONSTRAINT uk_v_portal_account_code UNIQUE (tenant_id, portal_code)
);

CREATE INDEX IF NOT EXISTS idx_v_portal_acc_vendor ON vendor_portal_accounts(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_v_portal_acc_status ON vendor_portal_accounts(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_v_portal_acc_project ON vendor_portal_accounts(tenant_id, project_id);

ALTER TABLE vendor_portal_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_accounts FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_portal_accounts_tenant_isolation') THEN
        CREATE POLICY vendor_portal_accounts_tenant_isolation ON vendor_portal_accounts
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;

-- 2. Table: vendor_portal_memberships
CREATE TABLE IF NOT EXISTS vendor_portal_memberships (
    membership_id VARCHAR(64) PRIMARY KEY,
    portal_account_id VARCHAR(64) NOT NULL REFERENCES vendor_portal_accounts(portal_account_id) ON DELETE CASCADE,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_scope VARCHAR(255) NOT NULL DEFAULT '*',
    role VARCHAR(50) NOT NULL DEFAULT 'VENDOR_OPERATOR',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_ACTIVATION',
    invitation_token VARCHAR(255),
    invitation_expires_at BIGINT,
    activated_at BIGINT,
    last_access_at BIGINT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uk_v_portal_user_membership UNIQUE (tenant_id, vendor_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_v_portal_mem_vendor ON vendor_portal_memberships(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_v_portal_mem_user ON vendor_portal_memberships(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_v_portal_mem_status ON vendor_portal_memberships(tenant_id, status);

ALTER TABLE vendor_portal_memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_memberships FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_portal_memberships_tenant_isolation') THEN
        CREATE POLICY vendor_portal_memberships_tenant_isolation ON vendor_portal_memberships
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;

-- 3. Table: vendor_portal_access_policies
CREATE TABLE IF NOT EXISTS vendor_portal_access_policies (
    policy_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64),
    allow_rfq_submission BOOLEAN NOT NULL DEFAULT TRUE,
    allow_po_acknowledgement BOOLEAN NOT NULL DEFAULT TRUE,
    allow_invoice_submission BOOLEAN NOT NULL DEFAULT TRUE,
    allow_quality_dispute BOOLEAN NOT NULL DEFAULT TRUE,
    require_two_factor_auth BOOLEAN NOT NULL DEFAULT FALSE,
    ip_whitelist VARCHAR(1000),
    session_inactivity_timeout_minutes INT NOT NULL DEFAULT 30,
    max_active_sessions_per_user INT NOT NULL DEFAULT 5,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uk_v_portal_policy_vendor UNIQUE (tenant_id, project_id, vendor_id)
);

CREATE INDEX IF NOT EXISTS idx_v_portal_pol_vendor ON vendor_portal_access_policies(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_v_portal_pol_project ON vendor_portal_access_policies(tenant_id, project_id);

ALTER TABLE vendor_portal_access_policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_access_policies FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_portal_access_policies_tenant_isolation') THEN
        CREATE POLICY vendor_portal_access_policies_tenant_isolation ON vendor_portal_access_policies
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;

-- 4. Table: vendor_portal_sessions
CREATE TABLE IF NOT EXISTS vendor_portal_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    membership_id VARCHAR(64) NOT NULL REFERENCES vendor_portal_memberships(membership_id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL,
    session_token_hash VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    expires_at BIGINT NOT NULL,
    last_activity_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_v_portal_sess_member ON vendor_portal_sessions(tenant_id, membership_id);
CREATE INDEX IF NOT EXISTS idx_v_portal_sess_user ON vendor_portal_sessions(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_v_portal_sess_token ON vendor_portal_sessions(session_token_hash);

ALTER TABLE vendor_portal_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_sessions FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_portal_sessions_tenant_isolation') THEN
        CREATE POLICY vendor_portal_sessions_tenant_isolation ON vendor_portal_sessions
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;

-- 5. Table: vendor_portal_audit_events
CREATE TABLE IF NOT EXISTS vendor_portal_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    membership_id VARCHAR(64),
    actor_user_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_id VARCHAR(64),
    result VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    details TEXT,
    ip_address VARCHAR(100),
    correlation_id VARCHAR(64),
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_v_portal_audit_vendor ON vendor_portal_audit_events(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_v_portal_audit_actor ON vendor_portal_audit_events(tenant_id, actor_user_id);
CREATE INDEX IF NOT EXISTS idx_v_portal_audit_time ON vendor_portal_audit_events(tenant_id, timestamp DESC);

ALTER TABLE vendor_portal_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_audit_events FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_portal_audit_events_tenant_isolation') THEN
        CREATE POLICY vendor_portal_audit_events_tenant_isolation ON vendor_portal_audit_events
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;
