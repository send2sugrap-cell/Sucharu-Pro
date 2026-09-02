-- =============================================================================
-- Migration: V20261005__create_customer_financial_accounts.sql
-- Module 14 Step 01: Customer Financial Account Foundation
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_financial_accounts (
    financial_account_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    suspension_reason TEXT,
    closed_reason TEXT,
    notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_cfa_tenant_customer UNIQUE (tenant_id, project_id, customer_id),
    CONSTRAINT uq_cfa_account_number UNIQUE (tenant_id, account_number)
);

CREATE INDEX IF NOT EXISTS idx_cfa_tenant_customer
    ON customer_financial_accounts(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cfa_tenant_status
    ON customer_financial_accounts(tenant_id, project_id, status);

CREATE TABLE IF NOT EXISTS customer_financial_account_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    financial_account_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    reason TEXT,
    occurred_at BIGINT NOT NULL,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_cfa_audit_lookup
    ON customer_financial_account_audit_events(tenant_id, financial_account_id, occurred_at DESC);

-- Enable and Force Row Level Security (RLS)
ALTER TABLE customer_financial_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_financial_accounts FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_financial_accounts ON customer_financial_accounts
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_financial_account_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_financial_account_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_financial_account_audit_events ON customer_financial_account_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
