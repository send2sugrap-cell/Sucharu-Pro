-- =============================================================================
-- Migration: V20261107__create_production_scheduling_tables.sql
-- Module 17 Step 06: Production Scheduling, Capacity Planning & Dispatch Orchestration Engine
-- =============================================================================

-- 1. Production Schedules
CREATE TABLE IF NOT EXISTS production_schedules (
    schedule_id VARCHAR(128) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(128) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'PROPOSED',
    planned_start_at BIGINT NOT NULL,
    planned_end_at BIGINT NOT NULL,
    total_setup_minutes INT NOT NULL DEFAULT 0,
    total_run_minutes INT NOT NULL DEFAULT 0,
    slots_json TEXT NOT NULL,
    capacity_windows_json TEXT NOT NULL,
    conflicts_json TEXT NOT NULL,
    schedule_fingerprint VARCHAR(128) NOT NULL,
    integrity_hash VARCHAR(128) NOT NULL,
    superseded_by_schedule_id VARCHAR(128),
    superseding_reason TEXT,
    approved_at BIGINT,
    approved_by VARCHAR(64),
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64),
    CONSTRAINT fk_ps_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_job FOREIGN KEY (execution_job_id) REFERENCES production_job_executions(execution_job_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ps_tenant_job ON production_schedules(tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_ps_tenant_order ON production_schedules(tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_ps_tenant_status ON production_schedules(tenant_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ps_tenant_idempotency ON production_schedules(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 2. Production Schedule Slots
CREATE TABLE IF NOT EXISTS production_schedule_slots (
    slot_id VARCHAR(128) PRIMARY KEY,
    schedule_id VARCHAR(128) NOT NULL,
    work_order_id VARCHAR(128) NOT NULL,
    execution_job_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    sequence_number INT NOT NULL,
    stage_type VARCHAR(64) NOT NULL,
    operation_code VARCHAR(64) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    machine_id VARCHAR(64) NOT NULL,
    machine_name VARCHAR(255) NOT NULL,
    operator_id VARCHAR(64),
    operator_name VARCHAR(255),
    scheduled_start_timestamp BIGINT NOT NULL,
    scheduled_end_timestamp BIGINT NOT NULL,
    setup_minutes INT NOT NULL DEFAULT 0,
    run_minutes INT NOT NULL DEFAULT 0,
    priority_score NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    notes TEXT,
    CONSTRAINT fk_pss_schedule FOREIGN KEY (schedule_id) REFERENCES production_schedules(schedule_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pss_schedule ON production_schedule_slots(schedule_id, sequence_number);
CREATE INDEX IF NOT EXISTS idx_pss_machine_time ON production_schedule_slots(machine_id, scheduled_start_timestamp);
CREATE INDEX IF NOT EXISTS idx_pss_tenant ON production_schedule_slots(tenant_id);

-- 3. Production Capacity Windows
CREATE TABLE IF NOT EXISTS production_capacity_windows (
    window_id VARCHAR(128) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL,
    machine_name VARCHAR(255) NOT NULL,
    shift_date VARCHAR(32) NOT NULL,
    shift_type VARCHAR(32) NOT NULL,
    start_timestamp BIGINT NOT NULL,
    end_timestamp BIGINT NOT NULL,
    total_capacity_minutes NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    allocated_minutes NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    available_minutes NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    utilization_rate NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    CONSTRAINT fk_pcw_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pcw_machine_date ON production_capacity_windows(tenant_id, machine_id, shift_date);

-- 4. Production Dispatch Queue
CREATE TABLE IF NOT EXISTS production_dispatch_queue (
    queue_item_id VARCHAR(128) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    schedule_id VARCHAR(128) NOT NULL,
    schedule_version INT NOT NULL DEFAULT 1,
    work_order_id VARCHAR(128) NOT NULL,
    execution_job_id VARCHAR(128) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    sequence_number INT NOT NULL,
    stage_type VARCHAR(64) NOT NULL,
    operation_code VARCHAR(64) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    target_work_center VARCHAR(128) NOT NULL,
    machine_id VARCHAR(64) NOT NULL,
    machine_name VARCHAR(255) NOT NULL,
    operator_id VARCHAR(64),
    operator_name VARCHAR(255),
    dispatch_status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    priority_score NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    planned_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    estimated_setup_minutes INT NOT NULL DEFAULT 0,
    estimated_run_minutes INT NOT NULL DEFAULT 0,
    scheduled_start_timestamp BIGINT NOT NULL,
    scheduled_end_timestamp BIGINT NOT NULL,
    queued_at BIGINT NOT NULL,
    ready_at BIGINT,
    dispatched_at BIGINT,
    acknowledged_at BIGINT,
    completed_at BIGINT,
    notes TEXT,
    CONSTRAINT fk_pdq_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_pdq_schedule FOREIGN KEY (schedule_id) REFERENCES production_schedules(schedule_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pdq_tenant_status ON production_dispatch_queue(tenant_id, dispatch_status);
CREATE INDEX IF NOT EXISTS idx_pdq_machine ON production_dispatch_queue(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_pdq_schedule ON production_dispatch_queue(schedule_id);

-- 5. Production Schedule Conflicts
CREATE TABLE IF NOT EXISTS production_schedule_conflicts (
    conflict_id VARCHAR(128) PRIMARY KEY,
    schedule_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    conflict_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'WARNING',
    work_order_id VARCHAR(128),
    machine_id VARCHAR(64),
    operator_id VARCHAR(64),
    message TEXT NOT NULL,
    is_blocking BOOLEAN NOT NULL DEFAULT FALSE,
    recommended_action TEXT NOT NULL,
    CONSTRAINT fk_psc_schedule FOREIGN KEY (schedule_id) REFERENCES production_schedules(schedule_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_psc_schedule ON production_schedule_conflicts(schedule_id);

-- 6. Production Schedule Events (Append-Only Audit)
CREATE TABLE IF NOT EXISTS production_schedule_events (
    event_id VARCHAR(128) PRIMARY KEY,
    schedule_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    payload TEXT,
    performed_by VARCHAR(64) NOT NULL,
    performed_at BIGINT NOT NULL,
    CONSTRAINT fk_pse_schedule FOREIGN KEY (schedule_id) REFERENCES production_schedules(schedule_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pse_schedule ON production_schedule_events(schedule_id, performed_at);

-- Row Level Security (RLS) Policies
ALTER TABLE production_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_schedules FORCE ROW LEVEL SECURITY;
CREATE POLICY ps_tenant_isolation ON production_schedules
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_schedule_slots ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_schedule_slots FORCE ROW LEVEL SECURITY;
CREATE POLICY pss_tenant_isolation ON production_schedule_slots
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_capacity_windows ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_capacity_windows FORCE ROW LEVEL SECURITY;
CREATE POLICY pcw_tenant_isolation ON production_capacity_windows
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_dispatch_queue ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_dispatch_queue FORCE ROW LEVEL SECURITY;
CREATE POLICY pdq_tenant_isolation ON production_dispatch_queue
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_schedule_conflicts ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_schedule_conflicts FORCE ROW LEVEL SECURITY;
CREATE POLICY psc_tenant_isolation ON production_schedule_conflicts
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE production_schedule_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_schedule_events FORCE ROW LEVEL SECURITY;
CREATE POLICY pse_tenant_isolation ON production_schedule_events
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true));
