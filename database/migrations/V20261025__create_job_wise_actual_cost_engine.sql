-- ============================================================================
-- SUCHARU PRO ERP - MODULE 16 STEP 02: JOB-WISE ACTUAL COST ENGINE
-- Flyway Migration: V20261025__create_job_wise_actual_cost_engine.sql
-- ============================================================================

-- 1. Job Actual Cost Snapshots Table
CREATE TABLE IF NOT EXISTS job_cost_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    job_number VARCHAR(64),
    customer_id VARCHAR(64),
    product_id VARCHAR(64),
    job_quantity INT NOT NULL DEFAULT 0,
    calculation_version VARCHAR(32) NOT NULL DEFAULT 'JOB_COST_ENGINE_V1',
    calculation_timestamp BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    total_actual_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    total_direct_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    total_indirect_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    estimated_cost DECIMAL(18, 4),
    cost_variance DECIMAL(18, 4),
    cost_variance_percentage DECIMAL(18, 4),
    variance_classification VARCHAR(32) NOT NULL DEFAULT 'BASELINE_UNAVAILABLE',
    readiness_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETE',
    is_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    source_count INT NOT NULL DEFAULT 0,
    duplicate_source_count INT NOT NULL DEFAULT 0,
    unresolved_source_count INT NOT NULL DEFAULT 0,
    warnings_json TEXT DEFAULT '[]',
    integrity_hash VARCHAR(128) NOT NULL,
    generated_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_cost_snap_tenant_proj ON job_cost_snapshots(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_job_cost_snap_job ON job_cost_snapshots(tenant_id, project_id, job_id);
CREATE INDEX IF NOT EXISTS idx_job_cost_snap_time ON job_cost_snapshots(calculation_timestamp DESC);

ALTER TABLE job_cost_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_cost_snapshots FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS job_cost_snapshots_tenant_isolation ON job_cost_snapshots;
CREATE POLICY job_cost_snapshots_tenant_isolation ON job_cost_snapshots
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 2. Job Cost Components Breakdown Table
CREATE TABLE IF NOT EXISTS job_cost_components (
    component_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL REFERENCES job_cost_snapshots(snapshot_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    directness VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
    quantity DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    unit_rate DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    original_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    attributed_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    percentage_of_total_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    attribution_basis VARCHAR(128) NOT NULL DEFAULT 'CANONICAL',
    source_item_count INT NOT NULL DEFAULT 1,
    calculation_explanation TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_cost_comp_snapshot ON job_cost_components(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_job_cost_comp_tenant_job ON job_cost_components(tenant_id, project_id, job_id);

ALTER TABLE job_cost_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_cost_components FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS job_cost_components_tenant_isolation ON job_cost_components;
CREATE POLICY job_cost_components_tenant_isolation ON job_cost_components
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 3. Job Cost Provenance Records Table
CREATE TABLE IF NOT EXISTS job_cost_provenance_records (
    provenance_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL REFERENCES job_cost_snapshots(snapshot_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    source_module VARCHAR(64) NOT NULL,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(64) NOT NULL,
    source_transaction_id VARCHAR(64),
    source_reference VARCHAR(128),
    vendor_id VARCHAR(64),
    operation_id VARCHAR(64),
    inventory_movement_id VARCHAR(64),
    expense_id VARCHAR(64),
    payable_id VARCHAR(64),
    qc_cost_id VARCHAR(64),
    rework_id VARCHAR(64),
    cost_component_type VARCHAR(64) NOT NULL,
    directness VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
    original_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    attributed_amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    attribution_basis VARCHAR(128) NOT NULL,
    calculation_explanation TEXT,
    fingerprint_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_cost_prov_snapshot ON job_cost_provenance_records(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_job_cost_prov_tenant_job ON job_cost_provenance_records(tenant_id, project_id, job_id);
CREATE INDEX IF NOT EXISTS idx_job_cost_prov_fingerprint ON job_cost_provenance_records(fingerprint_hash);

ALTER TABLE job_cost_provenance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_cost_provenance_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS job_cost_provenance_tenant_isolation ON job_cost_provenance_records;
CREATE POLICY job_cost_provenance_tenant_isolation ON job_cost_provenance_records
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 4. Job Cost Reconciliation Events Table
CREATE TABLE IF NOT EXISTS job_cost_reconciliation_events (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    is_reconciled BOOLEAN NOT NULL DEFAULT TRUE,
    component_total_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    snapshot_total_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    provenance_total_cost DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    component_difference DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    provenance_difference DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    duplicate_count INT NOT NULL DEFAULT 0,
    missing_source_count INT NOT NULL DEFAULT 0,
    discrepancies_json TEXT DEFAULT '[]',
    checked_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    checked_at BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_cost_recon_tenant_proj ON job_cost_reconciliation_events(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_job_cost_recon_job ON job_cost_reconciliation_events(tenant_id, project_id, job_id);

ALTER TABLE job_cost_reconciliation_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_cost_reconciliation_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS job_cost_recon_tenant_isolation ON job_cost_reconciliation_events;
CREATE POLICY job_cost_recon_tenant_isolation ON job_cost_reconciliation_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 5. Job Cost Audit Events Table
CREATE TABLE IF NOT EXISTS job_cost_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL DEFAULT 'STAFF',
    outcome VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    details TEXT,
    correlation_id VARCHAR(64),
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_cost_audit_tenant_proj ON job_cost_audit_events(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_job_cost_audit_job ON job_cost_audit_events(tenant_id, project_id, job_id);

ALTER TABLE job_cost_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_cost_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS job_cost_audit_tenant_isolation ON job_cost_audit_events;
CREATE POLICY job_cost_audit_tenant_isolation ON job_cost_audit_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
