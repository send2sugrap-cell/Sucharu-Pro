-- ======================================================================================
-- SUCHARU PRO ERP: MODULE 12 STEP 10
-- Flyway Migration: V20260924__create_vendor_settlement_analytics_integration.sql
-- Subsystem: Vendor Settlement, Analytics & Module Integration
-- ======================================================================================

-- 1. Table: vendor_settlements
CREATE TABLE IF NOT EXISTS vendor_settlements (
    settlement_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    settlement_number VARCHAR(100) NOT NULL,
    settlement_date BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    settlement_method VARCHAR(50) NOT NULL DEFAULT 'BANK_TRANSFER',
    reference_number VARCHAR(255),
    payment_id VARCHAR(64),
    notes TEXT,
    approved_by VARCHAR(64),
    approved_at BIGINT,
    settled_at BIGINT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uk_vendor_settlement_number UNIQUE (tenant_id, settlement_number)
);

CREATE INDEX IF NOT EXISTS idx_v_settlements_vendor ON vendor_settlements(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_v_settlements_status ON vendor_settlements(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_v_settlements_project ON vendor_settlements(tenant_id, project_id);

ALTER TABLE vendor_settlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_settlements FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_settlements_tenant_isolation') THEN
        CREATE POLICY vendor_settlements_tenant_isolation ON vendor_settlements
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;

-- 2. Table: vendor_settlement_allocations
CREATE TABLE IF NOT EXISTS vendor_settlement_allocations (
    allocation_id VARCHAR(64) PRIMARY KEY,
    settlement_id VARCHAR(64) NOT NULL REFERENCES vendor_settlements(settlement_id) ON DELETE CASCADE,
    payable_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64),
    allocated_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    status VARCHAR(50) NOT NULL DEFAULT 'ALLOCATED',
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_v_settlement_alloc_settlement ON vendor_settlement_allocations(settlement_id);
CREATE INDEX IF NOT EXISTS idx_v_settlement_alloc_payable ON vendor_settlement_allocations(payable_id);

-- 3. Table: vendor_reconciliation_results
CREATE TABLE IF NOT EXISTS vendor_reconciliation_results (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    project_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    settlement_id VARCHAR(64),
    payable_id VARCHAR(64),
    payment_id VARCHAR(64),
    status VARCHAR(50) NOT NULL,
    expected_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    settled_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    paid_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ledger_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    variance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    reasons TEXT,
    reconciled_at BIGINT NOT NULL,
    reconciled_by VARCHAR(64) NOT NULL DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_v_reconciliation_vendor ON vendor_reconciliation_results(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_v_reconciliation_status ON vendor_reconciliation_results(tenant_id, status);

ALTER TABLE vendor_reconciliation_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_reconciliation_results FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_reconciliation_tenant_isolation') THEN
        CREATE POLICY vendor_reconciliation_tenant_isolation ON vendor_reconciliation_results
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;

-- 4. Table: vendor_analytics_snapshots
CREATE TABLE IF NOT EXISTS vendor_analytics_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    project_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    period VARCHAR(50) NOT NULL,
    start_date BIGINT NOT NULL,
    end_date BIGINT NOT NULL,
    generated_at BIGINT NOT NULL,
    generated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    calculation_version VARCHAR(50) NOT NULL DEFAULT '1.0.0',
    metrics_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_v_analytics_snapshot_vendor ON vendor_analytics_snapshots(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_v_analytics_snapshot_period ON vendor_analytics_snapshots(tenant_id, period);

ALTER TABLE vendor_analytics_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_analytics_snapshots FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_analytics_snapshots_tenant_isolation') THEN
        CREATE POLICY vendor_analytics_snapshots_tenant_isolation ON vendor_analytics_snapshots
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;

-- 5. Table: vendor_settlement_audit_events
CREATE TABLE IF NOT EXISTS vendor_settlement_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    settlement_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    event_type VARCHAR(100) NOT NULL,
    details TEXT NOT NULL,
    actor VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_v_settlement_audit_settlement ON vendor_settlement_audit_events(tenant_id, settlement_id);
CREATE INDEX IF NOT EXISTS idx_v_settlement_audit_vendor ON vendor_settlement_audit_events(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_v_settlement_audit_timestamp ON vendor_settlement_audit_events(tenant_id, timestamp);

ALTER TABLE vendor_settlement_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_settlement_audit_events FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'vendor_settlement_audit_tenant_isolation') THEN
        CREATE POLICY vendor_settlement_audit_tenant_isolation ON vendor_settlement_audit_events
            USING (tenant_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;
