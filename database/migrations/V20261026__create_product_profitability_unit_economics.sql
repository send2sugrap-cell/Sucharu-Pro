-- ============================================================================
-- SUCHARU PRO ERP - MODULE 16 STEP 03: PRODUCT-WISE PROFITABILITY & UNIT ECONOMICS ENGINE
-- Flyway Migration: V20261026__create_product_profitability_unit_economics.sql
-- ============================================================================

-- 1. Product Profitability Snapshots Table
CREATE TABLE IF NOT EXISTS product_profitability_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    sku VARCHAR(64),
    product_name VARCHAR(128),
    edition_id VARCHAR(64),
    version_id VARCHAR(64),
    period_id VARCHAR(64),
    customerId VARCHAR(64),
    total_quantity INT NOT NULL DEFAULT 0,
    recognized_revenue DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    total_actual_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    gross_profit DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    gross_margin_percentage DECIMAL(18, 4),
    unit_revenue DECIMAL(18, 4),
    unit_actual_cost DECIMAL(18, 4),
    unit_gross_profit DECIMAL(18, 4),
    unit_metric_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    profitability_classification VARCHAR(32) NOT NULL DEFAULT 'BREAK_EVEN',
    variance_classification VARCHAR(32) NOT NULL DEFAULT 'BASELINE_UNAVAILABLE',
    baseline_cost DECIMAL(18, 4),
    cost_variance DECIMAL(18, 4),
    cost_variance_percentage DECIMAL(18, 4),
    source_integrity_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED',
    is_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    reconciliation_discrepancy DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    calculation_version VARCHAR(32) NOT NULL DEFAULT 'PRODUCT_PROFITABILITY_V1',
    generated_at BIGINT NOT NULL,
    generated_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    integrity_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prod_prof_snap_tenant_proj ON product_profitability_snapshots(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_prod_prof_snap_product ON product_profitability_snapshots(tenant_id, project_id, product_id);
CREATE INDEX IF NOT EXISTS idx_prod_prof_snap_time ON product_profitability_snapshots(generated_at DESC);

ALTER TABLE product_profitability_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_profitability_snapshots FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS product_profitability_snapshots_tenant_isolation ON product_profitability_snapshots;
CREATE POLICY product_profitability_snapshots_tenant_isolation ON product_profitability_snapshots
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 2. Product Profitability Cost Breakdown Components Table
CREATE TABLE IF NOT EXISTS product_profitability_components (
    component_entry_id BIGSERIAL PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL REFERENCES product_profitability_snapshots(snapshot_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    unit_amount DECIMAL(18, 4),
    percentage_of_total_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    source_count INT NOT NULL DEFAULT 0,
    allocation_basis VARCHAR(64) NOT NULL DEFAULT 'DIRECT',
    provenance_fingerprints TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_profitability_comp UNIQUE (snapshot_id, component_type)
);

CREATE INDEX IF NOT EXISTS idx_prod_prof_comp_snap ON product_profitability_components(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_prod_prof_comp_product ON product_profitability_components(tenant_id, project_id, product_id);

ALTER TABLE product_profitability_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_profitability_components FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS product_profitability_components_tenant_isolation ON product_profitability_components;
CREATE POLICY product_profitability_components_tenant_isolation ON product_profitability_components
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 3. Product Profitability Revenue Attributions Table
CREATE TABLE IF NOT EXISTS product_profitability_revenue_attributions (
    revenue_attribution_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    sku VARCHAR(64),
    edition_id VARCHAR(64),
    version_id VARCHAR(64),
    invoice_id VARCHAR(64),
    order_id VARCHAR(64),
    customer_id VARCHAR(64),
    quantity INT NOT NULL DEFAULT 0,
    recognized_revenue DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    attribution_ratio DECIMAL(18, 4) NOT NULL DEFAULT 1.0000,
    source_module VARCHAR(64) NOT NULL DEFAULT 'MODULE_14',
    source_entity_type VARCHAR(64) NOT NULL DEFAULT 'CUSTOMER_INVOICE_LINE',
    source_entity_id VARCHAR(64) NOT NULL,
    source_transaction_id VARCHAR(64),
    attribution_method VARCHAR(64) NOT NULL DEFAULT 'CANONICAL_INVOICE',
    provenance_fingerprint VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prod_prof_rev_tenant_prod ON product_profitability_revenue_attributions(tenant_id, project_id, product_id);
CREATE INDEX IF NOT EXISTS idx_prod_prof_rev_fingerprint ON product_profitability_revenue_attributions(provenance_fingerprint);

ALTER TABLE product_profitability_revenue_attributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_profitability_revenue_attributions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS product_profitability_revenue_tenant_isolation ON product_profitability_revenue_attributions;
CREATE POLICY product_profitability_revenue_tenant_isolation ON product_profitability_revenue_attributions
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 4. Product Profitability Cost Attributions Table
CREATE TABLE IF NOT EXISTS product_profitability_cost_attributions (
    cost_attribution_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    sku VARCHAR(64),
    edition_id VARCHAR(64),
    version_id VARCHAR(64),
    job_id VARCHAR(64),
    component_type VARCHAR(64) NOT NULL,
    directness VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
    attributed_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    allocation_basis VARCHAR(64) NOT NULL DEFAULT 'DIRECT',
    numerator DECIMAL(18, 4),
    denominator DECIMAL(18, 4),
    allocation_ratio DECIMAL(18, 4),
    source_module VARCHAR(64) NOT NULL DEFAULT 'MODULE_16_STEP_02',
    source_entity_type VARCHAR(64) NOT NULL DEFAULT 'JOB_COST_SNAPSHOT',
    source_entity_id VARCHAR(64) NOT NULL,
    source_transaction_id VARCHAR(64),
    provenance_fingerprint VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prod_prof_cost_tenant_prod ON product_profitability_cost_attributions(tenant_id, project_id, product_id);
CREATE INDEX IF NOT EXISTS idx_prod_prof_cost_fingerprint ON product_profitability_cost_attributions(provenance_fingerprint);

ALTER TABLE product_profitability_cost_attributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_profitability_cost_attributions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS product_profitability_cost_tenant_isolation ON product_profitability_cost_attributions;
CREATE POLICY product_profitability_cost_tenant_isolation ON product_profitability_cost_attributions
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 5. Product Profitability Reconciliation Events Table
CREATE TABLE IF NOT EXISTS product_profitability_reconciliation_events (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    is_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    revenue_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    cost_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    unit_economics_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    expected_revenue DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    actual_revenue DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    expected_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    actual_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    gross_profit_discrepancy DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    discrepancies_json TEXT DEFAULT '',
    checked_at BIGINT NOT NULL,
    checked_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prod_prof_recon_tenant_prod ON product_profitability_reconciliation_events(tenant_id, project_id, product_id);

ALTER TABLE product_profitability_reconciliation_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_profitability_reconciliation_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS product_profitability_recon_tenant_isolation ON product_profitability_reconciliation_events;
CREATE POLICY product_profitability_recon_tenant_isolation ON product_profitability_reconciliation_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 6. Product Profitability Audit Events Table
CREATE TABLE IF NOT EXISTS product_profitability_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL DEFAULT 'STAFF',
    outcome VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    details TEXT,
    correlation_id VARCHAR(64),
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prod_prof_audit_tenant_prod ON product_profitability_audit_events(tenant_id, project_id, product_id);

ALTER TABLE product_profitability_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_profitability_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS product_profitability_audit_tenant_isolation ON product_profitability_audit_events;
CREATE POLICY product_profitability_audit_tenant_isolation ON product_profitability_audit_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
