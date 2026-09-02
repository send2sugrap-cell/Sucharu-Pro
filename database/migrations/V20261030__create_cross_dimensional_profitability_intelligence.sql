-- ============================================================================
-- Migration: V20261030__create_cross_dimensional_profitability_intelligence.sql
-- Module 16 Step 07: Cross-Dimensional Profitability Intelligence & Management Decision Engine
-- ============================================================================

-- 1. Snapshots Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_snapshots (
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    analysis_period_id VARCHAR(64) NOT NULL,
    scope VARCHAR(32) NOT NULL DEFAULT 'FULL_BUSINESS',
    generated_at BIGINT NOT NULL,
    generated_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    calculation_version VARCHAR(32) NOT NULL DEFAULT 'MODULE16_INTELLIGENCE_V1',
    snapshot_version INT NOT NULL DEFAULT 1,

    revenue NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    gross_profit NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    gross_margin NUMERIC(19, 4),
    cost_to_revenue_percentage NUMERIC(19, 4),
    contribution_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin NUMERIC(19, 4),

    profitability_classification VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    health_status VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    confidence_status VARCHAR(32) NOT NULL DEFAULT 'HIGH',
    source_readiness VARCHAR(32) NOT NULL DEFAULT 'READY',

    dimension_count INT NOT NULL DEFAULT 0,
    relationship_count INT NOT NULL DEFAULT 0,
    driver_count INT NOT NULL DEFAULT 0,
    leakage_count INT NOT NULL DEFAULT 0,
    priority_count INT NOT NULL DEFAULT 0,

    integrity_hash VARCHAR(128) NOT NULL DEFAULT '',
    hash_algorithm VARCHAR(32) NOT NULL DEFAULT 'SHA-256',
    is_certified BOOLEAN NOT NULL DEFAULT FALSE,
    certified_at BIGINT,
    certificate_id VARCHAR(64),
    warnings TEXT,

    PRIMARY KEY (tenant_id, snapshot_id)
);

CREATE INDEX IF NOT EXISTS idx_profit_intel_snap_period
ON profitability_intelligence_snapshots (tenant_id, analysis_period_id, generated_at DESC);

-- 2. Dimensions Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_dimensions (
    insight_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    dimension_type VARCHAR(32) NOT NULL,
    dimension_id VARCHAR(64) NOT NULL,
    dimension_label VARCHAR(255) NOT NULL,
    revenue NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    gross_profit NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    margin NUMERIC(19, 4),
    contribution NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin NUMERIC(19, 4),
    unit_count BIGINT NOT NULL DEFAULT 0,
    profit_per_unit NUMERIC(19, 4),
    rank INT NOT NULL DEFAULT 1,
    share_of_revenue NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    share_of_profit NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    share_of_cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    trend_direction VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    health_status VARCHAR(32) NOT NULL DEFAULT 'HEALTHY',
    confidence_status VARCHAR(32) NOT NULL DEFAULT 'HIGH',

    PRIMARY KEY (tenant_id, insight_id)
);

CREATE INDEX IF NOT EXISTS idx_profit_intel_dim_lookup
ON profitability_intelligence_dimensions (tenant_id, snapshot_id, dimension_type);

-- 3. Relationships Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_relationships (
    relationship_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    from_dimension_type VARCHAR(32) NOT NULL,
    from_entity_id VARCHAR(64) NOT NULL,
    from_entity_label VARCHAR(255) NOT NULL,
    to_dimension_type VARCHAR(32) NOT NULL,
    to_entity_id VARCHAR(64) NOT NULL,
    to_entity_label VARCHAR(255) NOT NULL,
    revenue NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    gross_profit NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    gross_margin NUMERIC(19, 4),
    contribution NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin NUMERIC(19, 4),
    quantity BIGINT NOT NULL DEFAULT 0,
    average_revenue_per_unit NUMERIC(19, 4),
    average_cost_per_unit NUMERIC(19, 4),
    average_profit_per_unit NUMERIC(19, 4),
    revenue_share NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    cost_share NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    profit_share NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    trend_direction VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    classification VARCHAR(32) NOT NULL DEFAULT 'PROFITABLE',
    source_integrity_status VARCHAR(32) NOT NULL DEFAULT 'VALID',
    provenance_fingerprint VARCHAR(128) NOT NULL DEFAULT '',

    PRIMARY KEY (tenant_id, relationship_id)
);

CREATE INDEX IF NOT EXISTS idx_profit_intel_rel_lookup
ON profitability_intelligence_relationships (tenant_id, snapshot_id, from_dimension_type, to_dimension_type);

-- 4. Drivers Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_drivers (
    driver_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    dimension_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    entity_label VARCHAR(255) NOT NULL,
    driver_type VARCHAR(32) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    impact_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    impact_percentage NUMERIC(19, 4),
    rank INT NOT NULL,
    explanation TEXT NOT NULL,
    source_references TEXT,
    fingerprint VARCHAR(128) NOT NULL,

    PRIMARY KEY (tenant_id, driver_id)
);

CREATE INDEX IF NOT EXISTS idx_profit_intel_driver_lookup
ON profitability_intelligence_drivers (tenant_id, snapshot_id, driver_type);

-- 5. Leakages Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_leakages (
    leakage_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    dimension_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    entity_label VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    estimated_impact NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    revenue_context NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    cost_context NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    profit_impact NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    severity VARCHAR(32) NOT NULL,
    confidence VARCHAR(32) NOT NULL,
    source_integrity_status VARCHAR(32) NOT NULL DEFAULT 'VALID',
    recommended_action_code VARCHAR(64) NOT NULL,
    provenance_references TEXT,

    PRIMARY KEY (tenant_id, leakage_id)
);

CREATE INDEX IF NOT EXISTS idx_profit_intel_leakage_lookup
ON profitability_intelligence_leakages (tenant_id, snapshot_id, severity);

-- 6. Priorities Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_priorities (
    priority_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    dimension_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    entity_label VARCHAR(255) NOT NULL,
    issue_title VARCHAR(255) NOT NULL,
    issue_description TEXT NOT NULL,
    priority_level VARCHAR(32) NOT NULL,
    priority_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    financial_impact NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    severity_weight NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    trend_weight NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    concentration_weight NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    frequency_weight NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    trend VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    confidence VARCHAR(32) NOT NULL DEFAULT 'HIGH',
    recommended_action_code VARCHAR(64) NOT NULL,
    source_fingerprints TEXT,

    PRIMARY KEY (tenant_id, priority_id)
);

CREATE INDEX IF NOT EXISTS idx_profit_intel_priority_lookup
ON profitability_intelligence_priorities (tenant_id, snapshot_id, priority_level, priority_score DESC);

-- 7. Health Scores Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_health_scores (
    score_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    overall_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    margin_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    trend_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    cost_stability_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    revenue_stability_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    concentration_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    vendor_dependency_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    data_integrity_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    attribution_completeness_score NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    health_level VARCHAR(32) NOT NULL,
    explanation TEXT NOT NULL,
    calculated_at BIGINT NOT NULL,

    PRIMARY KEY (tenant_id, score_id)
);

-- 8. Provenance Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_provenance (
    provenance_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    source_module VARCHAR(64) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    source_transaction_id VARCHAR(64),
    source_snapshot_id VARCHAR(64),
    dimension_type VARCHAR(32) NOT NULL,
    dimension_entity_id VARCHAR(64) NOT NULL,
    metric_type VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    fingerprint VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL,

    PRIMARY KEY (tenant_id, provenance_id)
);

-- 9. Reconciliation Events Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_reconciliations (
    event_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64),
    is_balanced BOOLEAN NOT NULL DEFAULT FALSE,
    revenue_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    cost_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    profit_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    margin_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    contribution_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    relationship_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    driver_impact_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    assertions_json TEXT,
    error_details TEXT,
    timestamp BIGINT NOT NULL,

    PRIMARY KEY (tenant_id, event_id)
);

-- 10. Audit Events Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_audit_events (
    audit_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    period_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64),
    scope VARCHAR(32) NOT NULL DEFAULT 'FULL_BUSINESS',
    entity_id VARCHAR(64),
    result_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    correlation_id VARCHAR(64),
    metadata TEXT,
    integrity_hash VARCHAR(128),
    timestamp BIGINT NOT NULL,

    PRIMARY KEY (tenant_id, audit_id)
);

-- 11. Idempotency Table
CREATE TABLE IF NOT EXISTS profitability_intelligence_idempotency_records (
    idempotency_key VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,

    PRIMARY KEY (tenant_id, idempotency_key)
);

-- ============================================================================
-- Row-Level Security (RLS) Policies
-- ============================================================================

ALTER TABLE profitability_intelligence_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_snapshots FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_snap ON profitability_intelligence_snapshots
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_dimensions ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_dimensions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_dim ON profitability_intelligence_dimensions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_relationships ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_relationships FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_rel ON profitability_intelligence_relationships
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_drivers ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_drivers FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_driver ON profitability_intelligence_drivers
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_leakages ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_leakages FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_leakage ON profitability_intelligence_leakages
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_priorities ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_priorities FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_prio ON profitability_intelligence_priorities
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_health_scores ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_health_scores FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_health ON profitability_intelligence_health_scores
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_provenance ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_provenance FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_prov ON profitability_intelligence_provenance
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_reconciliations ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_reconciliations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_recon ON profitability_intelligence_reconciliations
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_audit ON profitability_intelligence_audit_events
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE profitability_intelligence_idempotency_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE profitability_intelligence_idempotency_records FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_profit_intel_idempotency ON profitability_intelligence_idempotency_records
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));
