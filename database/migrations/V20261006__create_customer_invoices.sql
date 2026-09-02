-- =============================================================================
-- Migration: V20261006__create_customer_invoices.sql
-- Module 14 Step 02: Customer Invoice & Receivable Management Foundation
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_invoices (
    invoice_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_financial_account_id VARCHAR(64) NOT NULL,
    invoice_number VARCHAR(64) NOT NULL,
    source_order_id VARCHAR(64),
    source_job_id VARCHAR(64),
    issue_date BIGINT,
    due_date BIGINT,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    subtotal NUMERIC(18, 4) NOT NULL DEFAULT 0,
    discount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    tax NUMERIC(18, 4) NOT NULL DEFAULT 0,
    adjustment NUMERIC(18, 4) NOT NULL DEFAULT 0,
    grand_total NUMERIC(18, 4) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    due_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    cancellation_reason TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_customer_invoice_num UNIQUE (tenant_id, invoice_number)
);

CREATE INDEX IF NOT EXISTS idx_ci_tenant_customer
    ON customer_invoices(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_ci_tenant_account
    ON customer_invoices(tenant_id, customer_financial_account_id);

CREATE INDEX IF NOT EXISTS idx_ci_tenant_status
    ON customer_invoices(tenant_id, project_id, status);

CREATE INDEX IF NOT EXISTS idx_ci_order_job
    ON customer_invoices(tenant_id, source_order_id, source_job_id);

CREATE TABLE IF NOT EXISTS customer_invoice_lines (
    line_id VARCHAR(64) PRIMARY KEY,
    invoice_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    product_id VARCHAR(64),
    job_id VARCHAR(64),
    quantity NUMERIC(18, 4) NOT NULL DEFAULT 1,
    unit VARCHAR(32) NOT NULL DEFAULT 'PCS',
    unit_price NUMERIC(18, 4) NOT NULL DEFAULT 0,
    discount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    tax NUMERIC(18, 4) NOT NULL DEFAULT 0,
    line_total NUMERIC(18, 4) NOT NULL DEFAULT 0,
    notes TEXT,
    line_order INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ci_lines_lookup
    ON customer_invoice_lines(tenant_id, invoice_id, line_order);

CREATE TABLE IF NOT EXISTS customer_invoice_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    invoice_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    reason TEXT,
    occurred_at BIGINT NOT NULL,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_ci_audit_lookup
    ON customer_invoice_audit_events(tenant_id, invoice_id, occurred_at DESC);

-- Enable and Force Row Level Security (RLS)
ALTER TABLE customer_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_invoices FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_invoices ON customer_invoices
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_invoice_lines ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_invoice_lines FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_invoice_lines ON customer_invoice_lines
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_invoice_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_invoice_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_invoice_audit_events ON customer_invoice_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
