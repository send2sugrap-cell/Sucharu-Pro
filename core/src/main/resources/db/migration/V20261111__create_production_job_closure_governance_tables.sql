-- =============================================================================
-- V20261111__create_production_job_closure_governance_tables.sql
-- Module 17 Step 10: Production Job Closure, Archival, Traceability & Enterprise Governance
-- =============================================================================

CREATE TABLE IF NOT EXISTS production_job_closure_records (
    closure_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    closure_status VARCHAR(32) NOT NULL DEFAULT 'GOVERNANCE_SEALED',
    readiness_audit_json TEXT NOT NULL,
    scorecard_json TEXT NOT NULL,
    provenance_graph_json TEXT NOT NULL,
    post_mortem_summary_json TEXT NOT NULL,
    master_seal_hash VARCHAR(128) NOT NULL,
    total_good_units_released NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    grand_total_actual_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_cost_variance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    overall_manufacturing_score NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    performance_grade VARCHAR(8) NOT NULL DEFAULT 'A',
    closed_at BIGINT NOT NULL,
    closed_by VARCHAR(64) NOT NULL DEFAULT 'plant-manager'
);

CREATE INDEX IF NOT EXISTS idx_job_closure_tenant_job ON production_job_closure_records(tenant_id, execution_job_id);

ALTER TABLE production_job_closure_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_job_closure_records FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'production_job_closure_tenant_isolation') THEN
        CREATE POLICY production_job_closure_tenant_isolation ON production_job_closure_records
            USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
            WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
    END IF;
END $$;


CREATE TABLE IF NOT EXISTS production_job_scorecard_records (
    scorecard_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    on_time_in_full_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    right_first_time_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    cost_adherence_index NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    machine_efficiency_index NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    quality_yield_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    overall_manufacturing_index NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    performance_grade VARCHAR(8) NOT NULL DEFAULT 'A',
    calculated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_scorecard_tenant_job ON production_job_scorecard_records(tenant_id, execution_job_id);

ALTER TABLE production_job_scorecard_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_job_scorecard_records FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'production_job_scorecard_tenant_isolation') THEN
        CREATE POLICY production_job_scorecard_tenant_isolation ON production_job_scorecard_records
            USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
            WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
    END IF;
END $$;


CREATE TABLE IF NOT EXISTS production_job_closure_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_closure_audit_tenant_job ON production_job_closure_audit_events(tenant_id, execution_job_id);

ALTER TABLE production_job_closure_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_job_closure_audit_events FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'production_job_closure_audit_tenant_isolation') THEN
        CREATE POLICY production_job_closure_audit_tenant_isolation ON production_job_closure_audit_events
            USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
            WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
    END IF;
END $$;
