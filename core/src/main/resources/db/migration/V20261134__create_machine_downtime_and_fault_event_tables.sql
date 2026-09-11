-- Module 21 Step 06: Downtime & Fault Event Management
-- Establishes canonical fault events, fault resolution tracking, and downtime event logging

-- 1. Machine Fault Events
CREATE TABLE IF NOT EXISTS machine_fault_events (
    fault_event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL REFERENCES machine_registry(machine_id) ON DELETE CASCADE,
    fault_code VARCHAR(64),
    fault_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'FAULT',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    description TEXT NOT NULL,
    source VARCHAR(64) NOT NULL DEFAULT 'MANUAL',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by VARCHAR(64),
    resolved_at TIMESTAMPTZ,
    resolved_by VARCHAR(64),
    resolution_notes TEXT,
    maintenance_record_id VARCHAR(64) REFERENCES machine_maintenance_records(record_id) ON DELETE SET NULL,
    metadata_json TEXT,
    correlation_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_fault_events_tenant_machine ON machine_fault_events(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_fault_events_tenant_status ON machine_fault_events(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_fault_events_tenant_occurred ON machine_fault_events(tenant_id, occurred_at DESC);

ALTER TABLE machine_fault_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_fault_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_fault_events_tenant_isolation ON machine_fault_events;
CREATE POLICY machine_fault_events_tenant_isolation ON machine_fault_events
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );

-- 2. Machine Downtime Events
CREATE TABLE IF NOT EXISTS machine_downtime_events (
    downtime_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL REFERENCES machine_registry(machine_id) ON DELETE CASCADE,
    fault_event_id VARCHAR(64) REFERENCES machine_fault_events(fault_event_id) ON DELETE SET NULL,
    execution_job_id VARCHAR(64),
    work_order_id VARCHAR(64),
    reason_category VARCHAR(64) NOT NULL,
    reason_details TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'STARTED',
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ,
    duration_seconds BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_downtime_tenant_machine ON machine_downtime_events(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_downtime_tenant_status ON machine_downtime_events(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_downtime_tenant_started ON machine_downtime_events(tenant_id, started_at DESC);

ALTER TABLE machine_downtime_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_downtime_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_downtime_events_tenant_isolation ON machine_downtime_events;
CREATE POLICY machine_downtime_events_tenant_isolation ON machine_downtime_events
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );
