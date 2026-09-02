-- Module 18 Step 05: Prepress CTP Output, Plate Imposition Package & Production-Ready Export
-- Enterprise Multi-Tenant PostgreSQL Schema with FORCE ROW LEVEL SECURITY

CREATE TABLE IF NOT EXISTS ctp_output_specifications (
    ctp_output_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    source_imposition_type VARCHAR(64) NOT NULL,
    source_imposition_id VARCHAR(64) NOT NULL,
    source_imposition_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'GENERATED',
    package_version INT NOT NULL DEFAULT 1,
    resolution_dpi INT NOT NULL DEFAULT 2540,
    screening_method VARCHAR(32) NOT NULL DEFAULT 'AM_CONVENTIONAL',
    default_screen_ruling_lpi NUMERIC(10, 4) NOT NULL DEFAULT 175.0000,
    plate_width_mm NUMERIC(10, 4) NOT NULL,
    plate_height_mm NUMERIC(10, 4) NOT NULL,
    plate_thickness_mm NUMERIC(10, 4) NOT NULL DEFAULT 0.3000,
    gripper_margin_mm NUMERIC(10, 4) NOT NULL DEFAULT 45.0000,
    tail_margin_mm NUMERIC(10, 4) NOT NULL DEFAULT 25.0000,
    side_guide_margin_left_mm NUMERIC(10, 4) NOT NULL DEFAULT 30.0000,
    side_guide_margin_right_mm NUMERIC(10, 4) NOT NULL DEFAULT 30.0000,
    total_plates_count INT NOT NULL,
    front_plates_count INT NOT NULL,
    back_plates_count INT NOT NULL,
    spot_colors_count INT NOT NULL DEFAULT 0,
    press_sheet_width_mm NUMERIC(10, 4) NOT NULL,
    press_sheet_height_mm NUMERIC(10, 4) NOT NULL,
    rip_instructions TEXT NOT NULL,
    validation_summary TEXT NOT NULL,
    integrity_hash VARCHAR(128) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ctp_output_tenant_job ON ctp_output_specifications(tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_ctp_output_source_imposition ON ctp_output_specifications(tenant_id, source_imposition_id);
CREATE INDEX IF NOT EXISTS idx_ctp_output_status ON ctp_output_specifications(tenant_id, status);

ALTER TABLE ctp_output_specifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE ctp_output_specifications FORCE ROW LEVEL SECURITY;

CREATE POLICY ctp_output_tenant_isolation_policy ON ctp_output_specifications
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));

CREATE TABLE IF NOT EXISTS ctp_output_plates (
    plate_id VARCHAR(64) PRIMARY KEY,
    ctp_output_id VARCHAR(64) NOT NULL REFERENCES ctp_output_specifications(ctp_output_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    plate_name VARCHAR(255) NOT NULL,
    form_reference_id VARCHAR(64) NOT NULL,
    signature_number INT NOT NULL DEFAULT 1,
    plate_side VARCHAR(32) NOT NULL,
    color_separation VARCHAR(32) NOT NULL,
    spot_color_name VARCHAR(128),
    plate_width_mm NUMERIC(10, 4) NOT NULL,
    plate_height_mm NUMERIC(10, 4) NOT NULL,
    plate_thickness_mm NUMERIC(10, 4) NOT NULL DEFAULT 0.3000,
    resolution_dpi INT NOT NULL DEFAULT 2540,
    screening_method VARCHAR(32) NOT NULL DEFAULT 'AM_CONVENTIONAL',
    screen_ruling_lpi NUMERIC(10, 4) NOT NULL DEFAULT 175.0000,
    screen_angle_degrees NUMERIC(10, 4) NOT NULL,
    dot_shape VARCHAR(32) NOT NULL DEFAULT 'EUCLIDEAN',
    sheet_offset_x_mm NUMERIC(10, 4) NOT NULL,
    sheet_offset_y_mm NUMERIC(10, 4) NOT NULL,
    plate_area_mm2 NUMERIC(16, 4) NOT NULL,
    plate_integrity_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ctp_plates_output_id ON ctp_output_plates(ctp_output_id);
CREATE INDEX IF NOT EXISTS idx_ctp_plates_tenant_side ON ctp_output_plates(tenant_id, plate_side);

ALTER TABLE ctp_output_plates ENABLE ROW LEVEL SECURITY;
ALTER TABLE ctp_output_plates FORCE ROW LEVEL SECURITY;

CREATE POLICY ctp_plates_tenant_isolation_policy ON ctp_output_plates
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));

CREATE TABLE IF NOT EXISTS ctp_prepress_marks (
    mark_id VARCHAR(64) PRIMARY KEY,
    ctp_output_id VARCHAR(64) NOT NULL REFERENCES ctp_output_specifications(ctp_output_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    mark_type VARCHAR(64) NOT NULL,
    plate_side VARCHAR(32) NOT NULL,
    x_position_mm NUMERIC(10, 4) NOT NULL,
    y_position_mm NUMERIC(10, 4) NOT NULL,
    width_mm NUMERIC(10, 4) NOT NULL,
    height_mm NUMERIC(10, 4) NOT NULL,
    rotation_degrees NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    label_text VARCHAR(255),
    target_color_separation VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ctp_marks_output_id ON ctp_prepress_marks(ctp_output_id);
CREATE INDEX IF NOT EXISTS idx_ctp_marks_tenant_type ON ctp_prepress_marks(tenant_id, mark_type);

ALTER TABLE ctp_prepress_marks ENABLE ROW LEVEL SECURITY;
ALTER TABLE ctp_prepress_marks FORCE ROW LEVEL SECURITY;

CREATE POLICY ctp_marks_tenant_isolation_policy ON ctp_prepress_marks
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));

CREATE TABLE IF NOT EXISTS ctp_output_audits (
    audit_id VARCHAR(64) PRIMARY KEY,
    ctp_output_id VARCHAR(64) NOT NULL REFERENCES ctp_output_specifications(ctp_output_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ctp_audits_output_id ON ctp_output_audits(ctp_output_id);
CREATE INDEX IF NOT EXISTS idx_ctp_audits_tenant ON ctp_output_audits(tenant_id);

ALTER TABLE ctp_output_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE ctp_output_audits FORCE ROW LEVEL SECURITY;

CREATE POLICY ctp_audits_tenant_isolation_policy ON ctp_output_audits
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
