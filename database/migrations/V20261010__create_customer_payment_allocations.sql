-- =============================================================================
-- Migration: V20261010__create_customer_payment_allocations.sql
-- Module 14 Step 06: Customer Financial Settlement, Payment Allocation & Account Balance Control
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_payment_allocations (
    allocation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_financial_account_id VARCHAR(64) NOT NULL,
    payment_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    allocated_amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    status VARCHAR(32) NOT NULL DEFAULT 'ALLOCATED',
    reversal_reason TEXT,
    idempotency_key VARCHAR(128),
    allocated_at BIGINT NOT NULL,
    allocated_by VARCHAR(64) NOT NULL,
    reversed_at BIGINT,
    reversed_by VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_cpa_tenant_payment
    ON customer_payment_allocations(tenant_id, project_id, payment_id);

CREATE INDEX IF NOT EXISTS idx_cpa_tenant_invoice
    ON customer_payment_allocations(tenant_id, project_id, invoice_id);

CREATE INDEX IF NOT EXISTS idx_cpa_tenant_customer
    ON customer_payment_allocations(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cpa_tenant_status
    ON customer_payment_allocations(tenant_id, project_id, status);

CREATE INDEX IF NOT EXISTS idx_cpa_idempotency
    ON customer_payment_allocations(tenant_id, project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS customer_settlement_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    allocation_id VARCHAR(64),
    payment_id VARCHAR(64),
    invoice_id VARCHAR(64),
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    amount NUMERIC(18, 4),
    reason TEXT,
    occurred_at BIGINT NOT NULL,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_csa_lookup
    ON customer_settlement_audit_events(tenant_id, project_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_csa_payment
    ON customer_settlement_audit_events(tenant_id, payment_id);

CREATE INDEX IF NOT EXISTS idx_csa_invoice
    ON customer_settlement_audit_events(tenant_id, invoice_id);

-- Enable and Force Row Level Security (RLS)
ALTER TABLE customer_payment_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_payment_allocations FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_payment_allocations ON customer_payment_allocations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_settlement_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_settlement_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_settlement_audit_events ON customer_settlement_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
