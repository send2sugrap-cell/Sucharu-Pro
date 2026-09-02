-- ============================================================================
-- SUCHARU PRO ERP — MODULE 17 STEP 03
-- Approved Quotation -> Order Conversion & Commercial Commitment Engine Schema
-- Migration: V20261104__create_commercial_commitment_conversion.sql
-- ============================================================================

-- 1. Commercial Commitments Table
CREATE TABLE IF NOT EXISTS commercial_commitments (
    commitment_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    quotation_id VARCHAR(64) NOT NULL,
    quotation_version INT NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64),
    order_number VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    committed_quantity BIGINT NOT NULL,
    approved_unit_price NUMERIC(18, 4) NOT NULL,
    approved_subtotal NUMERIC(18, 4) NOT NULL,
    approved_discount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    approved_tax NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    approved_grand_total NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    payment_terms VARCHAR(64) NOT NULL DEFAULT 'DEFAULT',
    delivery_terms VARCHAR(255),
    conversion_notes TEXT,
    idempotency_key VARCHAR(128),
    integrity_hash VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    converted_at BIGINT,
    converted_by VARCHAR(64),
    CONSTRAINT chk_comm_quantity CHECK (committed_quantity > 0),
    CONSTRAINT chk_comm_grand_total CHECK (approved_grand_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_comm_tenant_quote ON commercial_commitments (tenant_id, quotation_id);
CREATE INDEX IF NOT EXISTS idx_comm_tenant_order ON commercial_commitments (tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_comm_tenant_customer ON commercial_commitments (tenant_id, customer_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_comm_idempotency ON commercial_commitments (tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 2. Commercial Commitment Audit Events Table
CREATE TABLE IF NOT EXISTS commercial_commitment_events (
    event_id VARCHAR(64) PRIMARY KEY,
    commitment_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    details_json TEXT,
    occurred_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comm_event_tenant_comm ON commercial_commitment_events (tenant_id, commitment_id);
CREATE INDEX IF NOT EXISTS idx_comm_event_occurred ON commercial_commitment_events (occurred_at DESC);

-- 3. Row-Level Security (RLS) Policies
ALTER TABLE commercial_commitments ENABLE ROW LEVEL SECURITY;
ALTER TABLE commercial_commitments FORCE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'commercial_commitments' AND policyname = 'commercial_commitments_tenant_isolation'
    ) THEN
        CREATE POLICY commercial_commitments_tenant_isolation ON commercial_commitments
            USING (tenant_id = current_setting('app.current_tenant', true))
            WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

ALTER TABLE commercial_commitment_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE commercial_commitment_events FORCE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'commercial_commitment_events' AND policyname = 'commercial_commitment_events_tenant_isolation'
    ) THEN
        CREATE POLICY commercial_commitment_events_tenant_isolation ON commercial_commitment_events
            USING (tenant_id = current_setting('app.current_tenant', true))
            WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;
