-- Module 19: Substrate Stock Auto-Reservation
-- Step 06: Enterprise Reservation Audit, RLS & Cross-Module AI Handoff Tables

CREATE TABLE IF NOT EXISTS substrate_enterprise_audits (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    reservation_id VARCHAR(64) NOT NULL,
    reservation_version BIGINT NOT NULL DEFAULT 1,
    job_id VARCHAR(64),
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    substrate_requirement_id VARCHAR(64),
    batch_lot_id VARCHAR(64),
    warehouse_id VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    previous_state VARCHAR(64),
    new_state VARCHAR(64) NOT NULL,
    actor_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    role VARCHAR(64) NOT NULL,
    permission_context VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    reason TEXT NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64),
    idempotency_key VARCHAR(64),
    source_module VARCHAR(64) NOT NULL DEFAULT 'MODULE_19',
    source_operation VARCHAR(128) NOT NULL,
    event_outbox_id VARCHAR(64),
    record_hash VARCHAR(64) NOT NULL,
    previous_audit_hash VARCHAR(64),
    chain_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sea_tenant_res ON substrate_enterprise_audits(tenant_id, reservation_id);
CREATE INDEX IF NOT EXISTS idx_sea_tenant_order ON substrate_enterprise_audits(tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_sea_tenant_job ON substrate_enterprise_audits(tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_sea_tenant_event ON substrate_enterprise_audits(tenant_id, event_type);
CREATE INDEX IF NOT EXISTS idx_sea_tenant_timestamp ON substrate_enterprise_audits(tenant_id, timestamp);

CREATE TABLE IF NOT EXISTS substrate_reservation_reconciliations (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    reservation_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    sku VARCHAR(64) NOT NULL,
    required_sheets BIGINT NOT NULL,
    reserved_sheets BIGINT NOT NULL,
    physical_on_hand_sheets BIGINT NOT NULL DEFAULT 0,
    allocated_batch_sheets BIGINT NOT NULL DEFAULT 0,
    releasable_sheets BIGINT NOT NULL DEFAULT 0,
    consumed_sheets BIGINT NOT NULL DEFAULT 0,
    committed_sheets BIGINT NOT NULL DEFAULT 0,
    replenishment_required_sheets BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL,
    reconciled_by VARCHAR(64) NOT NULL,
    reconciled_at BIGINT NOT NULL,
    integrity_hash VARCHAR(64) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_srr_tenant_res ON substrate_reservation_reconciliations(tenant_id, reservation_id);
CREATE INDEX IF NOT EXISTS idx_srr_tenant_order ON substrate_reservation_reconciliations(tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_srr_tenant_status ON substrate_reservation_reconciliations(tenant_id, status);

CREATE TABLE IF NOT EXISTS substrate_reconciliation_discrepancies (
    discrepancy_id VARCHAR(64) PRIMARY KEY,
    reconciliation_id VARCHAR(64) NOT NULL REFERENCES substrate_reservation_reconciliations(reconciliation_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    discrepancy_type VARCHAR(64) NOT NULL,
    severity VARCHAR(64) NOT NULL,
    field_or_context VARCHAR(128) NOT NULL,
    expected_value TEXT NOT NULL,
    actual_value TEXT NOT NULL,
    explanation TEXT NOT NULL,
    resolution_recommendation TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_srd_tenant_recon ON substrate_reconciliation_discrepancies(tenant_id, reconciliation_id);

CREATE TABLE IF NOT EXISTS substrate_enterprise_ai_handoff_records (
    handoff_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    reservation_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    contract_version VARCHAR(32) NOT NULL DEFAULT '6.0.0',
    reservation_status VARCHAR(64) NOT NULL,
    reconciliation_status VARCHAR(64) NOT NULL,
    integrity_status VARCHAR(64) NOT NULL,
    master_integrity_hash VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    generated_by VARCHAR(64) NOT NULL,
    generated_at BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_seah_tenant_res ON substrate_enterprise_ai_handoff_records(tenant_id, reservation_id);

-- ROW LEVEL SECURITY ENFORCEMENT
ALTER TABLE substrate_enterprise_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_enterprise_audits FORCE ROW LEVEL SECURITY;

ALTER TABLE substrate_reservation_reconciliations ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_reservation_reconciliations FORCE ROW LEVEL SECURITY;

ALTER TABLE substrate_reconciliation_discrepancies ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_reconciliation_discrepancies FORCE ROW LEVEL SECURITY;

ALTER TABLE substrate_enterprise_ai_handoff_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_enterprise_ai_handoff_records FORCE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'substrate_enterprise_audits' AND policyname = 'substrate_enterprise_audits_tenant_isolation'
    ) THEN
        CREATE POLICY substrate_enterprise_audits_tenant_isolation ON substrate_enterprise_audits
            FOR ALL
            USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'substrate_reservation_reconciliations' AND policyname = 'substrate_reservation_reconciliations_tenant_isolation'
    ) THEN
        CREATE POLICY substrate_reservation_reconciliations_tenant_isolation ON substrate_reservation_reconciliations
            FOR ALL
            USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'substrate_reconciliation_discrepancies' AND policyname = 'substrate_reconciliation_discrepancies_tenant_isolation'
    ) THEN
        CREATE POLICY substrate_reconciliation_discrepancies_tenant_isolation ON substrate_reconciliation_discrepancies
            FOR ALL
            USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'substrate_enterprise_ai_handoff_records' AND policyname = 'substrate_enterprise_ai_handoff_records_tenant_isolation'
    ) THEN
        CREATE POLICY substrate_enterprise_ai_handoff_records_tenant_isolation ON substrate_enterprise_ai_handoff_records
            FOR ALL
            USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
    END IF;
END $$;
