-- Module 16 Step 04: Customer-Wise Profitability & Contribution Analysis Engine
-- Tables for customer profitability snapshots, cost components, revenue/cost attributions, reconciliation, audit, and unattributed items.

CREATE TABLE IF NOT EXISTS customer_profitability_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_name VARCHAR(255),
    customer_code VARCHAR(64),
    period_type VARCHAR(32) NOT NULL DEFAULT 'ALL_TIME',
    period_start BIGINT,
    period_end BIGINT,
    recognized_revenue NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    total_actual_cost NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    gross_profit NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    gross_margin_percentage NUMERIC(18, 4),
    attributable_variable_cost NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    attributable_fixed_cost NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    contribution_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin_percentage NUMERIC(18, 4),
    cost_to_revenue_percentage NUMERIC(18, 4),
    order_count INT NOT NULL DEFAULT 0,
    job_count INT NOT NULL DEFAULT 0,
    product_count INT NOT NULL DEFAULT 0,
    total_quantity_sold INT NOT NULL DEFAULT 0,
    average_order_value NUMERIC(18, 4),
    average_job_value NUMERIC(18, 4),
    average_revenue_per_unit NUMERIC(18, 4),
    average_cost_per_unit NUMERIC(18, 4),
    average_profit_per_unit NUMERIC(18, 4),
    unit_economics_status VARCHAR(64) NOT NULL DEFAULT 'AVAILABLE',
    profitability_classification VARCHAR(64) NOT NULL DEFAULT 'BREAK_EVEN',
    trend VARCHAR(64) NOT NULL DEFAULT 'STABLE',
    concentration_risk VARCHAR(64) NOT NULL DEFAULT 'CONCENTRATION_LOW',
    is_loss_making BOOLEAN NOT NULL DEFAULT FALSE,
    is_low_margin BOOLEAN NOT NULL DEFAULT FALSE,
    source_integrity_status VARCHAR(64) NOT NULL DEFAULT 'VERIFIED',
    is_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    reconciliation_discrepancy NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    calculation_version VARCHAR(64) NOT NULL DEFAULT 'CUSTOMER_PROFITABILITY_V1',
    generated_at BIGINT NOT NULL,
    generated_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    integrity_hash VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cust_prof_tenant_cust ON customer_profitability_snapshots(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_prof_generated ON customer_profitability_snapshots(generated_at);

CREATE TABLE IF NOT EXISTS customer_profitability_components (
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    percentage_of_total_cost NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    is_variable_cost BOOLEAN NOT NULL DEFAULT TRUE,
    source_count INT NOT NULL DEFAULT 0,
    allocation_basis VARCHAR(64) NOT NULL DEFAULT 'DIRECT',
    provenance_fingerprints TEXT,
    PRIMARY KEY (snapshot_id, component_type)
);

CREATE INDEX IF NOT EXISTS idx_cust_prof_comp_tenant ON customer_profitability_components(tenant_id, customer_id);

CREATE TABLE IF NOT EXISTS customer_profitability_revenue_attributions (
    revenue_attribution_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64),
    invoice_id VARCHAR(64),
    invoice_line_id VARCHAR(64),
    product_id VARCHAR(64),
    quantity INT NOT NULL DEFAULT 0,
    recognized_revenue NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    source_module VARCHAR(64) NOT NULL DEFAULT 'MODULE_14',
    source_entity_type VARCHAR(64) NOT NULL DEFAULT 'CUSTOMER_INVOICE_LINE',
    source_entity_id VARCHAR(64) NOT NULL,
    source_transaction_id VARCHAR(64),
    provenance_fingerprint VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cust_prof_rev_tenant ON customer_profitability_revenue_attributions(tenant_id, customer_id);

CREATE TABLE IF NOT EXISTS customer_profitability_cost_attributions (
    cost_attribution_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64),
    job_id VARCHAR(64),
    product_id VARCHAR(64),
    component_type VARCHAR(64) NOT NULL,
    directness VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
    is_variable_cost BOOLEAN NOT NULL DEFAULT TRUE,
    attributed_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    allocation_basis VARCHAR(64) NOT NULL DEFAULT 'DIRECT',
    numerator NUMERIC(18, 4),
    denominator NUMERIC(18, 4),
    allocation_ratio NUMERIC(18, 4) DEFAULT 1.0000,
    priority VARCHAR(64) NOT NULL DEFAULT 'PRIORITY_1_DIRECT_CUSTOMER',
    source_module VARCHAR(64) NOT NULL DEFAULT 'MODULE_16_STEP_02',
    source_entity_type VARCHAR(64) NOT NULL DEFAULT 'JOB_COST_SNAPSHOT',
    source_entity_id VARCHAR(64) NOT NULL,
    source_transaction_id VARCHAR(64),
    provenance_fingerprint VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cust_prof_cost_tenant ON customer_profitability_cost_attributions(tenant_id, customer_id);

CREATE TABLE IF NOT EXISTS customer_profitability_reconciliation_events (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    is_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    revenue_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    cost_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    profit_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    contribution_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    expected_revenue NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    actual_revenue NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    expected_cost NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    actual_cost NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    expected_gross_profit NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    actual_gross_profit NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    discrepancies_json TEXT,
    checked_at BIGINT NOT NULL,
    checked_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'
);

CREATE INDEX IF NOT EXISTS idx_cust_prof_recon_tenant ON customer_profitability_reconciliation_events(tenant_id, customer_id);

CREATE TABLE IF NOT EXISTS customer_profitability_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL DEFAULT 'STAFF',
    outcome VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    details TEXT,
    correlation_id VARCHAR(64),
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cust_prof_audit_tenant ON customer_profitability_audit_events(tenant_id, customer_id);

CREATE TABLE IF NOT EXISTS customer_profitability_unattributed_items (
    item_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    item_type VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    source_module VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    reason TEXT,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cust_prof_unattr_tenant ON customer_profitability_unattributed_items(tenant_id, project_id);

-- Enable & Force Row Level Security on all Customer Profitability tables
ALTER TABLE customer_profitability_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_profitability_snapshots FORCE ROW LEVEL SECURITY;
CREATE POLICY customer_profitability_snapshots_tenant_isolation ON customer_profitability_snapshots
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_profitability_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_profitability_components FORCE ROW LEVEL SECURITY;
CREATE POLICY customer_profitability_components_tenant_isolation ON customer_profitability_components
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_profitability_revenue_attributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_profitability_revenue_attributions FORCE ROW LEVEL SECURITY;
CREATE POLICY customer_profitability_revenue_attributions_tenant_isolation ON customer_profitability_revenue_attributions
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_profitability_cost_attributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_profitability_cost_attributions FORCE ROW LEVEL SECURITY;
CREATE POLICY customer_profitability_cost_attributions_tenant_isolation ON customer_profitability_cost_attributions
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_profitability_reconciliation_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_profitability_reconciliation_events FORCE ROW LEVEL SECURITY;
CREATE POLICY customer_profitability_reconciliation_events_tenant_isolation ON customer_profitability_reconciliation_events
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_profitability_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_profitability_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY customer_profitability_audit_events_tenant_isolation ON customer_profitability_audit_events
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_profitability_unattributed_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_profitability_unattributed_items FORCE ROW LEVEL SECURITY;
CREATE POLICY customer_profitability_unattributed_items_tenant_isolation ON customer_profitability_unattributed_items
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
