-- =============================================================================
-- Migration: V20261114__create_imposition_layout_tables.sql
-- Module 18: Advanced Dynamic Imposition & Gang-Run Optimizer Engine
-- Step 01: Automated Sheet Layout & Single-Job Dynamic Imposition Engine
-- =============================================================================

CREATE TABLE IF NOT EXISTS imposition_specifications (
    imposition_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    calculation_id VARCHAR(64),
    product_name VARCHAR(255) NOT NULL,
    
    -- Dimensions in normalized mm
    item_width_mm NUMERIC(10, 4) NOT NULL,
    item_height_mm NUMERIC(10, 4) NOT NULL,
    sheet_width_mm NUMERIC(10, 4) NOT NULL,
    sheet_height_mm NUMERIC(10, 4) NOT NULL,
    usable_width_mm NUMERIC(10, 4) NOT NULL,
    usable_height_mm NUMERIC(10, 4) NOT NULL,
    
    -- Margins & Prepress Spacing
    margin_top_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_bottom_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_left_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_right_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    bleed_mm NUMERIC(10, 4) NOT NULL DEFAULT 3.0000,
    horizontal_gutter_mm NUMERIC(10, 4) NOT NULL DEFAULT 4.0000,
    vertical_gutter_mm NUMERIC(10, 4) NOT NULL DEFAULT 4.0000,
    
    -- Optimization Outcome
    orientation_policy VARCHAR(32) NOT NULL DEFAULT 'AUTO_OPTIMAL',
    selected_orientation VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    columns_count INT NOT NULL,
    rows_count INT NOT NULL,
    copies_per_sheet INT NOT NULL,
    
    -- Quantities & Sheet Demands
    required_quantity BIGINT NOT NULL,
    required_sheets BIGINT NOT NULL,
    total_produced_capacity BIGINT NOT NULL,
    overage_quantity BIGINT NOT NULL DEFAULT 0,
    
    -- Efficiency & Areas
    occupied_area_mm2 NUMERIC(14, 4) NOT NULL,
    usable_area_mm2 NUMERIC(14, 4) NOT NULL,
    waste_area_mm2 NUMERIC(14, 4) NOT NULL,
    yield_percentage NUMERIC(8, 4) NOT NULL,
    
    -- Metadata & Governance
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'OPTIMIZED',
    integrity_hash VARCHAR(128) NOT NULL,
    notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_imp_specs_tenant_job ON imposition_specifications (tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_imp_specs_tenant_order ON imposition_specifications (tenant_id, order_id, order_item_id);
CREATE INDEX IF NOT EXISTS idx_imp_specs_tenant_status ON imposition_specifications (tenant_id, status);

ALTER TABLE imposition_specifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE imposition_specifications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS imposition_specifications_tenant_isolation ON imposition_specifications;
CREATE POLICY imposition_specifications_tenant_isolation ON imposition_specifications
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- Imposition Audit Events Table
CREATE TABLE IF NOT EXISTS imposition_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    imposition_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    details TEXT,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_imp_audit_spec FOREIGN KEY (imposition_id) REFERENCES imposition_specifications (imposition_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_imp_audit_tenant_spec ON imposition_audit_events (tenant_id, imposition_id);

ALTER TABLE imposition_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE imposition_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS imposition_audit_events_tenant_isolation ON imposition_audit_events;
CREATE POLICY imposition_audit_events_tenant_isolation ON imposition_audit_events
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
