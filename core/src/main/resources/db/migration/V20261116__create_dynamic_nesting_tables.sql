-- =============================================================================
-- Migration: V20261116__create_dynamic_nesting_tables.sql
-- Module 18: Advanced Dynamic Imposition & Gang-Run Optimizer Engine
-- Step 03: Dynamic Nesting, Sheet Utilization & Wastage Minimization
-- =============================================================================

CREATE TABLE IF NOT EXISTS dynamic_nesting_specifications (
    nesting_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    
    -- Substrate & Process Criteria
    paper_stock_type VARCHAR(64) NOT NULL,
    gsm NUMERIC(10, 4) NOT NULL,
    color_mode VARCHAR(64) NOT NULL,
    printing_side_option VARCHAR(64) NOT NULL,
    
    -- Sheet Dimensions in normalized mm
    sheet_width_mm NUMERIC(10, 4) NOT NULL,
    sheet_height_mm NUMERIC(10, 4) NOT NULL,
    
    -- Margins & Prepress Spacing
    margin_top_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_bottom_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_left_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_right_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    bleed_mm NUMERIC(10, 4) NOT NULL DEFAULT 3.0000,
    horizontal_gutter_mm NUMERIC(10, 4) NOT NULL DEFAULT 4.0000,
    vertical_gutter_mm NUMERIC(10, 4) NOT NULL DEFAULT 4.0000,
    
    -- Strategy
    orientation_policy VARCHAR(64) NOT NULL DEFAULT 'ALLOW_ROTATION',
    placement_strategy VARCHAR(64) NOT NULL DEFAULT 'BOTTOM_LEFT_FILL',
    
    -- Usable Canvas
    usable_width_mm NUMERIC(10, 4) NOT NULL,
    usable_height_mm NUMERIC(10, 4) NOT NULL,
    
    -- Slot and Sheet Metrics
    total_items_placed INT NOT NULL,
    common_required_sheets BIGINT NOT NULL,
    total_produced_items BIGINT NOT NULL,
    total_overage_items BIGINT NOT NULL DEFAULT 0,
    
    -- Efficiency & Area Metrics
    total_sheet_area_mm2 NUMERIC(14, 4) NOT NULL,
    usable_area_mm2 NUMERIC(14, 4) NOT NULL,
    occupied_area_mm2 NUMERIC(14, 4) NOT NULL,
    waste_area_mm2 NUMERIC(14, 4) NOT NULL,
    recoverable_offcut_area_mm2 NUMERIC(14, 4) NOT NULL DEFAULT 0.0000,
    sheet_utilization_percentage NUMERIC(8, 4) NOT NULL,
    usable_yield_percentage NUMERIC(8, 4) NOT NULL,
    offcut_recovery_percentage NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    
    -- Governance & Metadata
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'OPTIMIZED',
    integrity_hash VARCHAR(128) NOT NULL,
    notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dyn_nest_tenant_status ON dynamic_nesting_specifications (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_dyn_nest_tenant_stock ON dynamic_nesting_specifications (tenant_id, paper_stock_type, gsm);

ALTER TABLE dynamic_nesting_specifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE dynamic_nesting_specifications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS dynamic_nesting_specifications_tenant_isolation ON dynamic_nesting_specifications;
CREATE POLICY dynamic_nesting_specifications_tenant_isolation ON dynamic_nesting_specifications
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- Dynamic Nesting Placements Table
CREATE TABLE IF NOT EXISTS dynamic_nesting_placements (
    placement_id VARCHAR(64) PRIMARY KEY,
    nesting_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    slot_index INT NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    
    x_mm NUMERIC(10, 4) NOT NULL,
    y_mm NUMERIC(10, 4) NOT NULL,
    placed_width_mm NUMERIC(10, 4) NOT NULL,
    placed_height_mm NUMERIC(10, 4) NOT NULL,
    orientation VARCHAR(32) NOT NULL,
    occupied_area_mm2 NUMERIC(14, 4) NOT NULL,
    
    CONSTRAINT fk_nest_placement_spec FOREIGN KEY (nesting_id) REFERENCES dynamic_nesting_specifications (nesting_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nest_place_tenant_job ON dynamic_nesting_placements (tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_nest_place_tenant_spec ON dynamic_nesting_placements (tenant_id, nesting_id);

ALTER TABLE dynamic_nesting_placements ENABLE ROW LEVEL SECURITY;
ALTER TABLE dynamic_nesting_placements FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS dynamic_nesting_placements_tenant_isolation ON dynamic_nesting_placements;
CREATE POLICY dynamic_nesting_placements_tenant_isolation ON dynamic_nesting_placements
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- Dynamic Nesting Offcut Remnants Table
CREATE TABLE IF NOT EXISTS dynamic_nesting_offcuts (
    offcut_id VARCHAR(64) PRIMARY KEY,
    nesting_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    
    x_mm NUMERIC(10, 4) NOT NULL,
    y_mm NUMERIC(10, 4) NOT NULL,
    width_mm NUMERIC(10, 4) NOT NULL,
    height_mm NUMERIC(10, 4) NOT NULL,
    area_mm2 NUMERIC(14, 4) NOT NULL,
    is_recoverable BOOLEAN NOT NULL DEFAULT false,
    
    CONSTRAINT fk_nest_offcut_spec FOREIGN KEY (nesting_id) REFERENCES dynamic_nesting_specifications (nesting_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nest_offcut_tenant_spec ON dynamic_nesting_offcuts (tenant_id, nesting_id);

ALTER TABLE dynamic_nesting_offcuts ENABLE ROW LEVEL SECURITY;
ALTER TABLE dynamic_nesting_offcuts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS dynamic_nesting_offcuts_tenant_isolation ON dynamic_nesting_offcuts;
CREATE POLICY dynamic_nesting_offcuts_tenant_isolation ON dynamic_nesting_offcuts
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- Dynamic Nesting Audit Events Table
CREATE TABLE IF NOT EXISTS dynamic_nesting_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    nesting_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    details TEXT,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_nest_audit_spec FOREIGN KEY (nesting_id) REFERENCES dynamic_nesting_specifications (nesting_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nest_audit_tenant_spec ON dynamic_nesting_audit_events (tenant_id, nesting_id);

ALTER TABLE dynamic_nesting_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE dynamic_nesting_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS dynamic_nesting_audit_events_tenant_isolation ON dynamic_nesting_audit_events;
CREATE POLICY dynamic_nesting_audit_events_tenant_isolation ON dynamic_nesting_audit_events
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
