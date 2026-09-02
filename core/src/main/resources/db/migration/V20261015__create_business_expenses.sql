-- =============================================================================
-- Migration: V20261015__create_business_expenses.sql
-- Module 15 Step 01: Business Expense Management Foundation
-- =============================================================================

-- 1. Business Expense Categories Table
CREATE TABLE IF NOT EXISTS business_expense_categories (
    category_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_bec_tenant_proj_code
    ON business_expense_categories(tenant_id, project_id, code)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_bec_tenant_proj_active
    ON business_expense_categories(tenant_id, project_id, is_active, sort_order ASC);

-- 2. Business Expenses Table
CREATE TABLE IF NOT EXISTS business_expenses (
    expense_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    branch_id VARCHAR(64),
    location_id VARCHAR(64),
    expense_number VARCHAR(64) NOT NULL,
    expense_category_id VARCHAR(64) NOT NULL,
    amount DECIMAL(18, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    expense_date BIGINT NOT NULL,
    payment_method VARCHAR(32) NOT NULL DEFAULT 'CASH',
    payment_reference VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    vendor_id VARCHAR(64),
    job_id VARCHAR(64),
    description TEXT NOT NULL,
    notes TEXT,
    attachment_url TEXT,
    attachment_metadata TEXT,
    idempotency_key VARCHAR(128),
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    submitted_by VARCHAR(64),
    submitted_at BIGINT,
    approved_by VARCHAR(64),
    approved_at BIGINT,
    rejected_by VARCHAR(64),
    rejected_at BIGINT,
    rejection_reason TEXT,
    cancelled_by VARCHAR(64),
    cancelled_at BIGINT,
    cancellation_reason TEXT,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_be_tenant_proj_num
    ON business_expenses(tenant_id, project_id, expense_number);

CREATE UNIQUE INDEX IF NOT EXISTS idx_be_tenant_proj_idempotency
    ON business_expenses(tenant_id, project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_be_tenant_proj_date
    ON business_expenses(tenant_id, project_id, expense_date DESC);

CREATE INDEX IF NOT EXISTS idx_be_tenant_proj_status
    ON business_expenses(tenant_id, project_id, status);

CREATE INDEX IF NOT EXISTS idx_be_tenant_proj_cat
    ON business_expenses(tenant_id, project_id, expense_category_id);

CREATE INDEX IF NOT EXISTS idx_be_tenant_proj_vendor
    ON business_expenses(tenant_id, project_id, vendor_id)
    WHERE vendor_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_be_tenant_proj_job
    ON business_expenses(tenant_id, project_id, job_id)
    WHERE job_id IS NOT NULL;

-- 3. Business Expense Audit Events Table (Append-Only)
CREATE TABLE IF NOT EXISTS business_expense_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    expense_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    correlation_id VARCHAR(64),
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    reason TEXT,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_be_audit_expense_time
    ON business_expense_audit_events(tenant_id, project_id, expense_id, timestamp ASC);

-- =============================================================================
-- Row Level Security (RLS) Enforcement
-- =============================================================================

ALTER TABLE business_expense_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_expense_categories FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS business_expense_categories_tenant_isolation ON business_expense_categories;
CREATE POLICY business_expense_categories_tenant_isolation ON business_expense_categories
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE business_expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_expenses FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS business_expenses_tenant_isolation ON business_expenses;
CREATE POLICY business_expenses_tenant_isolation ON business_expenses
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE business_expense_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_expense_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS business_expense_audit_events_tenant_isolation ON business_expense_audit_events;
CREATE POLICY business_expense_audit_events_tenant_isolation ON business_expense_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
