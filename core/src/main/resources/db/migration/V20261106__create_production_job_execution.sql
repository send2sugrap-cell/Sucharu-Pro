-- =============================================================================
-- Migration: V20261106__create_production_job_execution.sql
-- Module 17 Step 05: Production Job Creation, Work Order Execution & Shop-Floor Operations Engine
-- =============================================================================

-- 1. Production Job Executions
CREATE TABLE IF NOT EXISTS production_job_executions (
    execution_job_id VARCHAR(128) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    quotation_id VARCHAR(64),
    quotation_version_number INT,
    commercial_commitment_id VARCHAR(64),
    planning_id VARCHAR(128) NOT NULL,
    planning_version INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(32) NOT NULL DEFAULT 'READY',
    specification_json TEXT NOT NULL,
    planned_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    started_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    completed_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rejected_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    wastage_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rework_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    remaining_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    current_hold_json TEXT,
    current_stage_type VARCHAR(64),
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at BIGINT,
    completion_summary TEXT,
    job_fingerprint VARCHAR(128) NOT NULL,
    integrity_hash VARCHAR(128) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64),
    CONSTRAINT fk_pje_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pje_tenant_order ON production_job_executions(tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_pje_tenant_status ON production_job_executions(tenant_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pje_tenant_idempotency ON production_job_executions(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 2. Production Work Orders
CREATE TABLE IF NOT EXISTS production_work_orders (
    work_order_id VARCHAR(128) PRIMARY KEY,
    execution_job_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    sequence_number INT NOT NULL,
    stage_type VARCHAR(64) NOT NULL,
    operation_code VARCHAR(64) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    target_work_center VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    assigned_machine_id VARCHAR(64),
    assigned_machine_name VARCHAR(255),
    assigned_operator_id VARCHAR(64),
    assigned_operator_name VARCHAR(255),
    estimated_setup_minutes INT NOT NULL DEFAULT 0,
    estimated_run_minutes INT NOT NULL DEFAULT 0,
    actual_setup_minutes INT NOT NULL DEFAULT 0,
    actual_run_minutes INT NOT NULL DEFAULT 0,
    planned_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    completed_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rejected_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    wastage_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    is_qc_checkpoint BOOLEAN NOT NULL DEFAULT FALSE,
    predecessors_json TEXT NOT NULL DEFAULT '[]',
    started_at BIGINT,
    paused_at BIGINT,
    completed_at BIGINT,
    notes TEXT,
    CONSTRAINT fk_pwo_job FOREIGN KEY (execution_job_id) REFERENCES production_job_executions(execution_job_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pwo_job ON production_work_orders(execution_job_id, sequence_number);
CREATE INDEX IF NOT EXISTS idx_pwo_tenant ON production_work_orders(tenant_id, status);

-- 3. Production Execution Actuals
CREATE TABLE IF NOT EXISTS production_execution_actuals (
    actual_id VARCHAR(128) PRIMARY KEY,
    execution_job_id VARCHAR(128) NOT NULL,
    work_order_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    stage_type VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64),
    operator_id VARCHAR(64),
    started_at BIGINT NOT NULL,
    completed_at BIGINT,
    duration_seconds BIGINT,
    good_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    scrap_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rework_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    remarks TEXT,
    CONSTRAINT fk_pea_job FOREIGN KEY (execution_job_id) REFERENCES production_job_executions(execution_job_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pea_job ON production_execution_actuals(execution_job_id);

-- 4. Production Holds
CREATE TABLE IF NOT EXISTS production_execution_holds (
    hold_id VARCHAR(128) PRIMARY KEY,
    execution_job_id VARCHAR(128) NOT NULL,
    work_order_id VARCHAR(128),
    tenant_id VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    held_at BIGINT NOT NULL,
    held_by VARCHAR(64) NOT NULL,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at BIGINT,
    resolved_by VARCHAR(64),
    resolution_notes TEXT,
    CONSTRAINT fk_peh_job FOREIGN KEY (execution_job_id) REFERENCES production_job_executions(execution_job_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_peh_job ON production_execution_holds(execution_job_id);

-- 5. Production Wastages
CREATE TABLE IF NOT EXISTS production_execution_wastages (
    wastage_id VARCHAR(128) PRIMARY KEY,
    execution_job_id VARCHAR(128) NOT NULL,
    work_order_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    material_code VARCHAR(64) NOT NULL,
    quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'SHEETS',
    reason TEXT NOT NULL,
    stage_type VARCHAR(64) NOT NULL,
    recorded_by VARCHAR(64) NOT NULL,
    recorded_at BIGINT NOT NULL,
    CONSTRAINT fk_pew_job FOREIGN KEY (execution_job_id) REFERENCES production_job_executions(execution_job_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pew_job ON production_execution_wastages(execution_job_id);

-- 6. Production Reworks
CREATE TABLE IF NOT EXISTS production_execution_reworks (
    rework_id VARCHAR(128) PRIMARY KEY,
    execution_job_id VARCHAR(128) NOT NULL,
    source_work_order_id VARCHAR(128) NOT NULL,
    target_work_order_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    defect_code VARCHAR(64),
    reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    requested_by VARCHAR(64) NOT NULL,
    requested_at BIGINT NOT NULL,
    resolved_at BIGINT,
    CONSTRAINT fk_per_job FOREIGN KEY (execution_job_id) REFERENCES production_job_executions(execution_job_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_per_job ON production_execution_reworks(execution_job_id);

-- 7. Production Execution Events (Append-Only Audit)
CREATE TABLE IF NOT EXISTS production_execution_events (
    event_id VARCHAR(128) PRIMARY KEY,
    execution_job_id VARCHAR(128) NOT NULL,
    work_order_id VARCHAR(128),
    tenant_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    payload TEXT,
    performed_by VARCHAR(64) NOT NULL,
    performed_at BIGINT NOT NULL,
    CONSTRAINT fk_pee_job FOREIGN KEY (execution_job_id) REFERENCES production_job_executions(execution_job_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pee_job ON production_execution_events(execution_job_id, performed_at);

-- Row Level Security (RLS) Policies
ALTER TABLE production_job_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_job_executions FORCE ROW LEVEL SECURITY;
CREATE POLICY pje_tenant_isolation ON production_job_executions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_work_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_work_orders FORCE ROW LEVEL SECURITY;
CREATE POLICY pwo_tenant_isolation ON production_work_orders
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_execution_actuals ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_execution_actuals FORCE ROW LEVEL SECURITY;
CREATE POLICY pea_tenant_isolation ON production_execution_actuals
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_execution_holds ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_execution_holds FORCE ROW LEVEL SECURITY;
CREATE POLICY peh_tenant_isolation ON production_execution_holds
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_execution_wastages ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_execution_wastages FORCE ROW LEVEL SECURITY;
CREATE POLICY pew_tenant_isolation ON production_execution_wastages
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_execution_reworks ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_execution_reworks FORCE ROW LEVEL SECURITY;
CREATE POLICY per_tenant_isolation ON production_execution_reworks
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_execution_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_execution_events FORCE ROW LEVEL SECURITY;
CREATE POLICY pee_tenant_isolation ON production_execution_events
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));
