-- =============================================================================
-- Migration: V20261011__create_customer_credit_profiles.sql
-- Module 14 Step 07: Customer Credit Limit, Payment Terms & Receivable Risk Control
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_credit_profiles (
    profile_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    credit_limit NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    payment_terms_type VARCHAR(32) NOT NULL DEFAULT 'DUE_ON_RECEIPT',
    credit_days INT NOT NULL DEFAULT 0,
    requires_advance BOOLEAN NOT NULL DEFAULT FALSE,
    financial_hold BOOLEAN NOT NULL DEFAULT FALSE,
    hold_reason TEXT,
    hold_placed_at BIGINT,
    hold_placed_by VARCHAR(64),
    effective_from BIGINT,
    effective_until BIGINT,
    notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_customer_credit_profile UNIQUE (tenant_id, project_id, customer_id)
);

CREATE INDEX IF NOT EXISTS idx_ccp_tenant_customer
    ON customer_credit_profiles(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_ccp_tenant_hold
    ON customer_credit_profiles(tenant_id, project_id, financial_hold);

CREATE TABLE IF NOT EXISTS customer_credit_control_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_value_json TEXT,
    new_value_json TEXT,
    reason TEXT,
    occurred_at BIGINT NOT NULL,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_ccca_lookup
    ON customer_credit_control_audit_events(tenant_id, project_id, customer_id, occurred_at DESC);

-- Enable and Force Row Level Security (RLS)
ALTER TABLE customer_credit_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_credit_profiles FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_credit_profiles ON customer_credit_profiles
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_credit_control_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_credit_control_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_credit_control_audit_events ON customer_credit_control_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
