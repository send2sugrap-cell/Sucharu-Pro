-- ============================================================================
-- SUCHARU PRO ERP — DATABASE MIGRATION
-- Module 17 Step 07: Shop-Floor Live Execution Tracking, Material Consumption,
-- Machine Telemetry & Output Recording Engine
-- ============================================================================

-- 1. Operator Time Tracking
CREATE TABLE IF NOT EXISTS operator_time_tracking (
    record_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    work_order_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    sequence_number INT NOT NULL,
    stage_type VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL,
    machine_name VARCHAR(128) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operator_name VARCHAR(128) NOT NULL,
    current_state VARCHAR(32) NOT NULL DEFAULT 'IDLE',
    started_at BIGINT,
    setup_minutes INT NOT NULL DEFAULT 0,
    run_minutes INT NOT NULL DEFAULT 0,
    downtime_minutes INT NOT NULL DEFAULT 0,
    good_quantity_produced NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    scrap_quantity_produced NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    paused_at BIGINT,
    pause_reason VARCHAR(255),
    completed_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_operator_time_tenant_job ON operator_time_tracking(tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_operator_time_tenant_wo ON operator_time_tracking(tenant_id, work_order_id);
CREATE INDEX IF NOT EXISTS idx_operator_time_tenant_op ON operator_time_tracking(tenant_id, operator_id);

ALTER TABLE operator_time_tracking ENABLE ROW LEVEL SECURITY;
ALTER TABLE operator_time_tracking FORCE ROW LEVEL SECURITY;

CREATE POLICY operator_time_tracking_tenant_isolation ON operator_time_tracking
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- 2. Material Consumption Records
CREATE TABLE IF NOT EXISTS production_material_consumption (
    consumption_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    work_order_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    stage_type VARCHAR(64) NOT NULL,
    material_code VARCHAR(64) NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    unit_of_measure VARCHAR(32) NOT NULL,
    planned_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    actual_quantity_consumed NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    scrap_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    variance_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    variance_percentage NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    batch_lot_number VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'RECORDED',
    recorded_by VARCHAR(64) NOT NULL,
    recorded_at BIGINT NOT NULL,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_mat_consumption_tenant_job ON production_material_consumption(tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_mat_consumption_tenant_wo ON production_material_consumption(tenant_id, work_order_id);

ALTER TABLE production_material_consumption ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_material_consumption FORCE ROW LEVEL SECURITY;

CREATE POLICY production_material_consumption_tenant_isolation ON production_material_consumption
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- 3. Machine Telemetry Logs
CREATE TABLE IF NOT EXISTS machine_telemetry_logs (
    log_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL,
    machine_name VARCHAR(128) NOT NULL,
    work_order_id VARCHAR(64),
    execution_job_id VARCHAR(64),
    recorded_speed_units_per_hour NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rated_speed_units_per_hour NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    speed_efficiency_percentage NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    total_impressions BIGINT NOT NULL DEFAULT 0,
    current_downtime_category VARCHAR(64),
    downtime_minutes INT NOT NULL DEFAULT 0,
    temperature_celsius NUMERIC(8, 4),
    is_running BOOLEAN NOT NULL DEFAULT TRUE,
    logged_at BIGINT NOT NULL,
    logged_by VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_telemetry_tenant_machine ON machine_telemetry_logs(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_tenant_job ON machine_telemetry_logs(tenant_id, execution_job_id);

ALTER TABLE machine_telemetry_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_telemetry_logs FORCE ROW LEVEL SECURITY;

CREATE POLICY machine_telemetry_logs_tenant_isolation ON machine_telemetry_logs
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- 4. Stage Output Handover Records
CREATE TABLE IF NOT EXISTS stage_output_handovers (
    handover_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    from_work_order_id VARCHAR(64) NOT NULL,
    from_stage VARCHAR(64) NOT NULL,
    to_work_order_id VARCHAR(64),
    to_stage VARCHAR(64),
    planned_output_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    actual_good_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    scrap_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    yield_percentage NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    handed_over_by VARCHAR(64) NOT NULL,
    handed_over_at BIGINT NOT NULL,
    accepted_by VARCHAR(64),
    accepted_at BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    discrepancy_notes TEXT,
    integrity_hash VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_stage_handovers_tenant_job ON stage_output_handovers(tenant_id, execution_job_id);

ALTER TABLE stage_output_handovers ENABLE ROW LEVEL SECURITY;
ALTER TABLE stage_output_handovers FORCE ROW LEVEL SECURITY;

CREATE POLICY stage_output_handovers_tenant_isolation ON stage_output_handovers
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- 5. Append-only Shop Floor Tracking Lifecycle Events
CREATE TABLE IF NOT EXISTS shop_floor_tracking_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    work_order_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    payload TEXT,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sf_events_tenant_job ON shop_floor_tracking_events(tenant_id, execution_job_id);

ALTER TABLE shop_floor_tracking_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_floor_tracking_events FORCE ROW LEVEL SECURITY;

CREATE POLICY shop_floor_tracking_events_tenant_isolation ON shop_floor_tracking_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
