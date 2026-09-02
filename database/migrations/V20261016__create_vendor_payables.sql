-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- MODULE 15 STEP 02: VENDOR PAYABLE & SUPPLIER LIABILITY MANAGEMENT FOUNDATION
-- Production-grade schema migration with Force Row Level Security (RLS) enforcement.
-- ====================================================================================

-- 1. Vendor Payables Table
CREATE TABLE IF NOT EXISTS vendor_payables (
    payable_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL DEFAULT 'PRJ-001',
    payable_number VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    vendor_job_id VARCHAR(64),
    bill_reference VARCHAR(128),
    description TEXT NOT NULL,
    notes TEXT,
    original_amount NUMERIC(18, 4) NOT NULL,
    paid_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    issue_date BIGINT NOT NULL,
    payment_terms VARCHAR(32) NOT NULL DEFAULT 'NET_30',
    custom_term_days INT,
    due_date BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    attachment_url TEXT,
    idempotency_key VARCHAR(128),
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    submitted_by VARCHAR(64),
    submitted_at BIGINT,
    approved_by VARCHAR(64),
    approved_at BIGINT,
    rejected_by VARCHAR(64),
    rejected_at BIGINT,
    recheck_requested_by VARCHAR(64),
    recheck_requested_at BIGINT,
    cancelled_by VARCHAR(64),
    cancelled_at BIGINT,
    voided_by VARCHAR(64),
    voided_at BIGINT,
    rejection_reason TEXT,
    cancellation_reason TEXT,
    void_reason TEXT,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_vp_original_amount CHECK (original_amount > 0),
    CONSTRAINT chk_vp_paid_amount CHECK (paid_amount >= 0 AND paid_amount <= original_amount),
    CONSTRAINT chk_vp_currency CHECK (LENGTH(currency) = 3)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vp_tenant_project_number
    ON vendor_payables(tenant_id, project_id, payable_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vp_tenant_project_idempotency
    ON vendor_payables(tenant_id, project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vp_tenant_vendor_due
    ON vendor_payables(tenant_id, vendor_id, due_date);

CREATE INDEX IF NOT EXISTS idx_vp_tenant_status
    ON vendor_payables(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_vp_tenant_job
    ON vendor_payables(tenant_id, job_id)
    WHERE job_id IS NOT NULL;

-- 2. Vendor Payable Payment Allocations Table
CREATE TABLE IF NOT EXISTS vendor_payable_payment_allocations (
    allocation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL DEFAULT 'PRJ-001',
    payable_id VARCHAR(64) NOT NULL REFERENCES vendor_payables(payable_id) ON DELETE RESTRICT,
    vendor_id VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    payment_method VARCHAR(32) NOT NULL,
    payment_reference VARCHAR(128),
    payment_date BIGINT NOT NULL,
    notes TEXT,
    allocated_by VARCHAR(64) NOT NULL,
    allocated_at BIGINT NOT NULL,
    idempotency_key VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_vpa_amount CHECK (amount > 0),
    CONSTRAINT chk_vpa_currency CHECK (LENGTH(currency) = 3)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vpa_tenant_idempotency
    ON vendor_payable_payment_allocations(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vpa_tenant_payable
    ON vendor_payable_payment_allocations(tenant_id, payable_id);

CREATE INDEX IF NOT EXISTS idx_vpa_tenant_vendor
    ON vendor_payable_payment_allocations(tenant_id, vendor_id);

-- 3. Vendor Payable Audit Events Table (Append-Only)
CREATE TABLE IF NOT EXISTS vendor_payable_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL DEFAULT 'PRJ-001',
    payable_id VARCHAR(64) NOT NULL REFERENCES vendor_payables(payable_id) ON DELETE RESTRICT,
    vendor_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    timestamp BIGINT NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    amount NUMERIC(18, 4),
    reason TEXT,
    correlation_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_vpa_audit_tenant_payable
    ON vendor_payable_audit_events(tenant_id, payable_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_vpa_audit_tenant_vendor
    ON vendor_payable_audit_events(tenant_id, vendor_id, timestamp);

-- ====================================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ====================================================================================

-- 1. vendor_payables RLS
ALTER TABLE vendor_payables ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_payables FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vendor_payables ON vendor_payables;
CREATE POLICY tenant_isolation_vendor_payables ON vendor_payables
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_tenant', true)
        OR current_setting('app.current_tenant', true) IS NULL
        OR current_setting('app.current_tenant', true) = ''
    )
    WITH CHECK (
        tenant_id = current_setting('app.current_tenant', true)
        OR current_setting('app.current_tenant', true) IS NULL
        OR current_setting('app.current_tenant', true) = ''
    );

-- 2. vendor_payable_payment_allocations RLS
ALTER TABLE vendor_payable_payment_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_payable_payment_allocations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vendor_payable_allocations ON vendor_payable_payment_allocations;
CREATE POLICY tenant_isolation_vendor_payable_allocations ON vendor_payable_payment_allocations
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_tenant', true)
        OR current_setting('app.current_tenant', true) IS NULL
        OR current_setting('app.current_tenant', true) = ''
    )
    WITH CHECK (
        tenant_id = current_setting('app.current_tenant', true)
        OR current_setting('app.current_tenant', true) IS NULL
        OR current_setting('app.current_tenant', true) = ''
    );

-- 3. vendor_payable_audit_events RLS
ALTER TABLE vendor_payable_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_payable_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vendor_payable_audits ON vendor_payable_audit_events;
CREATE POLICY tenant_isolation_vendor_payable_audits ON vendor_payable_audit_events
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_tenant', true)
        OR current_setting('app.current_tenant', true) IS NULL
        OR current_setting('app.current_tenant', true) = ''
    )
    WITH CHECK (
        tenant_id = current_setting('app.current_tenant', true)
        OR current_setting('app.current_tenant', true) IS NULL
        OR current_setting('app.current_tenant', true) = ''
    );
