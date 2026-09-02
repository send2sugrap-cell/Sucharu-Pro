-- ============================================================================
-- V20261023: Business Financial Final Integrity Control, Audit & Period Closure Foundation
-- Module 15 -> Step 10 (FINAL STEP OF MODULE 15)
-- ============================================================================

-- 1. Business Financial Integrity Control Runs
CREATE TABLE IF NOT EXISTS business_financial_integrity_runs (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    run_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL, -- PASSED, WARNING, FAILED, BLOCKED
    executed_by VARCHAR(64) NOT NULL,
    started_at BIGINT NOT NULL,
    completed_at BIGINT,
    total_assertions_count INT NOT NULL DEFAULT 18,
    passed_assertions_count INT NOT NULL DEFAULT 0,
    warning_assertions_count INT NOT NULL DEFAULT 0,
    failed_assertions_count INT NOT NULL DEFAULT 0,
    blocked_assertions_count INT NOT NULL DEFAULT 0,
    integrity_checksum VARCHAR(128) NOT NULL,
    notes TEXT,
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_biz_integrity_run_number UNIQUE (tenant_id, project_id, run_number)
);

CREATE INDEX IF NOT EXISTS idx_biz_integrity_runs_tenant_proj ON business_financial_integrity_runs(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_biz_integrity_runs_period ON business_financial_integrity_runs(tenant_id, project_id, period_id);
CREATE INDEX IF NOT EXISTS idx_biz_integrity_runs_status ON business_financial_integrity_runs(tenant_id, project_id, status);

-- 2. Business Financial Control Assertions
CREATE TABLE IF NOT EXISTS business_financial_control_assertions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    run_id VARCHAR(64) NOT NULL REFERENCES business_financial_integrity_runs(id) ON DELETE CASCADE,
    period_id VARCHAR(64) NOT NULL,
    assertion_type VARCHAR(64) NOT NULL,
    assertion_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL, -- PASSED, WARNING, FAILED, BLOCKED
    severity VARCHAR(32) NOT NULL, -- INFO, WARNING, CRITICAL
    expected_value VARCHAR(256) NOT NULL,
    actual_value VARCHAR(256) NOT NULL,
    variance_value VARCHAR(256),
    explanation TEXT NOT NULL,
    recommended_action TEXT,
    source_entities_count INT NOT NULL DEFAULT 0,
    evaluated_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_biz_control_assertions_tenant_proj ON business_financial_control_assertions(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_biz_control_assertions_run ON business_financial_control_assertions(run_id);
CREATE INDEX IF NOT EXISTS idx_biz_control_assertions_period ON business_financial_control_assertions(tenant_id, project_id, period_id);
CREATE INDEX IF NOT EXISTS idx_biz_control_assertions_status ON business_financial_control_assertions(tenant_id, project_id, status);

-- 3. Business Financial Period Close Certificates
CREATE TABLE IF NOT EXISTS business_financial_period_close_certificates (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    period_code VARCHAR(64) NOT NULL,
    final_run_id VARCHAR(64) NOT NULL REFERENCES business_financial_integrity_runs(id),
    closed_by VARCHAR(64) NOT NULL,
    closed_at BIGINT NOT NULL,
    approved_by VARCHAR(64) NOT NULL,
    approved_at BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'FINALIZED',
    total_recognized_expenses DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_settled_payables DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_ledger_debit DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_ledger_credit DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    net_recognized_adjustments DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    certificate_checksum VARCHAR(128) NOT NULL,
    snapshot_payload_json TEXT NOT NULL,
    notes TEXT,
    created_at BIGINT NOT NULL,
    CONSTRAINT uq_biz_period_certificate UNIQUE (tenant_id, project_id, period_id)
);

CREATE INDEX IF NOT EXISTS idx_biz_period_cert_tenant_proj ON business_financial_period_close_certificates(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_biz_period_cert_period ON business_financial_period_close_certificates(tenant_id, project_id, period_id);

-- Row Level Security (RLS) Policies
ALTER TABLE business_financial_integrity_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_integrity_runs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS biz_integrity_runs_tenant_isolation_policy ON business_financial_integrity_runs;
CREATE POLICY biz_integrity_runs_tenant_isolation_policy ON business_financial_integrity_runs
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE business_financial_control_assertions ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_control_assertions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS biz_control_assertions_tenant_isolation_policy ON business_financial_control_assertions;
CREATE POLICY biz_control_assertions_tenant_isolation_policy ON business_financial_control_assertions
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE business_financial_period_close_certificates ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_period_close_certificates FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS biz_period_cert_tenant_isolation_policy ON business_financial_period_close_certificates;
CREATE POLICY biz_period_cert_tenant_isolation_policy ON business_financial_period_close_certificates
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
