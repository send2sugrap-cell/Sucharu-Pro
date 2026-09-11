-- Module 21 Step 07: Machine Alerts & Maintenance Notifications
-- Establishes canonical machine operational alerts table with RLS tenant isolation

CREATE TABLE IF NOT EXISTS machine_operational_alerts (
    alert_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL REFERENCES machine_registry(machine_id) ON DELETE CASCADE,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'WARNING',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    telemetry_record_id VARCHAR(64),
    fault_event_id VARCHAR(64) REFERENCES machine_fault_events(fault_event_id) ON DELETE SET NULL,
    maintenance_schedule_id VARCHAR(64) REFERENCES machine_maintenance_schedules(schedule_id) ON DELETE SET NULL,
    maintenance_record_id VARCHAR(64) REFERENCES machine_maintenance_records(record_id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    correlation_key VARCHAR(128),
    notification_id VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by VARCHAR(64),
    resolved_at TIMESTAMPTZ,
    resolved_by VARCHAR(64),
    resolution_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_machine_alerts_tenant_machine ON machine_operational_alerts(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_machine_alerts_tenant_status ON machine_operational_alerts(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_machine_alerts_tenant_correlation ON machine_operational_alerts(tenant_id, correlation_key);

ALTER TABLE machine_operational_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_operational_alerts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_operational_alerts_tenant_isolation ON machine_operational_alerts;
CREATE POLICY machine_operational_alerts_tenant_isolation ON machine_operational_alerts
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );
