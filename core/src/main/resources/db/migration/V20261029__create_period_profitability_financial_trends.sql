-- ============================================================================
-- SUCHARU PRO ERP — MODULE 16 STEP 06
-- PERIOD-WISE PROFITABILITY, BUSINESS PERFORMANCE & FINANCIAL TRENDS ENGINE
-- Migration: V20261029__create_period_profitability_financial_trends.sql
-- ============================================================================

-- 1. Period Profitability Snapshots
CREATE TABLE IF NOT EXISTS period_profitability_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    period_type VARCHAR(32) NOT NULL,
    period_start BIGINT NOT NULL,
    period_end BIGINT NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Dhaka',
    period_key VARCHAR(128) NOT NULL,
    fiscal_period_id VARCHAR(64),
    period_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    calculation_version VARCHAR(64) NOT NULL DEFAULT 'MODULE16_PERIOD_PROFITABILITY_V1',
    snapshot_version INT NOT NULL DEFAULT 1,
    supersedes_snapshot_id VARCHAR(64),
    superseded_by_snapshot_id VARCHAR(64),
    generated_at BIGINT NOT NULL,
    generated_by VARCHAR(64) NOT NULL,
    source_as_of BIGINT NOT NULL,
    revenue NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_actual_cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    gross_profit NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    gross_margin_percentage NUMERIC(19, 4),
    cost_to_revenue_percentage NUMERIC(19, 4),
    direct_cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    indirect_cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    contribution_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin_percentage NUMERIC(19, 4),
    baseline_revenue NUMERIC(19, 4),
    baseline_cost NUMERIC(19, 4),
    revenue_variance NUMERIC(19, 4),
    revenue_variance_percentage NUMERIC(19, 4),
    cost_variance NUMERIC(19, 4),
    cost_variance_percentage NUMERIC(19, 4),
    profit_variance NUMERIC(19, 4),
    profit_variance_percentage NUMERIC(19, 4),
    job_count INT NOT NULL DEFAULT 0,
    completed_job_count INT NOT NULL DEFAULT 0,
    product_count INT NOT NULL DEFAULT 0,
    customer_count INT NOT NULL DEFAULT 0,
    vendor_count INT NOT NULL DEFAULT 0,
    total_units BIGINT NOT NULL DEFAULT 0,
    average_revenue_per_job NUMERIC(19, 4),
    average_profit_per_job NUMERIC(19, 4),
    average_revenue_per_unit NUMERIC(19, 4),
    average_cost_per_unit NUMERIC(19, 4),
    average_profit_per_unit NUMERIC(19, 4),
    profitability_classification VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    trend_direction VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    source_readiness VARCHAR(32) NOT NULL DEFAULT 'READY',
    provenance_fingerprints TEXT[] NOT NULL DEFAULT '{}',
    integrity_hash VARCHAR(128) NOT NULL,
    is_certified BOOLEAN NOT NULL DEFAULT FALSE,
    certified_at BIGINT,
    certificate_id VARCHAR(64),
    warnings TEXT[] NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,
    CONSTRAINT chk_period_profitability_boundaries CHECK (period_start < period_end)
);

CREATE INDEX IF NOT EXISTS idx_period_profit_snapshots_tenant_period ON period_profitability_snapshots(tenant_id, period_id);
CREATE INDEX IF NOT EXISTS idx_period_profit_snapshots_type_start ON period_profitability_snapshots(tenant_id, period_type, period_start);
CREATE INDEX IF NOT EXISTS idx_period_profit_snapshots_integrity ON period_profitability_snapshots(integrity_hash);

-- 2. Period Cost Components
CREATE TABLE IF NOT EXISTS period_profitability_cost_components (
    component_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    percentage_of_total_cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    percentage_of_revenue NUMERIC(19, 4),
    source_attribution_count INT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,
    FOREIGN KEY (snapshot_id) REFERENCES period_profitability_snapshots(snapshot_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_period_cost_comp_snapshot ON period_profitability_cost_components(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_period_cost_comp_tenant_period ON period_profitability_cost_components(tenant_id, period_id);

-- 3. Period Revenue Attributions
CREATE TABLE IF NOT EXISTS period_profitability_revenue_attributions (
    attribution_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    attribution_dimension VARCHAR(32) NOT NULL,
    dimension_id VARCHAR(64) NOT NULL,
    dimension_name VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    percentage_of_total_revenue NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    source_module VARCHAR(32) NOT NULL DEFAULT 'MODULE_14',
    source_entity_type VARCHAR(64) NOT NULL DEFAULT 'INVOICE',
    source_entity_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,
    FOREIGN KEY (snapshot_id) REFERENCES period_profitability_snapshots(snapshot_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_period_rev_attr_snapshot ON period_profitability_revenue_attributions(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_period_rev_attr_tenant_period ON period_profitability_revenue_attributions(tenant_id, period_id);

-- 4. Period Provenance Records
CREATE TABLE IF NOT EXISTS period_profitability_provenances (
    provenance_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    source_module VARCHAR(32) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    source_transaction_id VARCHAR(64),
    source_snapshot_id VARCHAR(64),
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    component_type VARCHAR(64),
    attribution_dimension VARCHAR(32) NOT NULL,
    fingerprint VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000
);

CREATE INDEX IF NOT EXISTS idx_period_provenance_tenant_period ON period_profitability_provenances(tenant_id, period_id);
CREATE INDEX IF NOT EXISTS idx_period_provenance_fingerprint ON period_profitability_provenances(fingerprint);

-- 5. Period Reconciliations
CREATE TABLE IF NOT EXISTS period_profitability_reconciliations (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64),
    is_balanced BOOLEAN NOT NULL,
    revenue_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    cost_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    profit_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    margin_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    contribution_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    child_aggregation_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    cross_dimensional_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    assertions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    error_details TEXT[] NOT NULL DEFAULT '{}',
    timestamp BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000
);

CREATE INDEX IF NOT EXISTS idx_period_recon_tenant_period ON period_profitability_reconciliations(tenant_id, period_id);

-- 6. Period Audit Events
CREATE TABLE IF NOT EXISTS period_profitability_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    snapshot_id VARCHAR(64),
    calculation_version VARCHAR(64) NOT NULL DEFAULT 'MODULE16_PERIOD_PROFITABILITY_V1',
    previous_state TEXT,
    resulting_state TEXT,
    details TEXT,
    integrity_hash VARCHAR(128),
    timestamp BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000
);

CREATE INDEX IF NOT EXISTS idx_period_audit_tenant_period ON period_profitability_audit_events(tenant_id, period_id);

-- 7. Period Idempotency Records
CREATE TABLE IF NOT EXISTS period_profitability_idempotency_records (
    idempotency_key VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,
    PRIMARY KEY (tenant_id, idempotency_key)
);

-- 8. Period Unattributed Items
CREATE TABLE IF NOT EXISTS period_profitability_unattributed_items (
    unattributed_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    source_module VARCHAR(32) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000
);

CREATE INDEX IF NOT EXISTS idx_period_unattributed_tenant_period ON period_profitability_unattributed_items(tenant_id, period_id);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

ALTER TABLE period_profitability_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_profitability_snapshots FORCE ROW LEVEL SECURITY;
CREATE POLICY period_profit_snapshots_tenant_isolation ON period_profitability_snapshots
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE period_profitability_cost_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_profitability_cost_components FORCE ROW LEVEL SECURITY;
CREATE POLICY period_cost_comp_tenant_isolation ON period_profitability_cost_components
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE period_profitability_revenue_attributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_profitability_revenue_attributions FORCE ROW LEVEL SECURITY;
CREATE POLICY period_rev_attr_tenant_isolation ON period_profitability_revenue_attributions
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE period_profitability_provenances ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_profitability_provenances FORCE ROW LEVEL SECURITY;
CREATE POLICY period_provenance_tenant_isolation ON period_profitability_provenances
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE period_profitability_reconciliations ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_profitability_reconciliations FORCE ROW LEVEL SECURITY;
CREATE POLICY period_recon_tenant_isolation ON period_profitability_reconciliations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE period_profitability_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_profitability_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY period_audit_tenant_isolation ON period_profitability_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE period_profitability_idempotency_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_profitability_idempotency_records FORCE ROW LEVEL SECURITY;
CREATE POLICY period_idempotency_tenant_isolation ON period_profitability_idempotency_records
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE period_profitability_unattributed_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_profitability_unattributed_items FORCE ROW LEVEL SECURITY;
CREATE POLICY period_unattributed_tenant_isolation ON period_profitability_unattributed_items
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
