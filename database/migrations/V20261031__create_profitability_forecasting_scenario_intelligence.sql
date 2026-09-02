-- =============================================================================
-- SUCHARU PRO ERP — MODULE 16 STEP 08 DATABASE MIGRATION
-- PROFITABILITY FORECASTING, SCENARIO & FORWARD-LOOKING BUSINESS INTELLIGENCE
-- =============================================================================

-- 1. Forecast Snapshots Table
CREATE TABLE IF NOT EXISTS profitability_forecast_snapshots (
    snapshot_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    forecast_version INT NOT NULL DEFAULT 1,
    forecast_method VARCHAR(50) NOT NULL,
    scenario_type VARCHAR(50) NOT NULL,
    scenario_id VARCHAR(100),
    target_scope VARCHAR(50) NOT NULL,
    target_entity_id VARCHAR(100) NOT NULL,
    target_entity_label VARCHAR(255) NOT NULL,
    historical_period_start VARCHAR(50) NOT NULL,
    historical_period_end VARCHAR(50) NOT NULL,
    forecast_period_start VARCHAR(50) NOT NULL,
    forecast_period_end VARCHAR(50) NOT NULL,
    horizon VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    projected_revenue NUMERIC(18, 4) NOT NULL,
    projected_total_cost NUMERIC(18, 4) NOT NULL,
    projected_gross_profit NUMERIC(18, 4) NOT NULL,
    projected_gross_margin NUMERIC(18, 4),
    projected_contribution NUMERIC(18, 4) NOT NULL,
    projected_contribution_margin NUMERIC(18, 4),
    projected_units BIGINT NOT NULL DEFAULT 0,
    projected_revenue_per_unit NUMERIC(18, 4),
    projected_cost_per_unit NUMERIC(18, 4),
    projected_profit_per_unit NUMERIC(18, 4),
    baseline_revenue NUMERIC(18, 4),
    baseline_cost NUMERIC(18, 4),
    baseline_gross_profit NUMERIC(18, 4),
    baseline_gross_margin NUMERIC(18, 4),
    projected_revenue_delta NUMERIC(18, 4),
    projected_cost_delta NUMERIC(18, 4),
    projected_profit_delta NUMERIC(18, 4),
    projected_margin_delta NUMERIC(18, 4),
    break_even_revenue NUMERIC(18, 4),
    break_even_units BIGINT,
    margin_of_safety NUMERIC(18, 4),
    is_break_even_attainable BOOLEAN NOT NULL DEFAULT TRUE,
    confidence_score NUMERIC(18, 4) NOT NULL,
    confidence_level VARCHAR(50) NOT NULL,
    risk_level VARCHAR(50) NOT NULL,
    source_readiness VARCHAR(50) NOT NULL,
    generated_at BIGINT NOT NULL,
    generated_by VARCHAR(100) NOT NULL,
    calculation_version VARCHAR(50) NOT NULL,
    integrity_hash VARCHAR(64) NOT NULL,
    hash_algorithm VARCHAR(20) NOT NULL DEFAULT 'SHA-256',
    warnings TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_profitability_forecast_snapshots PRIMARY KEY (tenant_id, snapshot_id)
);

CREATE INDEX IF NOT EXISTS idx_profit_forecast_scope ON profitability_forecast_snapshots (tenant_id, target_scope, target_entity_id);
CREATE INDEX IF NOT EXISTS idx_profit_forecast_generated ON profitability_forecast_snapshots (tenant_id, generated_at DESC);

ALTER TABLE profitability_forecast_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_snapshots FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_snapshots ON profitability_forecast_snapshots;
CREATE POLICY tenant_isolation_profitability_forecast_snapshots ON profitability_forecast_snapshots
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

-- 2. Forecast Cost Components Table
CREATE TABLE IF NOT EXISTS profitability_forecast_components (
    component_id VARCHAR(100) NOT NULL,
    forecast_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    component_type VARCHAR(50) NOT NULL,
    projected_amount NUMERIC(18, 4) NOT NULL,
    percentage_of_total_cost NUMERIC(18, 4) NOT NULL,
    baseline_amount NUMERIC(18, 4),
    delta_amount NUMERIC(18, 4),
    growth_rate NUMERIC(18, 4),
    driver_description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_profitability_forecast_components PRIMARY KEY (tenant_id, component_id)
);

CREATE INDEX IF NOT EXISTS idx_profit_forecast_components_fc ON profitability_forecast_components (tenant_id, forecast_id);

ALTER TABLE profitability_forecast_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_components FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_components ON profitability_forecast_components;
CREATE POLICY tenant_isolation_profitability_forecast_components ON profitability_forecast_components
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

-- 3. Scenarios Table
CREATE TABLE IF NOT EXISTS profitability_forecast_scenarios (
    scenario_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    scenario_name VARCHAR(255) NOT NULL,
    scenario_type VARCHAR(50) NOT NULL,
    description TEXT,
    target_scope VARCHAR(50) NOT NULL,
    revenue_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    volume_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    material_cost_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    labour_cost_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    machine_cost_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    vendor_cost_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rework_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    wastage_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    indirect_cost_adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_profitability_forecast_scenarios PRIMARY KEY (tenant_id, scenario_id)
);

ALTER TABLE profitability_forecast_scenarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_scenarios FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_scenarios ON profitability_forecast_scenarios;
CREATE POLICY tenant_isolation_profitability_forecast_scenarios ON profitability_forecast_scenarios
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

-- 4. Management Insights Table
CREATE TABLE IF NOT EXISTS profitability_forecast_insights (
    insight_id VARCHAR(100) NOT NULL,
    forecast_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    insight_type VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    dimension_type VARCHAR(50) NOT NULL,
    target_entity_id VARCHAR(100) NOT NULL,
    target_entity_label VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    explanation TEXT NOT NULL,
    financial_impact NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    supporting_references TEXT,
    action_code VARCHAR(100),
    generated_at BIGINT NOT NULL,
    CONSTRAINT pk_profitability_forecast_insights PRIMARY KEY (tenant_id, insight_id)
);

ALTER TABLE profitability_forecast_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_insights FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_insights ON profitability_forecast_insights;
CREATE POLICY tenant_isolation_profitability_forecast_insights ON profitability_forecast_insights
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

-- 5. Provenance Table
CREATE TABLE IF NOT EXISTS profitability_forecast_provenance (
    provenance_id VARCHAR(100) NOT NULL,
    forecast_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    source_module VARCHAR(50) NOT NULL,
    source_entity_type VARCHAR(50) NOT NULL,
    source_entity_id VARCHAR(100) NOT NULL,
    source_snapshot_id VARCHAR(100),
    source_period_id VARCHAR(100),
    metric_type VARCHAR(50) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_profitability_forecast_provenance PRIMARY KEY (tenant_id, provenance_id)
);

ALTER TABLE profitability_forecast_provenance ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_provenance FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_provenance ON profitability_forecast_provenance;
CREATE POLICY tenant_isolation_profitability_forecast_provenance ON profitability_forecast_provenance
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

-- 6. Reconciliations Table
CREATE TABLE IF NOT EXISTS profitability_forecast_reconciliations (
    event_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    forecast_id VARCHAR(100) NOT NULL,
    is_balanced BOOLEAN NOT NULL,
    revenue_difference NUMERIC(18, 4) NOT NULL,
    cost_difference NUMERIC(18, 4) NOT NULL,
    profit_difference NUMERIC(18, 4) NOT NULL,
    margin_difference NUMERIC(18, 4) NOT NULL,
    component_difference NUMERIC(18, 4) NOT NULL,
    scenario_difference NUMERIC(18, 4) NOT NULL,
    assertions_payload JSONB,
    error_details TEXT,
    timestamp BIGINT NOT NULL,
    CONSTRAINT pk_profitability_forecast_reconciliations PRIMARY KEY (tenant_id, event_id)
);

ALTER TABLE profitability_forecast_reconciliations ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_reconciliations FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_reconciliations ON profitability_forecast_reconciliations;
CREATE POLICY tenant_isolation_profitability_forecast_reconciliations ON profitability_forecast_reconciliations
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

-- 7. Forecast vs Actual Comparisons Table
CREATE TABLE IF NOT EXISTS profitability_forecast_actual_comparisons (
    comparison_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    forecast_id VARCHAR(100) NOT NULL,
    actual_period_id VARCHAR(100) NOT NULL,
    target_scope VARCHAR(50) NOT NULL,
    target_entity_id VARCHAR(100) NOT NULL,
    target_entity_label VARCHAR(255) NOT NULL,
    forecast_revenue NUMERIC(18, 4) NOT NULL,
    actual_revenue NUMERIC(18, 4) NOT NULL,
    revenue_variance NUMERIC(18, 4) NOT NULL,
    revenue_variance_pct NUMERIC(18, 4),
    forecast_cost NUMERIC(18, 4) NOT NULL,
    actual_cost NUMERIC(18, 4) NOT NULL,
    cost_variance NUMERIC(18, 4) NOT NULL,
    cost_variance_pct NUMERIC(18, 4),
    forecast_profit NUMERIC(18, 4) NOT NULL,
    actual_profit NUMERIC(18, 4) NOT NULL,
    profit_variance NUMERIC(18, 4) NOT NULL,
    profit_variance_pct NUMERIC(18, 4),
    forecast_margin NUMERIC(18, 4),
    actual_margin NUMERIC(18, 4),
    margin_variance_pct NUMERIC(18, 4),
    forecast_units BIGINT NOT NULL,
    actual_units BIGINT NOT NULL,
    units_variance BIGINT NOT NULL,
    is_directionally_accurate BOOLEAN NOT NULL,
    mape NUMERIC(18, 4),
    evaluation_notes TEXT,
    compared_at BIGINT NOT NULL,
    CONSTRAINT pk_profitability_forecast_actual_comparisons PRIMARY KEY (tenant_id, comparison_id)
);

ALTER TABLE profitability_forecast_actual_comparisons ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_actual_comparisons FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_actual_comparisons ON profitability_forecast_actual_comparisons;
CREATE POLICY tenant_isolation_profitability_forecast_actual_comparisons ON profitability_forecast_actual_comparisons
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

-- 8. Audit Events Table
CREATE TABLE IF NOT EXISTS profitability_forecast_audits (
    audit_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    forecast_id VARCHAR(100) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    details TEXT NOT NULL,
    previous_state_hash VARCHAR(64),
    new_state_hash VARCHAR(64),
    timestamp BIGINT NOT NULL,
    CONSTRAINT pk_profitability_forecast_audits PRIMARY KEY (tenant_id, audit_id)
);

ALTER TABLE profitability_forecast_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_audits FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_audits ON profitability_forecast_audits;
CREATE POLICY tenant_isolation_profitability_forecast_audits ON profitability_forecast_audits
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

-- 9. Idempotency Table
CREATE TABLE IF NOT EXISTS profitability_forecast_idempotency (
    idempotency_key VARCHAR(150) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    forecast_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_profitability_forecast_idempotency PRIMARY KEY (tenant_id, idempotency_key)
);

ALTER TABLE profitability_forecast_idempotency ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_forecast_idempotency FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profitability_forecast_idempotency ON profitability_forecast_idempotency;
CREATE POLICY tenant_isolation_profitability_forecast_idempotency ON profitability_forecast_idempotency
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
