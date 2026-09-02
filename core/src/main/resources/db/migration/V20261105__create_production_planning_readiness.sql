-- =========================================================================
-- MODULE 17 STEP 04: ORDER-TO-PRODUCTION PLANNING & MANUFACTURING READINESS
-- Schema Migration: V20261105__create_production_planning_readiness.sql
-- =========================================================================

-- 1. Production Planning Snapshots Table
CREATE TABLE IF NOT EXISTS production_planning_snapshots (
    planning_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    commercial_commitment_id VARCHAR(64),
    quotation_id VARCHAR(64),
    quotation_version_number INT,
    customer_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version INT NOT NULL DEFAULT 1,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    readiness_score NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    feasibility_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    order_requested_date BIGINT,
    estimated_completion_date BIGINT,
    planning_fingerprint VARCHAR(128) NOT NULL,
    integrity_hash VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    CONSTRAINT chk_planning_status CHECK (status IN (
        'DRAFT', 'ANALYZING', 'READY', 'BLOCKED', 'NEEDS_INFORMATION', 'REQUIRES_REVIEW', 'SUPERSEDED', 'CANCELLED', 'HANDED_OFF'
    )),
    CONSTRAINT chk_feasibility_status CHECK (feasibility_status IN (
        'FEASIBLE', 'AT_RISK', 'NOT_FEASIBLE', 'UNKNOWN'
    ))
);

CREATE INDEX IF NOT EXISTS idx_prod_planning_tenant_order ON production_planning_snapshots (tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_prod_planning_tenant_current ON production_planning_snapshots (tenant_id, order_id, is_current);
CREATE UNIQUE INDEX IF NOT EXISTS uq_prod_planning_idempotency ON production_planning_snapshots (tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_prod_planning_tenant_order_ver ON production_planning_snapshots (tenant_id, order_id, order_item_id, version);

-- 2. Production Job Specifications Table
CREATE TABLE IF NOT EXISTS production_job_specifications (
    spec_id VARCHAR(64) PRIMARY KEY,
    planning_id VARCHAR(64) NOT NULL REFERENCES production_planning_snapshots(planning_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    product_type VARCHAR(64) NOT NULL,
    ordered_quantity BIGINT NOT NULL,
    planned_quantity BIGINT NOT NULL,
    finished_width_mm NUMERIC(10, 4) NOT NULL,
    finished_height_mm NUMERIC(10, 4) NOT NULL,
    substrate_type VARCHAR(64) NOT NULL,
    substrate_gsm INT NOT NULL,
    substrate_brand VARCHAR(128),
    parent_sheet_width_mm NUMERIC(10, 4) NOT NULL,
    parent_sheet_height_mm NUMERIC(10, 4) NOT NULL,
    press_sheet_width_mm NUMERIC(10, 4) NOT NULL,
    press_sheet_height_mm NUMERIC(10, 4) NOT NULL,
    printing_method VARCHAR(32) NOT NULL,
    colors_front INT NOT NULL DEFAULT 4,
    colors_back INT NOT NULL DEFAULT 0,
    coating_front VARCHAR(32) NOT NULL DEFAULT 'NONE',
    coating_back VARCHAR(32) NOT NULL DEFAULT 'NONE',
    imposition_ups INT NOT NULL DEFAULT 1,
    lamination VARCHAR(32) NOT NULL DEFAULT 'NONE',
    binding_method VARCHAR(32) NOT NULL DEFAULT 'NONE',
    folding_type VARCHAR(32) NOT NULL DEFAULT 'NONE',
    cutting_required BOOLEAN NOT NULL DEFAULT TRUE,
    die_cutting_required BOOLEAN NOT NULL DEFAULT FALSE,
    packaging_method VARCHAR(64) NOT NULL DEFAULT 'CARTON_BOX',
    artwork_url TEXT,
    special_instructions TEXT,
    spec_fingerprint VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_prod_job_spec_planning ON production_job_specifications (planning_id);

-- 3. Production Planning Requirements Table
CREATE TABLE IF NOT EXISTS production_planning_requirements (
    requirement_id VARCHAR(64) PRIMARY KEY,
    planning_id VARCHAR(64) NOT NULL REFERENCES production_planning_snapshots(planning_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    required_quantity NUMERIC(15, 4) NOT NULL,
    make_ready_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0,
    waste_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0,
    total_planned_quantity NUMERIC(15, 4) NOT NULL,
    unit_of_measure VARCHAR(32) NOT NULL,
    estimated_available BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_prod_req_planning ON production_planning_requirements (planning_id);

-- 4. Production Planning Operations Table
CREATE TABLE IF NOT EXISTS production_planning_operations (
    operation_id VARCHAR(64) PRIMARY KEY,
    planning_id VARCHAR(64) NOT NULL REFERENCES production_planning_snapshots(planning_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    sequence_number INT NOT NULL,
    stage_type VARCHAR(32) NOT NULL,
    operation_code VARCHAR(64) NOT NULL,
    operation_name VARCHAR(128) NOT NULL,
    target_work_center VARCHAR(64) NOT NULL,
    estimated_setup_minutes INT NOT NULL DEFAULT 0,
    estimated_run_minutes INT NOT NULL DEFAULT 0,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    is_qc_checkpoint BOOLEAN NOT NULL DEFAULT FALSE,
    dependencies TEXT,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_prod_ops_planning ON production_planning_operations (planning_id);

-- 5. Production Planning Diagnostics Table
CREATE TABLE IF NOT EXISTS production_planning_diagnostics (
    diagnostic_id VARCHAR(64) PRIMARY KEY,
    planning_id VARCHAR(64) NOT NULL REFERENCES production_planning_snapshots(planning_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    is_blocking BOOLEAN NOT NULL DEFAULT FALSE,
    recommended_action TEXT
);

CREATE INDEX IF NOT EXISTS idx_prod_diag_planning ON production_planning_diagnostics (planning_id);

-- 6. Production Planning Events Table (Append-only Audit Trail)
CREATE TABLE IF NOT EXISTS production_planning_events (
    event_id VARCHAR(64) PRIMARY KEY,
    planning_id VARCHAR(64) NOT NULL REFERENCES production_planning_snapshots(planning_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    event_payload TEXT,
    performed_by VARCHAR(64) NOT NULL,
    performed_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_prod_events_planning ON production_planning_events (planning_id);
CREATE INDEX IF NOT EXISTS idx_prod_events_tenant ON production_planning_events (tenant_id);

-- =========================================================================
-- ROW-LEVEL SECURITY (RLS) POLICIES
-- =========================================================================

ALTER TABLE production_planning_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_planning_snapshots FORCE ROW LEVEL SECURITY;

ALTER TABLE production_job_specifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_job_specifications FORCE ROW LEVEL SECURITY;

ALTER TABLE production_planning_requirements ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_planning_requirements FORCE ROW LEVEL SECURITY;

ALTER TABLE production_planning_operations ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_planning_operations FORCE ROW LEVEL SECURITY;

ALTER TABLE production_planning_diagnostics ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_planning_diagnostics FORCE ROW LEVEL SECURITY;

ALTER TABLE production_planning_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_planning_events FORCE ROW LEVEL SECURITY;

CREATE POLICY production_planning_snapshots_tenant_isolation
    ON production_planning_snapshots
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

CREATE POLICY production_job_specifications_tenant_isolation
    ON production_job_specifications
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

CREATE POLICY production_planning_requirements_tenant_isolation
    ON production_planning_requirements
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

CREATE POLICY production_planning_operations_tenant_isolation
    ON production_planning_operations
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

CREATE POLICY production_planning_diagnostics_tenant_isolation
    ON production_planning_diagnostics
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

CREATE POLICY production_planning_events_tenant_isolation
    ON production_planning_events
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
