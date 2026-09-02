-- =============================================================================
-- Migration: V20261008__create_customer_credits_and_refunds.sql
-- Module 14 Step 04: Customer Advance, Credit, Adjustment & Refund Foundation
-- =============================================================================

-- 1. Customer Advances (Unallocated deposits / advances)
CREATE TABLE IF NOT EXISTS customer_advances (
    advance_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_financial_account_id VARCHAR(64) NOT NULL,
    advance_number VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    allocated_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    available_amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    payment_method VARCHAR(32) NOT NULL DEFAULT 'CASH',
    receipt_date BIGINT NOT NULL,
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
    CONSTRAINT uq_customer_advance_number UNIQUE (tenant_id, advance_number)
);

CREATE INDEX IF NOT EXISTS idx_cadv_tenant_customer
    ON customer_advances(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cadv_tenant_account
    ON customer_advances(tenant_id, customer_financial_account_id);

CREATE INDEX IF NOT EXISTS idx_cadv_tenant_status
    ON customer_advances(tenant_id, project_id, status);

CREATE INDEX IF NOT EXISTS idx_cadv_idempotency
    ON customer_advances(tenant_id, project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- 2. Customer Credit Allocations (Applying advances/credits to invoices)
CREATE TABLE IF NOT EXISTS customer_credit_allocations (
    allocation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_financial_account_id VARCHAR(64) NOT NULL,
    advance_id VARCHAR(64),
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

CREATE INDEX IF NOT EXISTS idx_cca_tenant_invoice
    ON customer_credit_allocations(tenant_id, project_id, invoice_id);

CREATE INDEX IF NOT EXISTS idx_cca_tenant_advance
    ON customer_credit_allocations(tenant_id, advance_id);

CREATE INDEX IF NOT EXISTS idx_cca_tenant_customer
    ON customer_credit_allocations(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cca_idempotency
    ON customer_credit_allocations(tenant_id, project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- 3. Customer Financial Adjustments
CREATE TABLE IF NOT EXISTS customer_adjustments (
    adjustment_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_financial_account_id VARCHAR(64) NOT NULL,
    adjustment_number VARCHAR(64) NOT NULL,
    adjustment_type VARCHAR(32) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    reason TEXT NOT NULL,
    reference_number VARCHAR(128),
    notes TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_customer_adjustment_number UNIQUE (tenant_id, adjustment_number)
);

CREATE INDEX IF NOT EXISTS idx_cadj_tenant_customer
    ON customer_adjustments(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cadj_tenant_account
    ON customer_adjustments(tenant_id, customer_financial_account_id);

-- 4. Customer Refunds
CREATE TABLE IF NOT EXISTS customer_refunds (
    refund_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_financial_account_id VARCHAR(64) NOT NULL,
    payment_id VARCHAR(64),
    advance_id VARCHAR(64),
    refund_number VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    refund_method VARCHAR(32) NOT NULL DEFAULT 'CASH',
    reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    rejection_reason TEXT,
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    approved_at BIGINT,
    approved_by VARCHAR(64),
    processed_at BIGINT,
    processed_by VARCHAR(64),
    completed_at BIGINT,
    completed_by VARCHAR(64),
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_customer_refund_number UNIQUE (tenant_id, refund_number)
);

CREATE INDEX IF NOT EXISTS idx_cref_tenant_customer
    ON customer_refunds(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cref_tenant_payment
    ON customer_refunds(tenant_id, payment_id);

CREATE INDEX IF NOT EXISTS idx_cref_tenant_advance
    ON customer_refunds(tenant_id, advance_id);

CREATE INDEX IF NOT EXISTS idx_cref_tenant_status
    ON customer_refunds(tenant_id, project_id, status);

-- 5. Customer Credit Audit Events
CREATE TABLE IF NOT EXISTS customer_credit_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_cc_audit_lookup
    ON customer_credit_audit_events(tenant_id, entity_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_cc_audit_customer
    ON customer_credit_audit_events(tenant_id, customer_id, occurred_at DESC);

-- Enable and Force Row Level Security (RLS) on all Module 14 Step 04 tables
ALTER TABLE customer_advances ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_advances FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_advances ON customer_advances
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_credit_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_credit_allocations FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_credit_allocations ON customer_credit_allocations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_adjustments ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_adjustments FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_adjustments ON customer_adjustments
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_refunds ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_refunds FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_refunds ON customer_refunds
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_credit_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_credit_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_credit_audit_events ON customer_credit_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
