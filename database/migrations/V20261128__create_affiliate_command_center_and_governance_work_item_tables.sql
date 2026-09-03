-- ============================================================================
-- Flyway Migration: V20261128__create_affiliate_command_center_and_governance_work_item_tables.sql
-- Module 20: Affiliate Management & Partner Ecosystem
-- Step 05: Affiliate Administrative Command Center & Governance Operations
-- PostgreSQL 14+ Row-Level Security Enabled & Forced
-- ============================================================================

-- 1. AFFILIATE GOVERNANCE WORK ITEMS TABLE
CREATE TABLE IF NOT EXISTS affiliate_governance_work_items (
    tenant_id VARCHAR(64) NOT NULL,
    work_item_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    program_id VARCHAR(64),
    item_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    required_action VARCHAR(64) NOT NULL,
    assigned_role VARCHAR(64),
    assigned_user_id VARCHAR(64),
    resolution_notes TEXT,
    resolved_by_user_id VARCHAR(64),
    resolved_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_affiliate_governance_work_items PRIMARY KEY (tenant_id, work_item_id),
    CONSTRAINT fk_affiliate_work_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT chk_affiliate_work_items_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'DISMISSED', 'ESCALATED')),
    CONSTRAINT chk_affiliate_work_items_priority CHECK (priority IN ('URGENT', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_affiliate_work_items_type CHECK (item_type IN ('PENDING_REVIEW', 'IDENTITY_VERIFICATION', 'BUSINESS_VERIFICATION', 'AGREEMENT_ACCEPTANCE', 'INCOMPLETE_PROFILE', 'ENROLLMENT_ACTION', 'SUSPENDED_REVIEW', 'GOVERNANCE_ISSUE', 'FAILED_NOTIFICATION', 'ADMIN_ACTION_CONFIRMATION'))
);

CREATE INDEX IF NOT EXISTS idx_affiliate_work_items_tenant_affiliate ON affiliate_governance_work_items(tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_work_items_tenant_status ON affiliate_governance_work_items(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_affiliate_work_items_tenant_priority ON affiliate_governance_work_items(tenant_id, priority);

-- Enable & Force Row-Level Security for affiliate_governance_work_items
ALTER TABLE affiliate_governance_work_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_governance_work_items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_work_items_tenant_isolation_policy ON affiliate_governance_work_items;
CREATE POLICY affiliate_work_items_tenant_isolation_policy ON affiliate_governance_work_items
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 2. AFFILIATE COMMAND CENTER AUDIT RECORDS TABLE
CREATE TABLE IF NOT EXISTS affiliate_command_center_audit_records (
    tenant_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64),
    work_item_id VARCHAR(64),
    actor_user_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_state VARCHAR(64),
    new_state VARCHAR(64) NOT NULL,
    reason TEXT,
    correlation_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64),
    record_hash VARCHAR(64) NOT NULL,
    previous_audit_hash VARCHAR(64),
    chain_hash VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    CONSTRAINT pk_affiliate_command_center_audit PRIMARY KEY (tenant_id, audit_id),
    CONSTRAINT fk_affiliate_cc_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_affiliate_cc_audit_tenant_affiliate ON affiliate_command_center_audit_records(tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_cc_audit_chain ON affiliate_command_center_audit_records(tenant_id, chain_hash);

-- Enable & Force Row-Level Security for affiliate_command_center_audit_records
ALTER TABLE affiliate_command_center_audit_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_command_center_audit_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_cc_audit_tenant_isolation_policy ON affiliate_command_center_audit_records;
CREATE POLICY affiliate_cc_audit_tenant_isolation_policy ON affiliate_command_center_audit_records
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 3. AFFILIATE COMMAND CENTER OUTBOX EVENTS TABLE
CREATE TABLE IF NOT EXISTS affiliate_command_center_outbox_events (
    tenant_id VARCHAR(64) NOT NULL,
    outbox_id VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    correlation_id VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL,
    CONSTRAINT pk_affiliate_cc_outbox_events PRIMARY KEY (tenant_id, outbox_id),
    CONSTRAINT fk_affiliate_cc_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_affiliate_cc_outbox_status ON affiliate_command_center_outbox_events(tenant_id, status);

-- Enable & Force Row-Level Security for affiliate_command_center_outbox_events
ALTER TABLE affiliate_command_center_outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_command_center_outbox_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_cc_outbox_tenant_isolation_policy ON affiliate_command_center_outbox_events;
CREATE POLICY affiliate_cc_outbox_tenant_isolation_policy ON affiliate_command_center_outbox_events
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));
