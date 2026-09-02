-- ====================================================================================
-- MODULE 16 STEP 09: PROFITABILITY ALERTS, EARLY-WARNING & MANAGEMENT ACTIONS ENGINE
-- Flyway Database Migration
-- Version: V20261101
-- ====================================================================================

-- 1. Profitability Alert Rules (Tenant Configured Thresholds)
CREATE TABLE IF NOT EXISTS profitability_alert_rules (
    rule_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    alert_type VARCHAR(64) NOT NULL,
    dimension_type VARCHAR(64) NOT NULL,
    threshold_metric VARCHAR(128) NOT NULL,
    threshold_value NUMERIC(18, 4) NOT NULL,
    comparison_operator VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    effective_from BIGINT NOT NULL DEFAULT 0,
    effective_to BIGINT,
    version INT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profit_alert_rules_tenant ON profitability_alert_rules(tenant_id, project_id, alert_type, dimension_type);

-- 2. Profitability Alerts
CREATE TABLE IF NOT EXISTS profitability_alerts (
    alert_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    dimension_type VARCHAR(64) NOT NULL,
    dimension_id VARCHAR(128) NOT NULL,
    dimension_label VARCHAR(255) NOT NULL,
    period_id VARCHAR(64),
    source_module VARCHAR(64) NOT NULL,
    source_step VARCHAR(64) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(128) NOT NULL,
    trigger_metric VARCHAR(128) NOT NULL,
    observed_value NUMERIC(18, 4) NOT NULL,
    threshold_value NUMERIC(18, 4) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    financial_impact NUMERIC(18, 4) NOT NULL,
    detected_at BIGINT NOT NULL,
    first_detected_at BIGINT NOT NULL,
    last_detected_at BIGINT NOT NULL,
    occurrence_count INT NOT NULL DEFAULT 1,
    fingerprint VARCHAR(128) NOT NULL,
    integrity_hash VARCHAR(128) NOT NULL,
    explanation TEXT NOT NULL,
    recommended_action_code VARCHAR(64),
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    rule_id VARCHAR(64),
    acknowledged_at BIGINT,
    acknowledged_by VARCHAR(64),
    resolved_at BIGINT,
    resolved_by VARCHAR(64),
    resolution_notes TEXT,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_profit_alerts_tenant_status ON profitability_alerts(tenant_id, project_id, status, severity);
CREATE INDEX IF NOT EXISTS idx_profit_alerts_fingerprint ON profitability_alerts(tenant_id, fingerprint);

-- 3. Profitability Alert Occurrences
CREATE TABLE IF NOT EXISTS profitability_alert_occurrences (
    occurrence_id VARCHAR(64) PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL REFERENCES profitability_alerts(alert_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    detected_at BIGINT NOT NULL,
    observed_value NUMERIC(18, 4) NOT NULL,
    financial_impact NUMERIC(18, 4) NOT NULL,
    previous_status VARCHAR(32) NOT NULL,
    trigger_details TEXT,
    source_snapshot_id VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_profit_alert_occ_alert ON profitability_alert_occurrences(tenant_id, alert_id);

-- 4. Profitability Management Actions
CREATE TABLE IF NOT EXISTS profitability_management_actions (
    action_id VARCHAR(64) PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL REFERENCES profitability_alerts(alert_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    action_title VARCHAR(255) NOT NULL,
    action_description TEXT NOT NULL,
    priority_score NUMERIC(18, 4) NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_to VARCHAR(64),
    assigned_by VARCHAR(64),
    due_at BIGINT,
    started_at BIGINT,
    completed_at BIGINT,
    verified_at BIGINT,
    verified_by VARCHAR(64),
    expected_financial_impact NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    realized_financial_impact NUMERIC(18, 4),
    outcome_notes TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    integrity_hash VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profit_actions_tenant_status ON profitability_management_actions(tenant_id, project_id, status, priority_score DESC);

-- 5. Profitability Action Outcomes
CREATE TABLE IF NOT EXISTS profitability_action_outcomes (
    outcome_id VARCHAR(64) PRIMARY KEY,
    action_id VARCHAR(64) NOT NULL REFERENCES profitability_management_actions(action_id) ON DELETE CASCADE,
    alert_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    evaluated_at BIGINT NOT NULL,
    metric_before NUMERIC(18, 4) NOT NULL,
    metric_after NUMERIC(18, 4) NOT NULL,
    improvement_percentage NUMERIC(18, 4) NOT NULL,
    realized_savings_or_revenue NUMERIC(18, 4) NOT NULL,
    is_effective BOOLEAN NOT NULL,
    evaluation_notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_profit_action_outcomes_tenant ON profitability_action_outcomes(tenant_id, action_id);

-- 6. Profitability Alert Correlations
CREATE TABLE IF NOT EXISTS profitability_alert_correlations (
    correlation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    correlation_title VARCHAR(255) NOT NULL,
    primary_dimension VARCHAR(64) NOT NULL,
    primary_entity_id VARCHAR(128) NOT NULL,
    primary_entity_label VARCHAR(255) NOT NULL,
    correlated_alert_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    composite_severity VARCHAR(32) NOT NULL,
    total_financial_impact NUMERIC(18, 4) NOT NULL,
    correlation_reason TEXT NOT NULL,
    detected_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profit_correlations_tenant ON profitability_alert_correlations(tenant_id, project_id);

-- 7. Profitability Alert Escalations
CREATE TABLE IF NOT EXISTS profitability_alert_escalations (
    escalation_id VARCHAR(64) PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL REFERENCES profitability_alerts(alert_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    escalation_level VARCHAR(32) NOT NULL,
    age_in_hours BIGINT NOT NULL,
    recurrence_count INT NOT NULL,
    is_action_overdue BOOLEAN NOT NULL,
    financial_impact NUMERIC(18, 4) NOT NULL,
    justification TEXT NOT NULL,
    calculated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profit_escalations_tenant ON profitability_alert_escalations(tenant_id, escalation_level);

-- 8. Profitability Monitoring Snapshots
CREATE TABLE IF NOT EXISTS profitability_monitoring_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64),
    total_active_alerts INT NOT NULL,
    critical_alert_count INT NOT NULL,
    high_alert_count INT NOT NULL,
    medium_alert_count INT NOT NULL,
    low_alert_count INT NOT NULL,
    total_unresolved_financial_impact NUMERIC(18, 4) NOT NULL,
    open_action_count INT NOT NULL,
    overdue_action_count INT NOT NULL,
    recurring_issue_count INT NOT NULL,
    escalated_alert_count INT NOT NULL,
    severity_distribution JSONB NOT NULL DEFAULT '{}'::jsonb,
    dimension_distribution JSONB NOT NULL DEFAULT '{}'::jsonb,
    generated_at BIGINT NOT NULL,
    integrity_hash VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profit_mon_snapshots_tenant ON profitability_monitoring_snapshots(tenant_id, project_id, generated_at DESC);

-- 9. Profitability Alert Provenance
CREATE TABLE IF NOT EXISTS profitability_alert_provenance (
    provenance_id VARCHAR(64) PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL REFERENCES profitability_alerts(alert_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    source_module VARCHAR(64) NOT NULL,
    source_step VARCHAR(64) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(128) NOT NULL,
    metric_key VARCHAR(128) NOT NULL,
    metric_value NUMERIC(18, 4) NOT NULL,
    calculation_timestamp BIGINT NOT NULL,
    provenance_hash VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profit_alert_prov_alert ON profitability_alert_provenance(tenant_id, alert_id);

-- 10. Profitability Alert Audit Events
CREATE TABLE IF NOT EXISTS profitability_alert_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    alert_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    previous_state VARCHAR(64),
    new_state VARCHAR(64) NOT NULL,
    notes TEXT,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profit_alert_audit_tenant ON profitability_alert_audit_events(tenant_id, alert_id, timestamp DESC);

-- ====================================================================================
-- ENABLE AND FORCE ROW LEVEL SECURITY
-- ====================================================================================

ALTER TABLE profitability_alert_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_alert_rules FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_alert_rules ON profitability_alert_rules
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_alerts FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_alerts ON profitability_alerts
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_alert_occurrences ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_alert_occurrences FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_alert_occurrences ON profitability_alert_occurrences
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_management_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_management_actions FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_management_actions ON profitability_management_actions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_action_outcomes ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_action_outcomes FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_action_outcomes ON profitability_action_outcomes
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_alert_correlations ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_alert_correlations FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_alert_correlations ON profitability_alert_correlations
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_alert_escalations ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_alert_escalations FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_alert_escalations ON profitability_alert_escalations
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_monitoring_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_monitoring_snapshots FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_monitoring_snapshots ON profitability_monitoring_snapshots
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_alert_provenance ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_alert_provenance FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_alert_provenance ON profitability_alert_provenance
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_alert_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_alert_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profitability_alert_audit_events ON profitability_alert_audit_events
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));
