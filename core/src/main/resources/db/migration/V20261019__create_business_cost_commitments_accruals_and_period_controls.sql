-- =========================================================================
-- MODULE 15 STEP 05: BUSINESS COST COMMITMENT, ACCRUAL & PERIOD-END CONTROL
-- Canonical PostgreSQL Schema & Row-Level Security (RLS)
-- Migration: V20261019__create_business_cost_commitments_accruals_and_period_controls.sql
-- =========================================================================

-- 1. BUSINESS FINANCIAL PERIODS TABLE
CREATE TABLE IF NOT EXISTS business_financial_periods (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_code VARCHAR(64) NOT NULL,
    period_name VARCHAR(255) NOT NULL,
    start_date BIGINT NOT NULL,
    end_date BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    closed_by VARCHAR(64),
    closed_at BIGINT,
    close_reason TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT uq_biz_period_code UNIQUE (tenant_id, project_id, period_code),
    CONSTRAINT chk_biz_period_code CHECK (char_length(trim(period_code)) >= 2),
    CONSTRAINT chk_biz_period_dates CHECK (end_date >= start_date)
);

-- 2. BUSINESS COST COMMITMENTS TABLE
CREATE TABLE IF NOT EXISTS business_cost_commitments (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    commitment_number VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64),
    job_id VARCHAR(64),
    cost_center_id VARCHAR(64),
    cost_category_id VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    committed_amount NUMERIC(19, 4) NOT NULL,
    consumed_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    remaining_amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    commitment_date BIGINT NOT NULL,
    expected_date BIGINT,
    period_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT uq_biz_commitment_num UNIQUE (tenant_id, project_id, commitment_number),
    CONSTRAINT chk_biz_commitment_amounts CHECK (committed_amount > 0 AND consumed_amount >= 0 AND remaining_amount >= 0 AND consumed_amount <= committed_amount),
    CONSTRAINT chk_biz_commitment_currency CHECK (char_length(currency) = 3)
);

-- 3. BUSINESS COST COMMITMENT CONSUMPTIONS TABLE (Append-Only)
CREATE TABLE IF NOT EXISTS business_cost_commitment_consumptions (
    id VARCHAR(64) PRIMARY KEY,
    commitment_id VARCHAR(64) NOT NULL REFERENCES business_cost_commitments(id) ON DELETE RESTRICT,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    consumed_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128),
    notes TEXT,

    CONSTRAINT chk_biz_consumption_amount CHECK (amount > 0),
    CONSTRAINT chk_biz_consumption_currency CHECK (char_length(currency) = 3)
);

-- 4. BUSINESS COST ACCRUALS TABLE
CREATE TABLE IF NOT EXISTS business_cost_accruals (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    accrual_number VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64),
    job_id VARCHAR(64),
    cost_center_id VARCHAR(64),
    cost_category_id VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    accrual_amount NUMERIC(19, 4) NOT NULL,
    reversed_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    accounting_period_id VARCHAR(64) NOT NULL,
    accrual_date BIGINT NOT NULL,
    source_commitment_id VARCHAR(64),
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    ledger_posting_id VARCHAR(64),
    reversal_posting_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    reviewed_by VARCHAR(64),
    approved_by VARCHAR(64),
    posted_by VARCHAR(64),
    reversed_by VARCHAR(64),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT uq_biz_accrual_num UNIQUE (tenant_id, project_id, accrual_number),
    CONSTRAINT chk_biz_accrual_amounts CHECK (accrual_amount > 0 AND reversed_amount >= 0 AND reversed_amount <= accrual_amount),
    CONSTRAINT chk_biz_accrual_currency CHECK (char_length(currency) = 3)
);

-- 5. BUSINESS COST ACCRUAL REVERSALS TABLE (Append-Only)
CREATE TABLE IF NOT EXISTS business_cost_accrual_reversals (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    accrual_id VARCHAR(64) NOT NULL REFERENCES business_cost_accruals(id) ON DELETE RESTRICT,
    reversal_amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    reversal_date BIGINT NOT NULL,
    accounting_period_id VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    ledger_posting_id VARCHAR(64) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    idempotency_key VARCHAR(128),

    CONSTRAINT chk_biz_reversal_amount CHECK (reversal_amount > 0),
    CONSTRAINT chk_biz_reversal_reason CHECK (char_length(trim(reason)) >= 3)
);

-- 6. BUSINESS COST CONTROL AUDIT EVENTS TABLE (Append-Only)
CREATE TABLE IF NOT EXISTS business_cost_control_audit_events (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    correlation_id VARCHAR(128),
    idempotency_key VARCHAR(128),
    previous_state TEXT,
    new_state TEXT,
    amount NUMERIC(19, 4),
    currency VARCHAR(3),
    reason TEXT,
    metadata TEXT
);

-- =========================================================================
-- INDEXES
-- =========================================================================
CREATE INDEX IF NOT EXISTS idx_biz_period_tenant_proj ON business_financial_periods(tenant_id, project_id, status);
CREATE INDEX IF NOT EXISTS idx_biz_commit_tenant_proj ON business_cost_commitments(tenant_id, project_id, status);
CREATE INDEX IF NOT EXISTS idx_biz_commit_vendor ON business_cost_commitments(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_biz_commit_job ON business_cost_commitments(tenant_id, project_id, job_id);
CREATE INDEX IF NOT EXISTS idx_biz_commit_period ON business_cost_commitments(tenant_id, project_id, period_id);
CREATE INDEX IF NOT EXISTS idx_biz_consump_commit ON business_cost_commitment_consumptions(tenant_id, project_id, commitment_id);
CREATE INDEX IF NOT EXISTS idx_biz_accrual_tenant_proj ON business_cost_accruals(tenant_id, project_id, status);
CREATE INDEX IF NOT EXISTS idx_biz_accrual_period ON business_cost_accruals(tenant_id, project_id, accounting_period_id);
CREATE INDEX IF NOT EXISTS idx_biz_accrual_vendor ON business_cost_accruals(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_biz_accrual_job ON business_cost_accruals(tenant_id, project_id, job_id);
CREATE INDEX IF NOT EXISTS idx_biz_reversal_accrual ON business_cost_accrual_reversals(tenant_id, project_id, accrual_id);
CREATE INDEX IF NOT EXISTS idx_biz_control_audit_entity ON business_cost_control_audit_events(tenant_id, project_id, entity_type, entity_id);

-- =========================================================================
-- ROW-LEVEL SECURITY (RLS)
-- =========================================================================
ALTER TABLE business_financial_periods ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_periods FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS sucharu_rls_policy ON business_financial_periods;
CREATE POLICY sucharu_rls_policy ON business_financial_periods
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        AND project_id = CURRENT_SETTING('app.current_project_id', true)
    );

ALTER TABLE business_cost_commitments ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_commitments FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS sucharu_rls_policy ON business_cost_commitments;
CREATE POLICY sucharu_rls_policy ON business_cost_commitments
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        AND project_id = CURRENT_SETTING('app.current_project_id', true)
    );

ALTER TABLE business_cost_commitment_consumptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_commitment_consumptions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS sucharu_rls_policy ON business_cost_commitment_consumptions;
CREATE POLICY sucharu_rls_policy ON business_cost_commitment_consumptions
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        AND project_id = CURRENT_SETTING('app.current_project_id', true)
    );

ALTER TABLE business_cost_accruals ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_accruals FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS sucharu_rls_policy ON business_cost_accruals;
CREATE POLICY sucharu_rls_policy ON business_cost_accruals
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        AND project_id = CURRENT_SETTING('app.current_project_id', true)
    );

ALTER TABLE business_cost_accrual_reversals ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_accrual_reversals FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS sucharu_rls_policy ON business_cost_accrual_reversals;
CREATE POLICY sucharu_rls_policy ON business_cost_accrual_reversals
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        AND project_id = CURRENT_SETTING('app.current_project_id', true)
    );

ALTER TABLE business_cost_control_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_control_audit_events FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS sucharu_rls_policy ON business_cost_control_audit_events;
CREATE POLICY sucharu_rls_policy ON business_cost_control_audit_events
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        AND project_id = CURRENT_SETTING('app.current_project_id', true)
    );
