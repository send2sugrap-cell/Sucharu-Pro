-- Module 19: Substrate Stock Auto-Reservation
-- Step 05: Job Cancellation, Revision & Substrate Release Governance Tables

CREATE TABLE IF NOT EXISTS substrate_release_governance_records (
    governance_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    reservation_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64),
    trigger_type VARCHAR(64) NOT NULL,
    upstream_event_id VARCHAR(64),
    sku VARCHAR(64) NOT NULL,
    material_name VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(64) NOT NULL,
    previous_required_sheets BIGINT NOT NULL,
    new_required_sheets BIGINT NOT NULL,
    allocated_sheets BIGINT NOT NULL,
    consumed_sheets BIGINT NOT NULL DEFAULT 0,
    committed_sheets BIGINT NOT NULL DEFAULT 0,
    releasable_sheets BIGINT NOT NULL DEFAULT 0,
    retained_sheets BIGINT NOT NULL DEFAULT 0,
    additional_required_sheets BIGINT NOT NULL DEFAULT 0,
    decision VARCHAR(64) NOT NULL,
    execution_status VARCHAR(64) NOT NULL,
    blocking_reason VARCHAR(64) NOT NULL,
    explanation TEXT NOT NULL,
    deduplication_fingerprint VARCHAR(64) NOT NULL,
    master_integrity_hash VARCHAR(64) NOT NULL,
    evaluated_by VARCHAR(64) NOT NULL,
    evaluated_at BIGINT NOT NULL,
    approved_by VARCHAR(64),
    approved_at BIGINT,
    executed_by VARCHAR(64),
    executed_at BIGINT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_srg_tenant_res ON substrate_release_governance_records(tenant_id, reservation_id);
CREATE INDEX IF NOT EXISTS idx_srg_tenant_order ON substrate_release_governance_records(tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_srg_tenant_job ON substrate_release_governance_records(tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_srg_tenant_status ON substrate_release_governance_records(tenant_id, execution_status);
CREATE INDEX IF NOT EXISTS idx_srg_tenant_fingerprint ON substrate_release_governance_records(tenant_id, deduplication_fingerprint);

CREATE TABLE IF NOT EXISTS substrate_release_governance_audits (
    event_id VARCHAR(64) PRIMARY KEY,
    governance_id VARCHAR(64) NOT NULL REFERENCES substrate_release_governance_records(governance_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_status VARCHAR(64),
    new_status VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_srga_tenant_gov ON substrate_release_governance_audits(tenant_id, governance_id);

-- ROW LEVEL SECURITY ENFORCEMENT
ALTER TABLE substrate_release_governance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_release_governance_records FORCE ROW LEVEL SECURITY;

ALTER TABLE substrate_release_governance_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_release_governance_audits FORCE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'substrate_release_governance_records' AND policyname = 'substrate_release_governance_records_tenant_isolation'
    ) THEN
        CREATE POLICY substrate_release_governance_records_tenant_isolation ON substrate_release_governance_records
            FOR ALL
            USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'substrate_release_governance_audits' AND policyname = 'substrate_release_governance_audits_tenant_isolation'
    ) THEN
        CREATE POLICY substrate_release_governance_audits_tenant_isolation ON substrate_release_governance_audits
            FOR ALL
            USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
    END IF;
END $$;
