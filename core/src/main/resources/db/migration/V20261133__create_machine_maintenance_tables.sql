-- Module 21 Step 05: Machine Maintenance Management
-- Establishes machine maintenance schedules, corrective/preventive maintenance records, and service history logs

-- 1. Maintenance Schedules
CREATE TABLE IF NOT EXISTS machine_maintenance_schedules (
    schedule_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL REFERENCES machine_registry(machine_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    maintenance_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    planned_date TIMESTAMPTZ NOT NULL,
    recurrence_interval_days INT,
    assigned_technician_id VARCHAR(64),
    assigned_technician_name VARCHAR(128),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_maint_sched_tenant_machine ON machine_maintenance_schedules(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_maint_sched_tenant_status ON machine_maintenance_schedules(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_maint_sched_tenant_date ON machine_maintenance_schedules(tenant_id, planned_date);

ALTER TABLE machine_maintenance_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_maintenance_schedules FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_maintenance_schedules_tenant_isolation ON machine_maintenance_schedules;
CREATE POLICY machine_maintenance_schedules_tenant_isolation ON machine_maintenance_schedules
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );

-- 2. Maintenance Records
CREATE TABLE IF NOT EXISTS machine_maintenance_records (
    record_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL REFERENCES machine_registry(machine_id) ON DELETE CASCADE,
    schedule_id VARCHAR(64) REFERENCES machine_maintenance_schedules(schedule_id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    problem_description TEXT,
    work_performed TEXT,
    maintenance_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    performed_by_id VARCHAR(64),
    performed_by_name VARCHAR(128),
    resolution_summary TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_maint_rec_tenant_machine ON machine_maintenance_records(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_maint_rec_tenant_status ON machine_maintenance_records(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_maint_rec_tenant_type ON machine_maintenance_records(tenant_id, maintenance_type);

ALTER TABLE machine_maintenance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_maintenance_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_maintenance_records_tenant_isolation ON machine_maintenance_records;
CREATE POLICY machine_maintenance_records_tenant_isolation ON machine_maintenance_records
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );

-- 3. Machine Service History Logs
CREATE TABLE IF NOT EXISTS machine_service_history_logs (
    history_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL REFERENCES machine_registry(machine_id) ON DELETE CASCADE,
    record_id VARCHAR(64) NOT NULL REFERENCES machine_maintenance_records(record_id) ON DELETE CASCADE,
    action_type VARCHAR(64) NOT NULL,
    performed_by_id VARCHAR(64),
    performed_by_name VARCHAR(128),
    details_json TEXT,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_maint_hist_tenant_machine ON machine_service_history_logs(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_maint_hist_tenant_record ON machine_service_history_logs(tenant_id, record_id);

ALTER TABLE machine_service_history_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_service_history_logs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_service_history_tenant_isolation ON machine_service_history_logs;
CREATE POLICY machine_service_history_tenant_isolation ON machine_service_history_logs
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );
