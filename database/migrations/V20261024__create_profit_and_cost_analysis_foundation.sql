-- =========================================================================
-- MODULE 16 STEP 01: PROFIT & COST ANALYSIS FOUNDATION & CANONICAL FINANCIAL HANDOFF
-- V20261024__create_profit_and_cost_analysis_foundation.sql
-- =========================================================================

-- 1. Profitability Analysis Snapshots (Analytical projection layer)
CREATE TABLE IF NOT EXISTS profitability_analysis_snapshots (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    target_entity_id VARCHAR(64),
    period_id VARCHAR(64),
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    revenue DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    direct_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    indirect_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    total_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    gross_profit DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    gross_margin_percentage DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
    baseline_cost DECIMAL(18, 4),
    cost_variance DECIMAL(18, 4),
    revenue_variance DECIMAL(18, 4),
    margin_variance DECIMAL(10, 4),
    calculation_version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    source_integrity_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED',
    financial_handoff_verified BOOLEAN NOT NULL DEFAULT TRUE,
    handoff_checksum VARCHAR(128),
    integrity_notes TEXT,
    cost_breakdown_json TEXT,
    generated_by VARCHAR(64) NOT NULL,
    generated_at BIGINT NOT NULL,
    CONSTRAINT chk_pas_direct_cost CHECK (direct_cost >= 0),
    CONSTRAINT chk_pas_indirect_cost CHECK (indirect_cost >= 0),
    CONSTRAINT chk_pas_total_cost CHECK (total_cost >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pas_tenant_proj ON profitability_analysis_snapshots(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_pas_scope_target ON profitability_analysis_snapshots(tenant_id, project_id, scope, target_entity_id);
CREATE INDEX IF NOT EXISTS idx_pas_period ON profitability_analysis_snapshots(tenant_id, project_id, period_id);
CREATE INDEX IF NOT EXISTS idx_pas_generated_at ON profitability_analysis_snapshots(tenant_id, project_id, generated_at);

ALTER TABLE profitability_analysis_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_analysis_snapshots FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS pas_tenant_isolation_policy ON profitability_analysis_snapshots;
CREATE POLICY pas_tenant_isolation_policy ON profitability_analysis_snapshots
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 2. Profitability Cost Attributions (Provenance of attributed costs)
CREATE TABLE IF NOT EXISTS profitability_cost_attributions (
    id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL REFERENCES profitability_analysis_snapshots(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    order_id VARCHAR(64),
    product_id VARCHAR(64),
    customer_id VARCHAR(64),
    vendor_id VARCHAR(64),
    period_id VARCHAR(64),
    attribution_basis VARCHAR(64) NOT NULL DEFAULT 'DIRECT' ,
    source_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    attributable_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    recorded_at BIGINT NOT NULL,
    CONSTRAINT chk_pca_amounts CHECK (source_amount >= 0 AND attributable_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pca_snapshot ON profitability_cost_attributions(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_pca_tenant_proj ON profitability_cost_attributions(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_pca_source ON profitability_cost_attributions(tenant_id, source_type, source_id);

ALTER TABLE profitability_cost_attributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_cost_attributions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS pca_tenant_isolation_policy ON profitability_cost_attributions;
CREATE POLICY pca_tenant_isolation_policy ON profitability_cost_attributions
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 3. Profitability Revenue Provenances (Provenance of canonical revenues)
CREATE TABLE IF NOT EXISTS profitability_revenue_provenances (
    id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL REFERENCES profitability_analysis_snapshots(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    canonical_source_type VARCHAR(64) NOT NULL,
    canonical_source_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64),
    order_id VARCHAR(64),
    job_id VARCHAR(64),
    period_id VARCHAR(64),
    recognized_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    recognition_state VARCHAR(32) NOT NULL DEFAULT 'RECOGNIZED',
    source_timestamp BIGINT NOT NULL,
    CONSTRAINT chk_prp_amount CHECK (recognized_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_prp_snapshot ON profitability_revenue_provenances(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_prp_tenant_proj ON profitability_revenue_provenances(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_prp_source ON profitability_revenue_provenances(tenant_id, canonical_source_type, canonical_source_id);

ALTER TABLE profitability_revenue_provenances ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_revenue_provenances FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS prp_tenant_isolation_policy ON profitability_revenue_provenances;
CREATE POLICY prp_tenant_isolation_policy ON profitability_revenue_provenances
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 4. Profitability Reconciliation Events
CREATE TABLE IF NOT EXISTS profitability_reconciliation_events (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    target_entity_id VARCHAR(64),
    period_id VARCHAR(64),
    is_reconciled BOOLEAN NOT NULL,
    canonical_revenue_total DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    snapshot_revenue_total DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    revenue_difference DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    canonical_cost_total DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    snapshot_cost_total DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    cost_difference DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    discrepancies TEXT,
    checked_by VARCHAR(64) NOT NULL,
    checked_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pre_tenant_proj ON profitability_reconciliation_events(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_pre_snapshot ON profitability_reconciliation_events(tenant_id, snapshot_id);

ALTER TABLE profitability_reconciliation_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_reconciliation_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS pre_tenant_isolation_policy ON profitability_reconciliation_events;
CREATE POLICY pre_tenant_isolation_policy ON profitability_reconciliation_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 5. Profitability Audit Events (Immutable analytical audit trail)
CREATE TABLE IF NOT EXISTS profitability_audit_events (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    scope VARCHAR(32),
    target_entity_id VARCHAR(64),
    outcome VARCHAR(32) NOT NULL,
    details TEXT,
    actor VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    correlation_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_pae_tenant_proj ON profitability_audit_events(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_pae_snapshot ON profitability_audit_events(tenant_id, snapshot_id);
CREATE INDEX IF NOT EXISTS idx_pae_timestamp ON profitability_audit_events(tenant_id, timestamp);

ALTER TABLE profitability_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS pae_tenant_isolation_policy ON profitability_audit_events;
CREATE POLICY pae_tenant_isolation_policy ON profitability_audit_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
