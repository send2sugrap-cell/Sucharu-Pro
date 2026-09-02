-- Module 19: Substrate Stock Auto-Reservation
-- Step 03: Batch/Lot Selection, Grain Direction & Sheet Dimension Matching Persistence Tables

CREATE TABLE IF NOT EXISTS substrate_batch_selection_records (
    selection_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64),
    work_order_id VARCHAR(64),
    reservation_id VARCHAR(64),
    product_id VARCHAR(64) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    requested_material_name VARCHAR(255) NOT NULL,
    stock_type VARCHAR(64) NOT NULL,
    target_gsm NUMERIC(10, 4) NOT NULL,
    required_sheet_width_mm NUMERIC(10, 4) NOT NULL,
    required_sheet_height_mm NUMERIC(10, 4) NOT NULL,
    required_grain_direction VARCHAR(32) NOT NULL DEFAULT 'LONG_GRAIN',
    required_sheets BIGINT NOT NULL,
    allocated_sheets BIGINT NOT NULL DEFAULT 0,
    deficit_sheets BIGINT NOT NULL DEFAULT 0,
    allocated_reams NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    allocated_weight_kg NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    allow_sheet_rotation BOOLEAN NOT NULL DEFAULT TRUE,
    allow_multi_batch_fulfillment BOOLEAN NOT NULL DEFAULT TRUE,
    selection_policy VARCHAR(32) NOT NULL DEFAULT 'FIFO',
    status VARCHAR(32) NOT NULL,
    is_fully_satisfied BOOLEAN NOT NULL DEFAULT FALSE,
    is_multi_batch_fulfillment BOOLEAN NOT NULL DEFAULT FALSE,
    primary_selected_batch_number VARCHAR(64),
    primary_selected_lot_number VARCHAR(64),
    primary_warehouse_id VARCHAR(64),
    overall_compatibility_score NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    selection_explanation TEXT NOT NULL,
    master_integrity_hash VARCHAR(64) NOT NULL,
    is_confirmed_and_allocated BOOLEAN NOT NULL DEFAULT FALSE,
    selected_by VARCHAR(64) NOT NULL,
    selected_at BIGINT NOT NULL,
    confirmed_at BIGINT,
    confirmed_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sbs_tenant_order ON substrate_batch_selection_records(tenant_id, order_id, order_item_id);
CREATE INDEX IF NOT EXISTS idx_sbs_tenant_job ON substrate_batch_selection_records(tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_sbs_tenant_status ON substrate_batch_selection_records(tenant_id, status);

CREATE TABLE IF NOT EXISTS substrate_batch_selection_allocations (
    allocation_id VARCHAR(64) PRIMARY KEY,
    selection_id VARCHAR(64) NOT NULL REFERENCES substrate_batch_selection_records(selection_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    warehouse_id VARCHAR(64) NOT NULL,
    warehouse_name VARCHAR(255) NOT NULL,
    location_id VARCHAR(64),
    batch_number VARCHAR(64) NOT NULL,
    lot_number VARCHAR(64) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    allocated_sheets BIGINT NOT NULL,
    allocated_reams NUMERIC(12, 4) NOT NULL,
    allocated_weight_kg NUMERIC(12, 4) NOT NULL,
    sheet_width_mm NUMERIC(10, 4) NOT NULL,
    sheet_height_mm NUMERIC(10, 4) NOT NULL,
    grain_direction VARCHAR(32) NOT NULL,
    is_rotated BOOLEAN NOT NULL DEFAULT FALSE,
    match_score NUMERIC(8, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sbs_alloc_selection ON substrate_batch_selection_allocations(selection_id);
CREATE INDEX IF NOT EXISTS idx_sbs_alloc_tenant_batch ON substrate_batch_selection_allocations(tenant_id, batch_number, lot_number);

CREATE TABLE IF NOT EXISTS substrate_batch_candidate_evaluations (
    evaluation_id VARCHAR(64) PRIMARY KEY,
    selection_id VARCHAR(64) NOT NULL REFERENCES substrate_batch_selection_records(selection_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    batch_number VARCHAR(64) NOT NULL,
    lot_number VARCHAR(64) NOT NULL,
    warehouse_id VARCHAR(64) NOT NULL,
    dimension_match VARCHAR(32) NOT NULL,
    grain_compatibility VARCHAR(32) NOT NULL,
    is_rotated BOOLEAN NOT NULL DEFAULT FALSE,
    gsm_match_score INT NOT NULL,
    overall_score NUMERIC(8, 4) NOT NULL,
    is_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    usable_sheets BIGINT NOT NULL,
    evaluation_reasons TEXT,
    rejection_reasons TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sbs_eval_selection ON substrate_batch_candidate_evaluations(selection_id);

CREATE TABLE IF NOT EXISTS substrate_batch_selection_audits (
    audit_id VARCHAR(64) PRIMARY KEY,
    selection_id VARCHAR(64) NOT NULL REFERENCES substrate_batch_selection_records(selection_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    details TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sbs_audit_selection ON substrate_batch_selection_audits(selection_id);

-- Enable and Force Row Level Security (RLS) for Multi-Tenant Isolation
ALTER TABLE substrate_batch_selection_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_batch_selection_records FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_substrate_batch_selection_records ON substrate_batch_selection_records
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE substrate_batch_selection_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_batch_selection_allocations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_substrate_batch_selection_allocations ON substrate_batch_selection_allocations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE substrate_batch_candidate_evaluations ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_batch_candidate_evaluations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_substrate_batch_candidate_evaluations ON substrate_batch_candidate_evaluations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE substrate_batch_selection_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_batch_selection_audits FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_substrate_batch_selection_audits ON substrate_batch_selection_audits
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
