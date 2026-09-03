-- V20261124__create_affiliate_management_foundation_tables.sql
-- Module 20 Step 01: Affiliate Management Foundation
-- Canonical PostgreSQL Schema with Multi-Tenant Row-Level Security (RLS)

-- 1. Main Affiliates Profile Table
CREATE TABLE IF NOT EXISTS affiliates (
    affiliate_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64),
    display_name VARCHAR(255) NOT NULL,
    affiliate_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    affiliate_type VARCHAR(32) NOT NULL,
    contact_phone VARCHAR(64),
    contact_email VARCHAR(255),
    tax_id_or_gst VARCHAR(64),
    onboarding_state VARCHAR(32) NOT NULL,
    verification_state VARCHAR(32) NOT NULL,
    agreement_reference VARCHAR(128),
    agreement_version VARCHAR(32),
    agreement_accepted_at BIGINT,
    agreement_accepted_by VARCHAR(64),
    joined_at BIGINT NOT NULL,
    activated_at BIGINT,
    suspended_at BIGINT,
    terminated_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    metadata_json TEXT,
    CONSTRAINT uq_affiliate_tenant_code UNIQUE (tenant_id, affiliate_code),
    CONSTRAINT uq_affiliate_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_affiliates_tenant_status ON affiliates (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_affiliates_tenant_code ON affiliates (tenant_id, affiliate_code);
CREATE INDEX IF NOT EXISTS idx_affiliates_tenant_user ON affiliates (tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_affiliates_tenant_type ON affiliates (tenant_id, affiliate_type);

-- Enable RLS for affiliates
ALTER TABLE affiliates ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliates FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliates_tenant_isolation_policy ON affiliates;
CREATE POLICY affiliates_tenant_isolation_policy ON affiliates
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));

-- 2. Affiliate Eligibility Records Table
CREATE TABLE IF NOT EXISTS affiliate_eligibility_records (
    eligibility_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL REFERENCES affiliates(affiliate_id) ON DELETE CASCADE,
    is_eligible BOOLEAN NOT NULL,
    identity_verified BOOLEAN NOT NULL,
    agreement_accepted BOOLEAN NOT NULL,
    account_active BOOLEAN NOT NULL,
    tax_compliant BOOLEAN NOT NULL,
    business_verified BOOLEAN NOT NULL,
    rejection_reasons TEXT,
    evaluated_at BIGINT NOT NULL,
    evaluated_by VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_affiliate_eligibility_tenant_affiliate ON affiliate_eligibility_records (tenant_id, affiliate_id);

-- Enable RLS for affiliate_eligibility_records
ALTER TABLE affiliate_eligibility_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_eligibility_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_eligibility_tenant_isolation_policy ON affiliate_eligibility_records;
CREATE POLICY affiliate_eligibility_tenant_isolation_policy ON affiliate_eligibility_records
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));

-- 3. Affiliate Append-Only Audit Records Table
CREATE TABLE IF NOT EXISTS affiliate_audit_records (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    record_hash VARCHAR(128) NOT NULL,
    previous_audit_hash VARCHAR(128),
    chain_hash VARCHAR(128) NOT NULL,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_affiliate_audit_tenant_affiliate ON affiliate_audit_records (tenant_id, affiliate_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_affiliate_audit_tenant_correlation ON affiliate_audit_records (tenant_id, correlation_id);

-- Enable RLS for affiliate_audit_records
ALTER TABLE affiliate_audit_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_audit_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_audit_tenant_isolation_policy ON affiliate_audit_records;
CREATE POLICY affiliate_audit_tenant_isolation_policy ON affiliate_audit_records
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));

-- 4. Affiliate Outbox Events Table
CREATE TABLE IF NOT EXISTS affiliate_outbox_events (
    outbox_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_affiliate_outbox_tenant_status ON affiliate_outbox_events (tenant_id, status);

-- Enable RLS for affiliate_outbox_events
ALTER TABLE affiliate_outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_outbox_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_outbox_tenant_isolation_policy ON affiliate_outbox_events;
CREATE POLICY affiliate_outbox_tenant_isolation_policy ON affiliate_outbox_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
