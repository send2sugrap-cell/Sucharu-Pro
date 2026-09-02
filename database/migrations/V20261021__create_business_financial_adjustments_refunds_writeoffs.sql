-- =========================================================================
-- MODULE 15 STEP 07: BUSINESS FINANCIAL ADJUSTMENT, REFUND, WRITE-OFF & CORRECTION CONTROL
-- V20261021__create_business_financial_adjustments_refunds_writeoffs.sql
-- =========================================================================

-- 1. Business Financial Adjustments
CREATE TABLE IF NOT EXISTS business_financial_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    adjustment_number VARCHAR(64) NOT NULL,
    adjustment_type VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    original_transaction_id VARCHAR(64),
    original_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    adjustment_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    effective_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    reason VARCHAR(500) NOT NULL,
    justification TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    period_id VARCHAR(64) NOT NULL,
    cost_center_id VARCHAR(64),
    job_id VARCHAR(64),
    customer_id VARCHAR(64),
    vendor_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    reviewed_by VARCHAR(64),
    approved_by VARCHAR(64),
    posted_by VARCHAR(64),
    cancelled_by VARCHAR(64),
    rejected_by VARCHAR(64),
    reversal_requested_by VARCHAR(64),
    reversal_approved_by VARCHAR(64),
    reviewed_at BIGINT,
    approved_at BIGINT,
    posted_at BIGINT,
    reversal_requested_at BIGINT,
    reversal_approved_at BIGINT,
    reversed_at BIGINT,
    ledger_posting_id VARCHAR(64),
    reversing_posting_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_bfa_tenant_proj_num UNIQUE (tenant_id, project_id, adjustment_number),
    CONSTRAINT chk_bfa_amounts CHECK (original_amount >= 0 AND effective_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bfa_tenant_proj ON business_financial_adjustments(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bfa_source ON business_financial_adjustments(tenant_id, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_bfa_status ON business_financial_adjustments(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_bfa_period ON business_financial_adjustments(tenant_id, period_id);
CREATE INDEX IF NOT EXISTS idx_bfa_idempotency ON business_financial_adjustments(tenant_id, idempotency_key);

ALTER TABLE business_financial_adjustments ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_adjustments FORCE ROW LEVEL SECURITY;

CREATE POLICY bfa_tenant_isolation ON business_financial_adjustments
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true));

-- 2. Business Financial Refunds
CREATE TABLE IF NOT EXISTS business_financial_refunds (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    refund_number VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64),
    vendor_id VARCHAR(64),
    original_transaction_id VARCHAR(64),
    eligible_balance DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    requested_amount DECIMAL(18, 4) NOT NULL,
    approved_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    refund_reason VARCHAR(500) NOT NULL,
    payment_method VARCHAR(64) NOT NULL DEFAULT 'BANK_TRANSFER',
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    period_id VARCHAR(64) NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64),
    posted_by VARCHAR(64),
    approved_at BIGINT,
    posted_at BIGINT,
    settled_at BIGINT,
    ledger_posting_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_bfr_tenant_proj_num UNIQUE (tenant_id, project_id, refund_number),
    CONSTRAINT chk_bfr_amounts CHECK (requested_amount > 0 AND approved_amount >= 0 AND eligible_balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bfr_tenant_proj ON business_financial_refunds(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bfr_source ON business_financial_refunds(tenant_id, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_bfr_status ON business_financial_refunds(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_bfr_customer ON business_financial_refunds(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_bfr_idempotency ON business_financial_refunds(tenant_id, idempotency_key);

ALTER TABLE business_financial_refunds ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_refunds FORCE ROW LEVEL SECURITY;

CREATE POLICY bfr_tenant_isolation ON business_financial_refunds
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true));

-- 3. Business Financial Write-Offs
CREATE TABLE IF NOT EXISTS business_financial_write_offs (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    write_off_number VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    write_off_type VARCHAR(64) NOT NULL,
    eligible_balance DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    amount DECIMAL(18, 4) NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    reason VARCHAR(500) NOT NULL,
    justification TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    period_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64),
    vendor_id VARCHAR(64),
    requested_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64),
    posted_by VARCHAR(64),
    approved_at BIGINT,
    posted_at BIGINT,
    ledger_posting_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_bfw_tenant_proj_num UNIQUE (tenant_id, project_id, write_off_number),
    CONSTRAINT chk_bfw_amount CHECK (amount > 0 AND eligible_balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bfw_tenant_proj ON business_financial_write_offs(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bfw_source ON business_financial_write_offs(tenant_id, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_bfw_status ON business_financial_write_offs(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_bfw_type ON business_financial_write_offs(tenant_id, write_off_type);
CREATE INDEX IF NOT EXISTS idx_bfw_idempotency ON business_financial_write_offs(tenant_id, idempotency_key);

ALTER TABLE business_financial_write_offs ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_write_offs FORCE ROW LEVEL SECURITY;

CREATE POLICY bfw_tenant_isolation ON business_financial_write_offs
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true));

-- 4. Business Financial Adjustment Postings (Compensating Entries)
CREATE TABLE IF NOT EXISTS business_financial_adjustment_postings (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    adjustment_id VARCHAR(64) NOT NULL,
    posting_number VARCHAR(64) NOT NULL,
    ledger_posting_id VARCHAR(64) NOT NULL,
    posting_type VARCHAR(64) NOT NULL,
    debit_account VARCHAR(64) NOT NULL,
    credit_account VARCHAR(64) NOT NULL,
    amount DECIMAL(18, 4) NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    status VARCHAR(32) NOT NULL DEFAULT 'POSTED',
    posted_by VARCHAR(64) NOT NULL,
    posted_at BIGINT NOT NULL,
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    CONSTRAINT uq_bfap_tenant_proj_num UNIQUE (tenant_id, project_id, posting_number),
    CONSTRAINT chk_bfap_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_bfap_tenant_proj ON business_financial_adjustment_postings(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bfap_adjustment ON business_financial_adjustment_postings(tenant_id, adjustment_id);
CREATE INDEX IF NOT EXISTS idx_bfap_ledger ON business_financial_adjustment_postings(tenant_id, ledger_posting_id);

ALTER TABLE business_financial_adjustment_postings ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_adjustment_postings FORCE ROW LEVEL SECURITY;

CREATE POLICY bfap_tenant_isolation ON business_financial_adjustment_postings
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true));

-- 5. Business Financial Adjustment Audit Events (Append-Only)
CREATE TABLE IF NOT EXISTS business_financial_adjustment_audit_events (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    reason VARCHAR(500),
    metadata_json TEXT,
    correlation_id VARCHAR(128),
    idempotency_key VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_bfae_tenant_proj ON business_financial_adjustment_audit_events(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bfae_entity ON business_financial_adjustment_audit_events(tenant_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_bfae_event_type ON business_financial_adjustment_audit_events(tenant_id, event_type);
CREATE INDEX IF NOT EXISTS idx_bfae_actor ON business_financial_adjustment_audit_events(tenant_id, actor_id);
CREATE INDEX IF NOT EXISTS idx_bfae_timestamp ON business_financial_adjustment_audit_events(tenant_id, timestamp);

ALTER TABLE business_financial_adjustment_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_adjustment_audit_events FORCE ROW LEVEL SECURITY;

CREATE POLICY bfae_tenant_isolation ON business_financial_adjustment_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true));
