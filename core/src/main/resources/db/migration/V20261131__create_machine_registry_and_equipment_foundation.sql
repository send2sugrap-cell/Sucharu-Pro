-- Module 21 Step 01: Machine Registry & Equipment Foundation
-- Establishes master machine identity, classification, location, ownership, and static configuration

CREATE TABLE IF NOT EXISTS machine_registry (
    machine_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    asset_code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    machine_type VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'PRODUCTION',
    manufacturer VARCHAR(128),
    model VARCHAR(128),
    serial_number VARCHAR(128),
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ownership_type VARCHAR(32) NOT NULL DEFAULT 'COMPANY_OWNED',
    location_reference VARCHAR(255),
    department VARCHAR(128),
    configuration_metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    CONSTRAINT uq_machine_tenant_asset_code UNIQUE (tenant_id, asset_code)
);

CREATE INDEX IF NOT EXISTS idx_machine_registry_tenant ON machine_registry(tenant_id);
CREATE INDEX IF NOT EXISTS idx_machine_registry_type ON machine_registry(tenant_id, machine_type);
CREATE INDEX IF NOT EXISTS idx_machine_registry_status ON machine_registry(tenant_id, status);

-- Row-Level Security (RLS) Policies
ALTER TABLE machine_registry ENABLE ROW LEVEL SECURITY;
ALTER TABLE machine_registry FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS machine_registry_tenant_isolation ON machine_registry;
CREATE POLICY machine_registry_tenant_isolation ON machine_registry
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );
