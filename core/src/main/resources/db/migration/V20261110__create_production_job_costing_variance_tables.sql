-- =============================================================================
-- V20261110__create_production_job_costing_variance_tables.sql
-- Module 17 Step 09: Production Actual Job Costing, Variance Analysis & Reconciliation
-- =============================================================================

CREATE TABLE IF NOT EXISTS production_actual_job_cost_records (
    cost_record_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    manufactured_good_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_material_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_labor_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_machine_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_quality_scrap_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_rework_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_packaging_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_overhead_allocated_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    grand_total_actual_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_unit_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    material_breakdown_json TEXT,
    labor_breakdown_json TEXT,
    machine_breakdown_json TEXT,
    scrap_rework_breakdown_json TEXT,
    packaging_breakdown_json TEXT,
    cost_status VARCHAR(32) NOT NULL DEFAULT 'ACTUAL_COSTED',
    calculated_at BIGINT NOT NULL,
    calculated_by VARCHAR(64) NOT NULL DEFAULT 'cost-engine'
);

CREATE INDEX IF NOT EXISTS idx_actual_job_cost_tenant_job ON production_actual_job_cost_records(tenant_id, execution_job_id);

ALTER TABLE production_actual_job_cost_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_actual_job_cost_records FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'production_actual_job_cost_tenant_isolation') THEN
        CREATE POLICY production_actual_job_cost_tenant_isolation ON production_actual_job_cost_records
            USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
            WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
    END IF;
END $$;


CREATE TABLE IF NOT EXISTS production_job_cost_variance_records (
    variance_record_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_good_output_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    quoted_selling_price NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    estimated_total_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_total_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_cost_variance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_cost_variance_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    overall_cost_classification VARCHAR(32) NOT NULL DEFAULT 'NEUTRAL',
    estimated_material_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_material_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    material_variance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    material_variance_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    material_cost_classification VARCHAR(32) NOT NULL DEFAULT 'NEUTRAL',
    estimated_labor_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_labor_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    labor_variance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    labor_variance_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    labor_cost_classification VARCHAR(32) NOT NULL DEFAULT 'NEUTRAL',
    estimated_machine_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_machine_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    machine_variance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    machine_variance_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    machine_cost_classification VARCHAR(32) NOT NULL DEFAULT 'NEUTRAL',
    total_quality_scrap_rework_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    estimated_unit_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_unit_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    unit_cost_variance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    estimated_gross_profit NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_gross_profit NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    gross_profit_variance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    estimated_gross_margin_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    actual_gross_margin_percentage NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    gross_margin_percentage_delta NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    generated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_cost_variance_tenant_job ON production_job_cost_variance_records(tenant_id, execution_job_id);

ALTER TABLE production_job_cost_variance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_job_cost_variance_records FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'production_job_cost_variance_tenant_isolation') THEN
        CREATE POLICY production_job_cost_variance_tenant_isolation ON production_job_cost_variance_records
            USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
            WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
    END IF;
END $$;


CREATE TABLE IF NOT EXISTS production_job_cost_reconciliation_records (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    bom_quantities_reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    labor_hours_reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    machine_hours_reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    scrap_rework_valuation_consistent BOOLEAN NOT NULL DEFAULT FALSE,
    packaging_cost_balanced BOOLEAN NOT NULL DEFAULT FALSE,
    actual_cost_math_balanced BOOLEAN NOT NULL DEFAULT FALSE,
    variance_integrity_hash_valid BOOLEAN NOT NULL DEFAULT FALSE,
    multi_tenant_isolation_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_fully_reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    certificate_hash VARCHAR(128) NOT NULL,
    discrepancies_json TEXT,
    reconciled_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_cost_reconcile_tenant_job ON production_job_cost_reconciliation_records(tenant_id, execution_job_id);

ALTER TABLE production_job_cost_reconciliation_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_job_cost_reconciliation_records FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'production_job_cost_reconcile_tenant_isolation') THEN
        CREATE POLICY production_job_cost_reconcile_tenant_isolation ON production_job_cost_reconciliation_records
            USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
            WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
    END IF;
END $$;


CREATE TABLE IF NOT EXISTS production_job_costing_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_cost_audit_tenant_job ON production_job_costing_audit_events(tenant_id, execution_job_id);

ALTER TABLE production_job_costing_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_job_costing_audit_events FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'production_job_cost_audit_tenant_isolation') THEN
        CREATE POLICY production_job_cost_audit_tenant_isolation ON production_job_costing_audit_events
            USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
            WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
    END IF;
END $$;
