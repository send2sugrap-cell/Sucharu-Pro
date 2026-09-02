-- =============================================================================
-- Migration: V20261115__create_gang_run_batch_tables.sql
-- Module 18: Advanced Dynamic Imposition & Gang-Run Optimizer Engine
-- Step 02: Multi-Job Gang-Run Batching & Compatibility Clustering
-- =============================================================================

CREATE TABLE IF NOT EXISTS gang_run_specifications (
    gang_run_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    batch_name VARCHAR(255) NOT NULL,
    
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
    
    -- Slot and Sheet Metrics
    total_available_slots INT NOT NULL,
    allocated_slots_count INT NOT NULL,
    common_required_sheets BIGINT NOT NULL,
    total_produced_items BIGINT NOT NULL,
    total_overage_items BIGINT NOT NULL DEFAULT 0,
    
    -- Efficiency & Area Metrics
    usable_area_mm2 NUMERIC(14, 4) NOT NULL,
    occupied_area_mm2 NUMERIC(14, 4) NOT NULL,
    waste_area_mm2 NUMERIC(14, 4) NOT NULL,
    sheet_yield_percentage NUMERIC(8, 4) NOT NULL,
    
    -- Governance & Metadata
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'OPTIMIZED',
    integrity_hash VARCHAR(128) NOT NULL,
    notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gang_run_tenant_status ON gang_run_specifications (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_gang_run_tenant_stock ON gang_run_specifications (tenant_id, paper_stock_type, gsm);

ALTER TABLE gang_run_specifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE gang_run_specifications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS gang_run_specifications_tenant_isolation ON gang_run_specifications;
CREATE POLICY gang_run_specifications_tenant_isolation ON gang_run_specifications
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- Gang-Run Item Allocation Table (Line-item UP allocations)
CREATE TABLE IF NOT EXISTS gang_run_item_allocations (
    allocation_id VARCHAR(64) PRIMARY KEY,
    gang_run_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    
    assigned_slots INT NOT NULL,
    orientation VARCHAR(32) NOT NULL,
    slot_item_width_mm NUMERIC(10, 4) NOT NULL,
    slot_item_height_mm NUMERIC(10, 4) NOT NULL,
    
    required_quantity BIGINT NOT NULL,
    produced_quantity BIGINT NOT NULL,
    overage_quantity BIGINT NOT NULL DEFAULT 0,
    item_occupied_area_mm2 NUMERIC(14, 4) NOT NULL,
    relative_yield_percentage NUMERIC(8, 4) NOT NULL,
    
    CONSTRAINT fk_gang_alloc_spec FOREIGN KEY (gang_run_id) REFERENCES gang_run_specifications (gang_run_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gang_alloc_tenant_job ON gang_run_item_allocations (tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_gang_alloc_tenant_spec ON gang_run_item_allocations (tenant_id, gang_run_id);

ALTER TABLE gang_run_item_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE gang_run_item_allocations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS gang_run_item_allocations_tenant_isolation ON gang_run_item_allocations;
CREATE POLICY gang_run_item_allocations_tenant_isolation ON gang_run_item_allocations
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

-- Gang-Run Audit Events Table
CREATE TABLE IF NOT EXISTS gang_run_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    gang_run_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    details TEXT,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_gang_audit_spec FOREIGN KEY (gang_run_id) REFERENCES gang_run_specifications (gang_run_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gang_audit_tenant_spec ON gang_run_audit_events (tenant_id, gang_run_id);

ALTER TABLE gang_run_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE gang_run_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS gang_run_audit_events_tenant_isolation ON gang_run_audit_events;
CREATE POLICY gang_run_audit_events_tenant_isolation ON gang_run_audit_events
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
