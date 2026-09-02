-- =========================================================================
-- MODULE 15 STEP 09: BUSINESS FINANCIAL GOVERNANCE, BUDGET CONTROL & FORECAST
-- V20261022__create_business_financial_governance_and_budget_control.sql
-- =========================================================================

-- 1. Business Financial Budgets
CREATE TABLE IF NOT EXISTS business_financial_budgets (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    budget_name VARCHAR(200) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    dimension_type VARCHAR(64) NOT NULL,
    dimension_id VARCHAR(64) NOT NULL,
    allocated_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 1,
    effective_start_date BIGINT NOT NULL,
    effective_end_date BIGINT NOT NULL,
    description TEXT,
    created_by VARCHAR(64) NOT NULL,
    reviewed_by VARCHAR(64),
    approved_by VARCHAR(64),
    approved_at BIGINT,
    rejection_reason VARCHAR(500),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_bfb_tenant_period_dim_ver UNIQUE (tenant_id, project_id, period_id, dimension_type, dimension_id, version),
    CONSTRAINT chk_bfb_allocated_amount CHECK (allocated_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bfb_tenant_proj ON business_financial_budgets(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bfb_period ON business_financial_budgets(tenant_id, period_id);
CREATE INDEX IF NOT EXISTS idx_bfb_dimension ON business_financial_budgets(tenant_id, dimension_type, dimension_id);
CREATE INDEX IF NOT EXISTS idx_bfb_status ON business_financial_budgets(tenant_id, status);

ALTER TABLE business_financial_budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_budgets FORCE ROW LEVEL SECURITY;

CREATE POLICY bfb_tenant_isolation ON business_financial_budgets
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 2. Business Financial Budget Revisions
CREATE TABLE IF NOT EXISTS business_financial_budget_revisions (
    id VARCHAR(64) PRIMARY KEY,
    budget_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL,
    previous_allocated_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    new_allocated_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    revision_reason VARCHAR(500) NOT NULL,
    revised_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64),
    revised_at BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    CONSTRAINT chk_bfbr_amounts CHECK (previous_allocated_amount >= 0 AND new_allocated_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bfbr_budget ON business_financial_budget_revisions(tenant_id, budget_id);
CREATE INDEX IF NOT EXISTS idx_bfbr_tenant_proj ON business_financial_budget_revisions(tenant_id, project_id);

ALTER TABLE business_financial_budget_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_budget_revisions FORCE ROW LEVEL SECURITY;

CREATE POLICY bfbr_tenant_isolation ON business_financial_budget_revisions
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 3. Business Financial Budget Thresholds
CREATE TABLE IF NOT EXISTS business_financial_budget_thresholds (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    threshold_name VARCHAR(200) NOT NULL,
    dimension_type VARCHAR(64) NOT NULL DEFAULT 'OVERALL_BUSINESS',
    dimension_id VARCHAR(64) NOT NULL DEFAULT 'ALL',
    warning_utilization_pct DECIMAL(10, 4) NOT NULL DEFAULT 80.0000,
    critical_utilization_pct DECIMAL(10, 4) NOT NULL DEFAULT 100.0000,
    large_expense_threshold_amount DECIMAL(18, 4) NOT NULL DEFAULT 50000.0000,
    commitment_exposure_threshold_pct DECIMAL(10, 4) NOT NULL DEFAULT 90.0000,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_bfbt_tenant_dim UNIQUE (tenant_id, project_id, dimension_type, dimension_id),
    CONSTRAINT chk_bfbt_percentages CHECK (warning_utilization_pct >= 0 AND critical_utilization_pct >= warning_utilization_pct)
);

CREATE INDEX IF NOT EXISTS idx_bfbt_tenant_proj ON business_financial_budget_thresholds(tenant_id, project_id);

ALTER TABLE business_financial_budget_thresholds ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_budget_thresholds FORCE ROW LEVEL SECURITY;

CREATE POLICY bfbt_tenant_isolation ON business_financial_budget_thresholds
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 4. Business Financial Forecasts
CREATE TABLE IF NOT EXISTS business_financial_forecasts (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    forecast_name VARCHAR(200) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    dimension_type VARCHAR(64) NOT NULL,
    dimension_id VARCHAR(64) NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    actual_ytd_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    projected_remaining_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    forecast_total_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    run_rate_per_day DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    generated_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bff_tenant_proj ON business_financial_forecasts(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bff_period ON business_financial_forecasts(tenant_id, period_id);

ALTER TABLE business_financial_forecasts ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_forecasts FORCE ROW LEVEL SECURITY;

CREATE POLICY bff_tenant_isolation ON business_financial_forecasts
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 5. Business Financial Forecast Scenarios
CREATE TABLE IF NOT EXISTS business_financial_forecast_scenarios (
    id VARCHAR(64) PRIMARY KEY,
    forecast_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    scenario_type VARCHAR(32) NOT NULL,
    projected_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    variance_vs_budget DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    assumptions_json TEXT,
    created_at BIGINT NOT NULL,
    CONSTRAINT uq_bffs_forecast_scenario UNIQUE (tenant_id, forecast_id, scenario_type)
);

CREATE INDEX IF NOT EXISTS idx_bffs_forecast ON business_financial_forecast_scenarios(tenant_id, forecast_id);

ALTER TABLE business_financial_forecast_scenarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_forecast_scenarios FORCE ROW LEVEL SECURITY;

CREATE POLICY bffs_tenant_isolation ON business_financial_forecast_scenarios
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 6. Business Financial Governance Alerts
CREATE TABLE IF NOT EXISTS business_financial_governance_alerts (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'WARNING',
    source_dimension_type VARCHAR(64) NOT NULL,
    source_dimension_id VARCHAR(64) NOT NULL,
    message VARCHAR(500) NOT NULL,
    threshold_value DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    current_value DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    acknowledged_by VARCHAR(64),
    acknowledged_at BIGINT,
    acknowledgement_notes VARCHAR(500),
    resolved_by VARCHAR(64),
    resolved_at BIGINT,
    resolution_notes VARCHAR(500),
    dismissal_reason VARCHAR(500),
    period_id VARCHAR(64),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bfga_tenant_proj ON business_financial_governance_alerts(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bfga_status ON business_financial_governance_alerts(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_bfga_type ON business_financial_governance_alerts(tenant_id, alert_type);
CREATE INDEX IF NOT EXISTS idx_bfga_period ON business_financial_governance_alerts(tenant_id, period_id);

ALTER TABLE business_financial_governance_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_governance_alerts FORCE ROW LEVEL SECURITY;

CREATE POLICY bfga_tenant_isolation ON business_financial_governance_alerts
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 7. Business Financial Governance Audit Events
CREATE TABLE IF NOT EXISTS business_financial_governance_audit_events (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    outcome VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    target_id VARCHAR(64),
    target_type VARCHAR(64),
    timestamp BIGINT NOT NULL,
    details_json TEXT,
    client_ip VARCHAR(64),
    correlation_id VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_bfgae_tenant_proj ON business_financial_governance_audit_events(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_bfgae_timestamp ON business_financial_governance_audit_events(tenant_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_bfgae_event_type ON business_financial_governance_audit_events(tenant_id, event_type);

ALTER TABLE business_financial_governance_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_financial_governance_audit_events FORCE ROW LEVEL SECURITY;

CREATE POLICY bfgae_tenant_isolation ON business_financial_governance_audit_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
