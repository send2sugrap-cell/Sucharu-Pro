-- ============================================================================
-- Flyway Migration: V20261127__create_affiliate_communication_and_notification_tables.sql
-- Module 20: Affiliate Management & Partner Ecosystem
-- Step 04: Affiliate Communication, Notification & Lifecycle Governance
-- PostgreSQL 14+ Row-Level Security Enabled & Forced
-- ============================================================================

-- 1. AFFILIATE COMMUNICATIONS TABLE
CREATE TABLE IF NOT EXISTS affiliate_communications (
    tenant_id VARCHAR(64) NOT NULL,
    communication_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    recipient_user_id VARCHAR(64) NOT NULL,
    canonical_notification_id VARCHAR(64),
    communication_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(64),
    reference_id VARCHAR(64),
    channels_json TEXT NOT NULL,
    delivered_channels_json TEXT,
    failed_channels_json TEXT,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    idempotency_key VARCHAR(64),
    read_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_affiliate_communications PRIMARY KEY (tenant_id, communication_id),
    CONSTRAINT fk_affiliate_communications_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT chk_affiliate_communications_status CHECK (status IN ('CREATED', 'QUEUED', 'PROCESSING', 'DELIVERED', 'READ', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_affiliate_communications_type CHECK (communication_type IN ('APPLICATION', 'ENROLLMENT', 'PROFILE', 'VERIFICATION', 'PROGRAM', 'GOVERNANCE', 'SECURITY', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_affiliate_comms_tenant_affiliate ON affiliate_communications(tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_comms_tenant_status ON affiliate_communications(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_affiliate_comms_tenant_idempotency ON affiliate_communications(tenant_id, idempotency_key);

-- Enable & Force Row-Level Security for affiliate_communications
ALTER TABLE affiliate_communications ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_communications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_communications_tenant_isolation_policy ON affiliate_communications;
CREATE POLICY affiliate_communications_tenant_isolation_policy ON affiliate_communications
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 2. AFFILIATE NOTIFICATION PREFERENCES TABLE
CREATE TABLE IF NOT EXISTS affiliate_notification_preferences (
    tenant_id VARCHAR(64) NOT NULL,
    preference_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    communication_type VARCHAR(64) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL,
    CONSTRAINT pk_affiliate_notification_preferences PRIMARY KEY (tenant_id, preference_id),
    CONSTRAINT fk_affiliate_notif_pref_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT uq_affiliate_notif_pref UNIQUE (tenant_id, affiliate_id, communication_type),
    CONSTRAINT chk_affiliate_notif_pref_type CHECK (communication_type IN ('APPLICATION', 'ENROLLMENT', 'PROFILE', 'VERIFICATION', 'PROGRAM', 'GOVERNANCE', 'SECURITY', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_affiliate_notif_pref_tenant_affiliate ON affiliate_notification_preferences(tenant_id, affiliate_id);

-- Enable & Force Row-Level Security for affiliate_notification_preferences
ALTER TABLE affiliate_notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_notification_preferences FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_notif_pref_tenant_isolation_policy ON affiliate_notification_preferences;
CREATE POLICY affiliate_notif_pref_tenant_isolation_policy ON affiliate_notification_preferences
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 3. AFFILIATE COMMUNICATION AUDIT RECORDS TABLE
CREATE TABLE IF NOT EXISTS affiliate_communication_audit_records (
    tenant_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    communication_id VARCHAR(64),
    communication_type VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    channels_summary VARCHAR(255),
    reason TEXT,
    correlation_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64),
    record_hash VARCHAR(64) NOT NULL,
    previous_audit_hash VARCHAR(64),
    chain_hash VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    CONSTRAINT pk_affiliate_comm_audit PRIMARY KEY (tenant_id, audit_id),
    CONSTRAINT fk_affiliate_comm_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_affiliate_comm_audit_tenant_affiliate ON affiliate_communication_audit_records(tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_comm_audit_chain ON affiliate_communication_audit_records(tenant_id, chain_hash);

-- Enable & Force Row-Level Security for affiliate_communication_audit_records
ALTER TABLE affiliate_communication_audit_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_communication_audit_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_comm_audit_tenant_isolation_policy ON affiliate_communication_audit_records;
CREATE POLICY affiliate_comm_audit_tenant_isolation_policy ON affiliate_communication_audit_records
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));
