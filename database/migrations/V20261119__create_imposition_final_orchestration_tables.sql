-- =============================================================================
-- Migration: V20261119__create_imposition_final_orchestration_tables.sql
-- Module: 18 - Dynamic Imposition & Gang-Run Optimizer Engine
-- Step: 06 - Imposition Audit Trail, Production Job Interlock & AI Handoff
-- =============================================================================

-- Table 1: Prepress Orchestration Master Plans
CREATE TABLE IF NOT EXISTS prepress_orchestration_plans (
    plan_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    plan_name VARCHAR(255) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    
    -- Source Business Identifiers
    job_id VARCHAR(64),
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    
    -- Upstream Step References & Hashes
    step01_imposition_id VARCHAR(64),
    step01_integrity_hash VARCHAR(64),
    step02_gang_run_batch_id VARCHAR(64),
    step02_integrity_hash VARCHAR(64),
    step03_nesting_id VARCHAR(64),
    step03_integrity_hash VARCHAR(64),
    step04_signature_id VARCHAR(64),
    step04_integrity_hash VARCHAR(64),
    step05_ctp_output_id VARCHAR(64),
    step05_integrity_hash VARCHAR(64),
    
    -- Reconciled Metrics
    required_quantity BIGINT NOT NULL,
    total_produced_quantity BIGINT NOT NULL,
    required_sheets BIGINT NOT NULL,
    sheet_utilization_percentage NUMERIC(8, 4) NOT NULL,
    waste_percentage NUMERIC(8, 4) NOT NULL,
    total_signatures_count INT NOT NULL DEFAULT 0,
    total_plates_count INT NOT NULL DEFAULT 0,
    press_sheet_width_mm NUMERIC(10, 4) NOT NULL,
    press_sheet_height_mm NUMERIC(10, 4) NOT NULL,
    plate_width_mm NUMERIC(10, 4) NOT NULL,
    plate_height_mm NUMERIC(10, 4) NOT NULL,
    
    -- Readiness & Reconciliation
    readiness_score NUMERIC(8, 4) NOT NULL,
    is_fully_reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    blocking_errors_count INT NOT NULL DEFAULT 0,
    warnings_count INT NOT NULL DEFAULT 0,
    reconciliation_summary TEXT,
    
    -- Master Integrity Seal (SHA-256)
    master_integrity_hash VARCHAR(64) NOT NULL,
    
    -- Governance & Audit
    approval_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    approved_by VARCHAR(64),
    approved_at BIGINT,
    ai_handoff_status VARCHAR(32) NOT NULL DEFAULT 'READY_FOR_HANDOFF',
    downstream_handoff_status VARCHAR(32) NOT NULL DEFAULT 'EMITTED',
    notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL
);

-- Table 2: Prepress Discrepancies & Audit Diagnostics
CREATE TABLE IF NOT EXISTS prepress_reconciliation_discrepancies (
    discrepancy_id VARCHAR(64) PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL REFERENCES prepress_orchestration_plans(plan_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(64) NOT NULL,
    source_step VARCHAR(32) NOT NULL,
    target_step VARCHAR(32) NOT NULL,
    expected_value VARCHAR(255) NOT NULL,
    actual_value VARCHAR(255) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    message TEXT NOT NULL
);

-- Table 3: Prepress Optimization Recommendations
CREATE TABLE IF NOT EXISTS prepress_optimization_recommendations (
    recommendation_id VARCHAR(64) PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL REFERENCES prepress_orchestration_plans(plan_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    recommendation_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    affected_step VARCHAR(32) NOT NULL,
    estimated_waste_reduction_percentage NUMERIC(8, 4) NOT NULL,
    estimated_plate_savings_count INT NOT NULL DEFAULT 0,
    rationale TEXT NOT NULL,
    confidence_score NUMERIC(6, 4) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
    is_applied BOOLEAN NOT NULL DEFAULT FALSE
);

-- Table 4: Prepress Orchestration Audits
CREATE TABLE IF NOT EXISTS prepress_orchestration_audits (
    audit_id VARCHAR(64) PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL REFERENCES prepress_orchestration_plans(plan_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    reason TEXT,
    timestamp BIGINT NOT NULL
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_prepress_plans_tenant_job ON prepress_orchestration_plans(tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_prepress_plans_tenant_order ON prepress_orchestration_plans(tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_prepress_discrepancies_plan ON prepress_reconciliation_discrepancies(plan_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_prepress_recommendations_plan ON prepress_optimization_recommendations(plan_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_prepress_audits_plan ON prepress_orchestration_audits(plan_id, tenant_id);

-- Enable Row Level Security (RLS)
ALTER TABLE prepress_orchestration_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE prepress_orchestration_plans FORCE ROW LEVEL SECURITY;

ALTER TABLE prepress_reconciliation_discrepancies ENABLE ROW LEVEL SECURITY;
ALTER TABLE prepress_reconciliation_discrepancies FORCE ROW LEVEL SECURITY;

ALTER TABLE prepress_optimization_recommendations ENABLE ROW LEVEL SECURITY;
ALTER TABLE prepress_optimization_recommendations FORCE ROW LEVEL SECURITY;

ALTER TABLE prepress_orchestration_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE prepress_orchestration_audits FORCE ROW LEVEL SECURITY;

-- RLS Policies
DROP POLICY IF EXISTS prepress_plans_tenant_isolation ON prepress_orchestration_plans;
CREATE POLICY prepress_plans_tenant_isolation ON prepress_orchestration_plans
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

DROP POLICY IF EXISTS prepress_discrepancies_tenant_isolation ON prepress_reconciliation_discrepancies;
CREATE POLICY prepress_discrepancies_tenant_isolation ON prepress_reconciliation_discrepancies
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

DROP POLICY IF EXISTS prepress_recommendations_tenant_isolation ON prepress_optimization_recommendations;
CREATE POLICY prepress_recommendations_tenant_isolation ON prepress_optimization_recommendations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

DROP POLICY IF EXISTS prepress_audits_tenant_isolation ON prepress_orchestration_audits;
CREATE POLICY prepress_audits_tenant_isolation ON prepress_orchestration_audits
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
