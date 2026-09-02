-- Flyway Migration: V20261117__create_signature_imposition_tables.sql
-- Module 18 Step 04: Signature Layouts, Page Imposition & Work-and-Turn / Tumble

-- 1. Signature Imposition Specifications Table
CREATE TABLE IF NOT EXISTS signature_imposition_specifications (
    signature_imposition_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    total_pages INT NOT NULL,
    padded_total_pages INT NOT NULL,
    signature_page_count INT NOT NULL,
    total_signatures_count INT NOT NULL,
    binding_method VARCHAR(64) NOT NULL,
    sheet_turning_method VARCHAR(64) NOT NULL,
    folding_scheme VARCHAR(64) NOT NULL,
    paper_stock_type VARCHAR(64) NOT NULL,
    gsm NUMERIC(10, 4) NOT NULL,
    page_width_mm NUMERIC(10, 4) NOT NULL,
    page_height_mm NUMERIC(10, 4) NOT NULL,
    parent_sheet_width_mm NUMERIC(10, 4) NOT NULL,
    parent_sheet_height_mm NUMERIC(10, 4) NOT NULL,
    margin_top_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_bottom_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_left_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    margin_right_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    spine_gutter_mm NUMERIC(10, 4) NOT NULL DEFAULT 6.0000,
    head_gutter_mm NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    foot_gutter_mm NUMERIC(10, 4) NOT NULL DEFAULT 8.0000,
    face_trim_mm NUMERIC(10, 4) NOT NULL DEFAULT 6.0000,
    bleed_mm NUMERIC(10, 4) NOT NULL DEFAULT 3.0000,
    creep_is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    paper_caliper_mm NUMERIC(10, 4) NOT NULL DEFAULT 0.1200,
    total_creep_mm NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    creep_per_sheet_mm NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    innermost_page_shift_mm NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    common_required_sheets BIGINT NOT NULL,
    total_parent_sheets_required BIGINT NOT NULL,
    total_produced_copies BIGINT NOT NULL,
    overage_copies BIGINT NOT NULL,
    total_sheet_area_mm2 NUMERIC(14, 4) NOT NULL,
    usable_area_mm2 NUMERIC(14, 4) NOT NULL,
    occupied_area_mm2 NUMERIC(14, 4) NOT NULL,
    waste_area_mm2 NUMERIC(14, 4) NOT NULL,
    sheet_utilization_percentage NUMERIC(8, 4) NOT NULL,
    usable_yield_percentage NUMERIC(8, 4) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'OPTIMIZED',
    integrity_hash VARCHAR(128) NOT NULL,
    notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL
);

-- 2. Signature Forms Table
CREATE TABLE IF NOT EXISTS signature_forms (
    form_id VARCHAR(64) PRIMARY KEY,
    signature_imposition_id VARCHAR(64) NOT NULL REFERENCES signature_imposition_specifications(signature_imposition_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    signature_number INT NOT NULL,
    form_side VARCHAR(64) NOT NULL,
    pages_per_side INT NOT NULL,
    columns INT NOT NULL,
    rows INT NOT NULL,
    form_sheet_width_mm NUMERIC(10, 4) NOT NULL,
    form_sheet_height_mm NUMERIC(10, 4) NOT NULL,
    occupied_area_mm2 NUMERIC(14, 4) NOT NULL,
    usable_area_mm2 NUMERIC(14, 4) NOT NULL,
    yield_percentage NUMERIC(8, 4) NOT NULL
);

-- 3. Signature Page Allocations Table
CREATE TABLE IF NOT EXISTS signature_page_allocations (
    placement_id VARCHAR(64) PRIMARY KEY,
    form_id VARCHAR(64) NOT NULL REFERENCES signature_forms(form_id) ON DELETE CASCADE,
    signature_imposition_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    page_number INT NOT NULL,
    slot_index INT NOT NULL,
    grid_row INT NOT NULL,
    grid_column INT NOT NULL,
    x_mm NUMERIC(10, 4) NOT NULL,
    y_mm NUMERIC(10, 4) NOT NULL,
    width_mm NUMERIC(10, 4) NOT NULL,
    height_mm NUMERIC(10, 4) NOT NULL,
    head_orientation VARCHAR(32) NOT NULL,
    creep_shift_x_mm NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    creep_shift_y_mm NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    is_blank_page BOOLEAN NOT NULL DEFAULT FALSE
);

-- 4. Signature Imposition Audit Events Table
CREATE TABLE IF NOT EXISTS signature_imposition_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    signature_imposition_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    actor VARCHAR(64) NOT NULL,
    notes TEXT,
    timestamp BIGINT NOT NULL
);

-- Indexes for Fast Querying and Multi-Tenant Isolation
CREATE INDEX IF NOT EXISTS idx_sig_imp_tenant ON signature_imposition_specifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sig_imp_job ON signature_imposition_specifications(tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_sig_forms_spec ON signature_forms(tenant_id, signature_imposition_id);
CREATE INDEX IF NOT EXISTS idx_sig_pages_form ON signature_page_allocations(tenant_id, form_id);
CREATE INDEX IF NOT EXISTS idx_sig_audit_spec ON signature_imposition_audit_events(tenant_id, signature_imposition_id);

-- Enforce PostgreSQL Row Level Security (RLS)
ALTER TABLE signature_imposition_specifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE signature_imposition_specifications FORCE ROW LEVEL SECURITY;

ALTER TABLE signature_forms ENABLE ROW LEVEL SECURITY;
ALTER TABLE signature_forms FORCE ROW LEVEL SECURITY;

ALTER TABLE signature_page_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE signature_page_allocations FORCE ROW LEVEL SECURITY;

ALTER TABLE signature_imposition_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE signature_imposition_audit_events FORCE ROW LEVEL SECURITY;

-- Tenant Isolation Policies
DROP POLICY IF EXISTS tenant_isolation_signature_imposition_specifications ON signature_imposition_specifications;
CREATE POLICY tenant_isolation_signature_imposition_specifications ON signature_imposition_specifications
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_signature_forms ON signature_forms;
CREATE POLICY tenant_isolation_signature_forms ON signature_forms
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_signature_page_allocations ON signature_page_allocations;
CREATE POLICY tenant_isolation_signature_page_allocations ON signature_page_allocations
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

DROP POLICY IF EXISTS tenant_isolation_signature_imposition_audit_events ON signature_imposition_audit_events;
CREATE POLICY tenant_isolation_signature_imposition_audit_events ON signature_imposition_audit_events
    FOR ALL
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
