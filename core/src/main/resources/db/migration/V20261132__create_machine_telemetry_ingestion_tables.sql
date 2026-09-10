-- Module 21 Step 02: Machine Telemetry Ingestion Foundation
-- Establishes canonical telemetry records, metric measurements, and timestamp boundaries

CREATE TABLE IF NOT EXISTS machine_telemetry_records (
    telemetry_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    machine_id VARCHAR(64) NOT NULL REFERENCES machine_registry(machine_id) ON DELETE CASCADE,
    source_device_id VARCHAR(128),
    metric_type VARCHAR(64) NOT NULL,
    metric_value NUMERIC(18, 4) NOT NULL,
    unit VARCHAR(32),
    event_timestamp TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata_json TEXT,
    idempotency_key VARCHAR(128),
    created_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_telemetry_records_tenant_machine ON machine_telemetry_records(tenant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_records_tenant_event ON machine_telemetry_records(tenant_id, event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_telemetry_records_tenant_idempotency ON machine_telemetry_records(tenant_id, idempotency_key);

-- Row-Level Security (RLS) Policies
ALTER TABLE machine_telemetry_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_telemetry_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_telemetry_records_tenant_isolation ON machine_telemetry_records;
CREATE POLICY machine_telemetry_records_tenant_isolation ON machine_telemetry_records
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );
