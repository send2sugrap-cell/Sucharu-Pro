-- =============================================================================
-- Migration: V20261007__create_customer_payments.sql
-- Module 14 Step 03: Customer Payment Recording Foundation
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_payments (
    payment_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    payment_number VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_financial_account_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64),
    amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    payment_method VARCHAR(32) NOT NULL DEFAULT 'CASH',
    payment_date BIGINT NOT NULL,
    reference_number VARCHAR(128),
    external_reference VARCHAR(128),
    notes TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'RECORDED',
    idempotency_key VARCHAR(128),
    cancellation_reason TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_customer_payment_number UNIQUE (tenant_id, payment_number)
);

CREATE INDEX IF NOT EXISTS idx_cp_tenant_customer
    ON customer_payments(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cp_tenant_account
    ON customer_payments(tenant_id, customer_financial_account_id);

CREATE INDEX IF NOT EXISTS idx_cp_tenant_invoice
    ON customer_payments(tenant_id, invoice_id);

CREATE INDEX IF NOT EXISTS idx_cp_tenant_status
    ON customer_payments(tenant_id, project_id, status);

CREATE INDEX IF NOT EXISTS idx_cp_payment_date
    ON customer_payments(tenant_id, project_id, payment_date DESC);

CREATE INDEX IF NOT EXISTS idx_cp_idempotency
    ON customer_payments(tenant_id, project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS customer_payment_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_cp_audit_lookup
    ON customer_payment_audit_events(tenant_id, payment_id, occurred_at DESC);

-- Enable and Force Row Level Security (RLS)
ALTER TABLE customer_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_payments FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_payments ON customer_payments
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_payment_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_payment_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_payment_audit_events ON customer_payment_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
