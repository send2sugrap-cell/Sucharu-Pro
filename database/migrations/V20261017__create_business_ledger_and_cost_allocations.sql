-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- MODULE 15 STEP 03: BUSINESS LEDGER, FINANCIAL POSTING & COST ALLOCATION FOUNDATION
-- Production-grade schema migration with Force Row Level Security (RLS) enforcement.
-- ====================================================================================

-- 1. Business Ledger Postings Table (Immutable Financial Posting Layer)
CREATE TABLE IF NOT EXISTS business_ledger_postings (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL DEFAULT 'PRJ-001',
    posting_number VARCHAR(64) NOT NULL,
    posting_type VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    account_category VARCHAR(64) NOT NULL,
    debit_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    credit_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    posting_date BIGINT NOT NULL,
    effective_date BIGINT NOT NULL,
    description TEXT NOT NULL,
    reference VARCHAR(128),
    job_id VARCHAR(64),
    vendor_id VARCHAR(64),
    expense_id VARCHAR(64),
    payable_id VARCHAR(64),
    allocation_id VARCHAR(64),
    reversal_of_posting_id VARCHAR(64) REFERENCES business_ledger_postings(id) ON DELETE RESTRICT,
    is_reversed BOOLEAN NOT NULL DEFAULT FALSE,
    reversal_reason TEXT,
    reversed_by VARCHAR(64),
    reversed_at BIGINT,
    correlation_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    checksum VARCHAR(128),
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_blp_debit_non_negative CHECK (debit_amount >= 0),
    CONSTRAINT chk_blp_credit_non_negative CHECK (credit_amount >= 0),
    CONSTRAINT chk_blp_amount_valid CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (credit_amount > 0 AND debit_amount = 0)
    ),
    CONSTRAINT chk_blp_currency CHECK (LENGTH(currency) = 3)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blp_tenant_project_number
    ON business_ledger_postings(tenant_id, project_id, posting_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blp_tenant_project_idempotency
    ON business_ledger_postings(tenant_id, project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_blp_tenant_source_posting
    ON business_ledger_postings(tenant_id, project_id, source_type, source_id, posting_type)
    WHERE reversal_of_posting_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_blp_tenant_posting_date
    ON business_ledger_postings(tenant_id, posting_date);

CREATE INDEX IF NOT EXISTS idx_blp_tenant_source
    ON business_ledger_postings(tenant_id, source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_blp_tenant_account_category
    ON business_ledger_postings(tenant_id, account_category);

CREATE INDEX IF NOT EXISTS idx_blp_tenant_job
    ON business_ledger_postings(tenant_id, job_id)
    WHERE job_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blp_tenant_vendor
    ON business_ledger_postings(tenant_id, vendor_id)
    WHERE vendor_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blp_tenant_expense
    ON business_ledger_postings(tenant_id, expense_id)
    WHERE expense_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blp_tenant_payable
    ON business_ledger_postings(tenant_id, payable_id)
    WHERE payable_id IS NOT NULL;


-- 2. Business Cost Allocations Table (Job / Project Analytical Cost Attribution)
CREATE TABLE IF NOT EXISTS business_cost_allocations (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL DEFAULT 'PRJ-001',
    allocation_number VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    ledger_posting_id VARCHAR(64) REFERENCES business_ledger_postings(id) ON DELETE RESTRICT,
    job_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64),
    cost_category VARCHAR(64) NOT NULL,
    allocated_amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    allocation_date BIGINT NOT NULL,
    reason TEXT,
    is_reversed BOOLEAN NOT NULL DEFAULT FALSE,
    reversal_reason TEXT,
    reversed_by VARCHAR(64),
    reversed_at BIGINT,
    correlation_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_bca_amount CHECK (allocated_amount > 0),
    CONSTRAINT chk_bca_currency CHECK (LENGTH(currency) = 3)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bca_tenant_project_number
    ON business_cost_allocations(tenant_id, project_id, allocation_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bca_tenant_idempotency
    ON business_cost_allocations(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bca_tenant_job
    ON business_cost_allocations(tenant_id, job_id);

CREATE INDEX IF NOT EXISTS idx_bca_tenant_source
    ON business_cost_allocations(tenant_id, source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_bca_tenant_vendor
    ON business_cost_allocations(tenant_id, vendor_id)
    WHERE vendor_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bca_tenant_posting
    ON business_cost_allocations(tenant_id, ledger_posting_id)
    WHERE ledger_posting_id IS NOT NULL;


-- 3. Business Ledger Audit Events Table (Append-Only Audit Trail)
CREATE TABLE IF NOT EXISTS business_ledger_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(64) NOT NULL DEFAULT 'PRJ-001',
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    timestamp BIGINT NOT NULL,
    source_type VARCHAR(32),
    source_id VARCHAR(64),
    posting_id VARCHAR(64),
    allocation_id VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    previous_state TEXT,
    new_state TEXT,
    amount NUMERIC(18, 4),
    reason TEXT,
    correlation_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    checksum VARCHAR(128),
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_bla_audit_tenant_posting
    ON business_ledger_audit_events(tenant_id, posting_id, timestamp)
    WHERE posting_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bla_audit_tenant_source
    ON business_ledger_audit_events(tenant_id, source_type, source_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_bla_audit_tenant_allocation
    ON business_ledger_audit_events(tenant_id, allocation_id, timestamp)
    WHERE allocation_id IS NOT NULL;


-- ====================================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ====================================================================================

-- 1. business_ledger_postings RLS
ALTER TABLE business_ledger_postings ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_ledger_postings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_business_ledger_postings ON business_ledger_postings;
CREATE POLICY tenant_isolation_business_ledger_postings ON business_ledger_postings
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

-- 2. business_cost_allocations RLS
ALTER TABLE business_cost_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_allocations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_business_cost_allocations ON business_cost_allocations;
CREATE POLICY tenant_isolation_business_cost_allocations ON business_cost_allocations
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

-- 3. business_ledger_audit_events RLS
ALTER TABLE business_ledger_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_ledger_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_business_ledger_audits ON business_ledger_audit_events;
CREATE POLICY tenant_isolation_business_ledger_audits ON business_ledger_audit_events
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
