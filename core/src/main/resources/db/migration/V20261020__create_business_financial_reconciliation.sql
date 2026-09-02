-- ============================================================================
-- V20261020: Business Financial Reconciliation, Settlement Control & Period-End Closing Foundation
-- Module 15 -> Step 06
-- ============================================================================

-- 1. Business Financial Reconciliation Runs
CREATE TABLE IF NOT EXISTS business_financial_reconciliation_runs (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    run_number VARCHAR(64) NOT NULL,
    run_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at BIGINT NOT NULL,
    completed_at BIGINT,
    created_by VARCHAR(64) NOT NULL,
    reviewed_by VARCHAR(64),
    approved_by VARCHAR(64),
    total_records_checked INT NOT NULL DEFAULT 0,
    matched_records INT NOT NULL DEFAULT 0,
    discrepancy_count INT NOT NULL DEFAULT 0,
    critical_discrepancy_count INT NOT NULL DEFAULT 0,
    warning_count INT NOT NULL DEFAULT 0,
    checksum VARCHAR(128) NOT NULL,
    notes TEXT,
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_biz_recon_run_number UNIQUE (tenant_id, project_id, run_number)
);

CREATE INDEX IF NOT EXISTS idx_biz_recon_runs_tenant_proj ON business_financial_reconciliation_runs(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_biz_recon_runs_period ON business_financial_reconciliation_runs(tenant_id, project_id, period_id);
CREATE INDEX IF NOT EXISTS idx_biz_recon_runs_status ON business_financial_reconciliation_runs(tenant_id, project_id, status);

-- 2. Business Financial Reconciliation Discrepancies
CREATE TABLE IF NOT EXISTS business_financial_reconciliation_discrepancies (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    reconciliation_run_id VARCHAR(64) NOT NULL REFERENCES business_financial_reconciliation_runs(id) ON DELETE CASCADE,
    period_id VARCHAR(64) NOT NULL,
    discrepancy_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    expected_amount DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    actual_amount DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    difference_amount DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    detected_at BIGINT NOT NULL,
    assigned_to VARCHAR(64),
    resolution_note TEXT,
    resolved_by VARCHAR(64),
    resolved_at BIGINT,
    approved_by VARCHAR(64),
    approved_at BIGINT,
    linked_correction_type VARCHAR(64),
    linked_correction_id VARCHAR(64),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_biz_recon_disc_tenant_proj ON business_financial_reconciliation_discrepancies(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_biz_recon_disc_run ON business_financial_reconciliation_discrepancies(reconciliation_run_id);
CREATE INDEX IF NOT EXISTS idx_biz_recon_disc_period ON business_financial_reconciliation_discrepancies(tenant_id, project_id, period_id);
CREATE INDEX IF NOT EXISTS idx_biz_recon_disc_status ON business_financial_reconciliation_discrepancies(tenant_id, project_id, status);
CREATE INDEX IF NOT EXISTS idx_biz_recon_disc_severity ON business_financial_reconciliation_discrepancies(tenant_id, project_id, severity);

-- 3. Business Financial Reconciliation Snapshots
CREATE TABLE IF NOT EXISTS business_financial_reconciliation_snapshots (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    reconciliation_run_id VARCHAR(64) NOT NULL REFERENCES business_financial_reconciliation_runs(id) ON DELETE CASCADE,
    period_id VARCHAR(64) NOT NULL,
    snapshot_data TEXT NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_biz_recon_snap_run ON business_financial_reconciliation_snapshots(reconciliation_run_id);

-- 4. Business Financial Reconciliation Audit Events
CREATE TABLE IF NOT EXISTS business_financial_reconciliation_audit_events (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    reconciliation_run_id VARCHAR(64),
    discrepancy_id VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    correlation_id VARCHAR(128),
    idempotency_key VARCHAR(128),
    reason TEXT,
    before_state TEXT,
    after_state TEXT,
    checksum VARCHAR(128),
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_biz_recon_audit_tenant_proj ON business_financial_reconciliation_audit_events(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_biz_recon_audit_run ON business_financial_reconciliation_audit_events(reconciliation_run_id);
CREATE INDEX IF NOT EXISTS idx_biz_recon_audit_disc ON business_financial_reconciliation_audit_events(discrepancy_id);

-- Row Level Security (RLS) Policies
ALTER TABLE business_financial_reconciliation_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_reconciliation_runs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS biz_recon_runs_tenant_isolation_policy ON business_financial_reconciliation_runs;
CREATE POLICY biz_recon_runs_tenant_isolation_policy ON business_financial_reconciliation_runs
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE business_financial_reconciliation_discrepancies ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_reconciliation_discrepancies FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS biz_recon_disc_tenant_isolation_policy ON business_financial_reconciliation_discrepancies;
CREATE POLICY biz_recon_disc_tenant_isolation_policy ON business_financial_reconciliation_discrepancies
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE business_financial_reconciliation_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_reconciliation_snapshots FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS biz_recon_snap_tenant_isolation_policy ON business_financial_reconciliation_snapshots;
CREATE POLICY biz_recon_snap_tenant_isolation_policy ON business_financial_reconciliation_snapshots
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE business_financial_reconciliation_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_reconciliation_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS biz_recon_audit_tenant_isolation_policy ON business_financial_reconciliation_audit_events;
CREATE POLICY biz_recon_audit_tenant_isolation_policy ON business_financial_reconciliation_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
