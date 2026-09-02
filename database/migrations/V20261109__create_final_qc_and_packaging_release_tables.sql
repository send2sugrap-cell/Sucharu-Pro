-- =========================================================================
-- SUCHARU PRO ERP — MODULE 17 STEP 08: FINAL QUALITY CONTROL & PACKAGING RELEASE
-- Migration: V20261109__create_final_qc_and_packaging_release_tables.sql
-- =========================================================================

-- 1. Final QC Inspections Table
CREATE TABLE IF NOT EXISTS production_final_qc_inspections (
    inspection_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    sample_plan_type VARCHAR(64) NOT NULL,
    total_lot_quantity NUMERIC(18, 4) NOT NULL,
    sample_size NUMERIC(18, 4) NOT NULL,
    accepted_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rejected_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rework_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(64) NOT NULL,
    checklist_json TEXT NOT NULL DEFAULT '[]',
    inspector_id VARCHAR(64) NOT NULL,
    inspector_name VARCHAR(128) NOT NULL,
    inspection_notes TEXT,
    inspected_at BIGINT NOT NULL,
    completed_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_final_qc_job ON production_final_qc_inspections(tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_final_qc_order ON production_final_qc_inspections(tenant_id, order_id);

ALTER TABLE production_final_qc_inspections ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_final_qc_inspections FORCE ROW LEVEL SECURITY;

CREATE POLICY final_qc_tenant_isolation_policy ON production_final_qc_inspections
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 2. Defect Containment & Quarantine Records Table
CREATE TABLE IF NOT EXISTS production_defect_containments (
    containment_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(64) NOT NULL,
    root_cause_stage VARCHAR(64) NOT NULL,
    defect_type VARCHAR(64) NOT NULL,
    severity VARCHAR(64) NOT NULL,
    defect_quantity NUMERIC(18, 4) NOT NULL,
    disposition VARCHAR(64) NOT NULL,
    quarantine_location VARCHAR(128) NOT NULL,
    rework_work_order_id VARCHAR(64),
    root_cause_details TEXT NOT NULL,
    logged_by VARCHAR(64) NOT NULL,
    logged_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_defect_containment_job ON production_defect_containments(tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_defect_containment_insp ON production_defect_containments(tenant_id, inspection_id);

ALTER TABLE production_defect_containments ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_defect_containments FORCE ROW LEVEL SECURITY;

CREATE POLICY defect_containment_tenant_isolation_policy ON production_defect_containments
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 3. Packaging Orchestration Records Table
CREATE TABLE IF NOT EXISTS production_packaging_records (
    packaging_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(64) NOT NULL,
    packaging_type VARCHAR(64) NOT NULL,
    units_per_package NUMERIC(18, 4) NOT NULL,
    total_package_count INT NOT NULL,
    total_packaged_quantity NUMERIC(18, 4) NOT NULL,
    pallet_identifier VARCHAR(64),
    carton_numbers_range VARCHAR(64),
    gross_weight_kg NUMERIC(18, 4),
    packaging_slip_barcode VARCHAR(128) NOT NULL,
    packaged_by VARCHAR(64) NOT NULL,
    packaged_at BIGINT NOT NULL,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_packaging_job ON production_packaging_records(tenant_id, execution_job_id);

ALTER TABLE production_packaging_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_packaging_records FORCE ROW LEVEL SECURITY;

CREATE POLICY packaging_tenant_isolation_policy ON production_packaging_records
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 4. Finished Goods Release Records Table
CREATE TABLE IF NOT EXISTS finished_goods_release_records (
    release_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(64) NOT NULL,
    packaging_id VARCHAR(64) NOT NULL,
    released_quantity NUMERIC(18, 4) NOT NULL,
    destination VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    authorized_by VARCHAR(64) NOT NULL,
    authorized_at BIGINT NOT NULL,
    integrity_hash VARCHAR(128) NOT NULL,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_fg_release_job ON finished_goods_release_records(tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_fg_release_order ON finished_goods_release_records(tenant_id, order_id);

ALTER TABLE finished_goods_release_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE finished_goods_release_records FORCE ROW LEVEL SECURITY;

CREATE POLICY fg_release_tenant_isolation_policy ON finished_goods_release_records
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));


-- 5. Final QC Packaging Events Table
CREATE TABLE IF NOT EXISTS final_qc_packaging_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_final_qc_events_job ON final_qc_packaging_events(tenant_id, execution_job_id);

ALTER TABLE final_qc_packaging_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE final_qc_packaging_events FORCE ROW LEVEL SECURITY;

CREATE POLICY final_qc_events_tenant_isolation_policy ON final_qc_packaging_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
