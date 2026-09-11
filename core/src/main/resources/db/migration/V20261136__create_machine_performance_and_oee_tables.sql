-- Module 21 Step 08: Machine Performance & OEE Foundation
-- Establishes canonical machine OEE metrics table with RLS tenant isolation

CREATE TABLE IF NOT EXISTS machine_oee_metrics (
    metric_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL REFERENCES machine_registry(machine_id) ON DELETE CASCADE,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    planned_production_seconds BIGINT NOT NULL DEFAULT 0,
    run_time_seconds BIGINT NOT NULL DEFAULT 0,
    downtime_seconds BIGINT NOT NULL DEFAULT 0,
    ideal_rate_units_per_hour NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    actual_output_units NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    good_output_units NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    rejected_output_units NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    availability_ratio NUMERIC(7, 4) NOT NULL DEFAULT 0.0000,
    performance_ratio NUMERIC(7, 4) NOT NULL DEFAULT 0.0000,
    quality_ratio NUMERIC(7, 4) NOT NULL DEFAULT 0.0000,
    oee_ratio NUMERIC(7, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_machine_oee_tenant_machine ON machine_oee_metrics(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_machine_oee_tenant_period ON machine_oee_metrics(tenant_id, period_start, period_end);

ALTER TABLE machine_oee_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_oee_metrics FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_oee_metrics_tenant_isolation ON machine_oee_metrics;
CREATE POLICY machine_oee_metrics_tenant_isolation ON machine_oee_metrics
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );
