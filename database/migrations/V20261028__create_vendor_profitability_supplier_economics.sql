-- V20261028__create_vendor_profitability_supplier_economics.sql
-- Module 16 Step 05: Vendor-Wise Profitability, Cost Contribution & Supplier Economics Engine

CREATE TABLE IF NOT EXISTS vendor_profitability_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    vendor_code VARCHAR(64),
    service_category VARCHAR(64),
    vendor_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    period_id VARCHAR(64),
    period_start BIGINT,
    period_end BIGINT,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    generated_at BIGINT NOT NULL,

    total_vendor_cost NUMERIC(18, 4) NOT NULL,
    direct_vendor_cost NUMERIC(18, 4) NOT NULL,
    paid_vendor_cost NUMERIC(18, 4) NOT NULL,
    outstanding_exposure NUMERIC(18, 4) NOT NULL,
    unbilled_estimate_cost NUMERIC(18, 4) NOT NULL DEFAULT 0,
    rework_cost NUMERIC(18, 4) NOT NULL DEFAULT 0,
    baseline_cost NUMERIC(18, 4),
    cost_variance NUMERIC(18, 4),
    cost_variance_percentage NUMERIC(18, 4),

    attributed_revenue_context NUMERIC(18, 4) NOT NULL DEFAULT 0,
    attributed_total_job_cost NUMERIC(18, 4) NOT NULL DEFAULT 0,
    fulfillment_profitability_impact NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cost_to_revenue_context_percentage NUMERIC(18, 4),
    vendor_cost_share_percentage NUMERIC(18, 4),

    attributed_work_order_count INT NOT NULL DEFAULT 0,
    attributed_job_count INT NOT NULL DEFAULT 0,
    attributed_product_count INT NOT NULL DEFAULT 0,
    attributed_customer_count INT NOT NULL DEFAULT 0,
    total_attributed_quantity BIGINT NOT NULL DEFAULT 0,
    cost_per_job NUMERIC(18, 4),
    cost_per_unit NUMERIC(18, 4),

    quality_failure_count INT NOT NULL DEFAULT 0,
    rework_count INT NOT NULL DEFAULT 0,
    rejection_count INT NOT NULL DEFAULT 0,
    dispute_count INT NOT NULL DEFAULT 0,
    quality_failure_rate NUMERIC(18, 4),
    rework_rate NUMERIC(18, 4),

    efficiency_score NUMERIC(18, 4) NOT NULL DEFAULT 0,
    risk_classification VARCHAR(32) NOT NULL,
    dependency_classification VARCHAR(32) NOT NULL,
    dependency_share_percentage NUMERIC(18, 4),
    trend_direction VARCHAR(32) NOT NULL,
    data_readiness VARCHAR(32) NOT NULL DEFAULT 'READY',

    integrity_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_profitability_components (
    component_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL REFERENCES vendor_profitability_snapshots(snapshot_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    percentage_of_total_cost NUMERIC(18, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_profitability_cost_attributions (
    cost_attribution_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    work_order_id VARCHAR(64),
    job_id VARCHAR(64),
    product_id VARCHAR(64),
    customer_id VARCHAR(64),
    component_type VARCHAR(64) NOT NULL,
    attributed_amount NUMERIC(18, 4) NOT NULL,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    source_module VARCHAR(64) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    source_transaction_id VARCHAR(64),
    attribution_method VARCHAR(64) NOT NULL,
    provenance_fingerprint VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_profitability_revenue_context (
    revenue_context_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    product_id VARCHAR(64),
    customer_id VARCHAR(64),
    recognized_revenue_context NUMERIC(18, 4) NOT NULL,
    source_module VARCHAR(64) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_profitability_reconciliation_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64),
    is_balanced BOOLEAN NOT NULL,
    total_cost_difference NUMERIC(18, 4) NOT NULL,
    component_difference NUMERIC(18, 4) NOT NULL,
    provenance_difference NUMERIC(18, 4) NOT NULL,
    job_difference NUMERIC(18, 4) NOT NULL,
    product_difference NUMERIC(18, 4) NOT NULL,
    customer_difference NUMERIC(18, 4) NOT NULL,
    paid_vs_liability_valid BOOLEAN NOT NULL,
    error_details TEXT,
    timestamp BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_profitability_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    details TEXT,
    integrity_hash VARCHAR(128),
    timestamp BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_profitability_unattributed_items (
    unattributed_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    source_module VARCHAR(64) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_vps_tenant_vendor ON vendor_profitability_snapshots(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vps_period ON vendor_profitability_snapshots(period_id);
CREATE INDEX IF NOT EXISTS idx_vps_risk ON vendor_profitability_snapshots(risk_classification);
CREATE INDEX IF NOT EXISTS idx_vps_dependency ON vendor_profitability_snapshots(dependency_classification);

CREATE INDEX IF NOT EXISTS idx_vpc_snapshot ON vendor_profitability_components(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_vpca_tenant_vendor ON vendor_profitability_cost_attributions(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vprc_tenant_vendor ON vendor_profitability_revenue_context(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpre_tenant_vendor ON vendor_profitability_reconciliation_events(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpae_tenant_vendor ON vendor_profitability_audit_events(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpui_tenant_vendor ON vendor_profitability_unattributed_items(tenant_id, vendor_id);

-- Enable & Force Row Level Security
ALTER TABLE vendor_profitability_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_profitability_snapshots FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_profitability_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_profitability_components FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_profitability_cost_attributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_profitability_cost_attributions FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_profitability_revenue_context ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_profitability_revenue_context FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_profitability_reconciliation_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_profitability_reconciliation_events FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_profitability_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_profitability_audit_events FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_profitability_unattributed_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_profitability_unattributed_items FORCE ROW LEVEL SECURITY;

-- Tenant Isolation Policies
DROP POLICY IF EXISTS tenant_isolation_policy ON vendor_profitability_snapshots;
CREATE POLICY tenant_isolation_policy ON vendor_profitability_snapshots
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_policy ON vendor_profitability_components;
CREATE POLICY tenant_isolation_policy ON vendor_profitability_components
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_policy ON vendor_profitability_cost_attributions;
CREATE POLICY tenant_isolation_policy ON vendor_profitability_cost_attributions
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_policy ON vendor_profitability_revenue_context;
CREATE POLICY tenant_isolation_policy ON vendor_profitability_revenue_context
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_policy ON vendor_profitability_reconciliation_events;
CREATE POLICY tenant_isolation_policy ON vendor_profitability_reconciliation_events
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_policy ON vendor_profitability_audit_events;
CREATE POLICY tenant_isolation_policy ON vendor_profitability_audit_events
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_policy ON vendor_profitability_unattributed_items;
CREATE POLICY tenant_isolation_policy ON vendor_profitability_unattributed_items
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
