-- V20261102__create_executive_profitability_command_center_reporting.sql
-- Module 16 Step 10: Executive Profitability Command Center, KPI Cockpit & Management Reporting Engine

-- 1. Executive Profitability Snapshots
CREATE TABLE IF NOT EXISTS executive_profitability_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(32),
    generated_at BIGINT NOT NULL,
    total_gross_revenue NUMERIC(18, 4) NOT NULL,
    total_net_revenue NUMERIC(18, 4) NOT NULL,
    total_actual_cost NUMERIC(18, 4) NOT NULL,
    total_gross_profit NUMERIC(18, 4) NOT NULL,
    gross_margin_percentage NUMERIC(10, 4) NOT NULL,
    total_contribution_amount NUMERIC(18, 4) NOT NULL,
    contribution_margin_percentage NUMERIC(10, 4) NOT NULL,
    forecast_revenue NUMERIC(18, 4),
    forecast_gross_profit NUMERIC(18, 4),
    forecast_gross_margin NUMERIC(10, 4),
    active_alerts_count INT NOT NULL DEFAULT 0,
    critical_alerts_count INT NOT NULL DEFAULT 0,
    pending_actions_count INT NOT NULL DEFAULT 0,
    overall_health VARCHAR(32) NOT NULL,
    overall_score NUMERIC(10, 4) NOT NULL,
    scorecard_json TEXT NOT NULL,
    kpis_json TEXT NOT NULL,
    rankings_json TEXT NOT NULL,
    priorities_json TEXT NOT NULL,
    concentration_json TEXT NOT NULL,
    drivers_json TEXT NOT NULL,
    leakage_json TEXT NOT NULL,
    reconciliation_json TEXT NOT NULL,
    source_fingerprint VARCHAR(64) NOT NULL,
    integrity_hash VARCHAR(64) NOT NULL,
    calculation_version VARCHAR(16) NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_exec_snap_tenant_proj ON executive_profitability_snapshots(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_exec_snap_period ON executive_profitability_snapshots(tenant_id, period_id);
CREATE INDEX IF NOT EXISTS idx_exec_snap_fingerprint ON executive_profitability_snapshots(tenant_id, source_fingerprint);

-- 2. Executive Provenance Records
CREATE TABLE IF NOT EXISTS executive_provenance_records (
    provenance_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL REFERENCES executive_profitability_snapshots(snapshot_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    kpi_or_section_key VARCHAR(64) NOT NULL,
    source_module VARCHAR(32) NOT NULL,
    source_step VARCHAR(32) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    source_snapshot_id VARCHAR(64),
    metric_key VARCHAR(64) NOT NULL,
    metric_value NUMERIC(18, 4) NOT NULL,
    calculation_timestamp BIGINT NOT NULL,
    provenance_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_exec_prov_snap ON executive_provenance_records(tenant_id, snapshot_id);

-- 3. Executive Reconciliation Events
CREATE TABLE IF NOT EXISTS executive_reconciliation_events (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(32),
    snapshot_id VARCHAR(64) NOT NULL REFERENCES executive_profitability_snapshots(snapshot_id) ON DELETE CASCADE,
    checked_at BIGINT NOT NULL,
    is_balanced BOOLEAN NOT NULL,
    revenue_matches BOOLEAN NOT NULL,
    cost_matches BOOLEAN NOT NULL,
    profit_matches BOOLEAN NOT NULL,
    forecast_matches BOOLEAN NOT NULL,
    alert_counts_match BOOLEAN NOT NULL,
    discrepancies_json TEXT NOT NULL DEFAULT '',
    integrity_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_exec_recon_tenant ON executive_reconciliation_events(tenant_id, project_id);

-- ----------------------------------------------------
-- TENANT ISOLATION & ROW LEVEL SECURITY (RLS)
-- ----------------------------------------------------

ALTER TABLE executive_profitability_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE executive_profitability_snapshots FORCE ROW LEVEL SECURITY;

CREATE POLICY executive_profitability_snapshots_tenant_isolation ON executive_profitability_snapshots
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE executive_provenance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE executive_provenance_records FORCE ROW LEVEL SECURITY;

CREATE POLICY executive_provenance_records_tenant_isolation ON executive_provenance_records
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE executive_reconciliation_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE executive_reconciliation_events FORCE ROW LEVEL SECURITY;

CREATE POLICY executive_reconciliation_events_tenant_isolation ON executive_reconciliation_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
