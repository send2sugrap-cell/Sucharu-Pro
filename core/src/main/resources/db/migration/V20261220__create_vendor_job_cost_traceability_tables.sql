-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- VENDOR-BASED JOB COST & TRACEABILITY DDL SPECIFICATION (V20261220)
-- ====================================================================================

CREATE TABLE job_vendor_cost_entries (
    cost_entry_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    job_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50),
    work_context VARCHAR(50) NOT NULL CHECK (work_context IN ('PAPER_PURCHASE', 'CTP_PREPRESS', 'PRINTING', 'FINISHING_LAMINATION', 'FINISHING_FOLDING', 'FINISHING_BINDING', 'PACKAGING', 'LOGISTICS_TRANSPORT', 'MAINTENANCE', 'OTHER')),
    vendor_attribution_type VARCHAR(30) NOT NULL DEFAULT 'VENDOR' CHECK (vendor_attribution_type IN ('VENDOR', 'NO_VENDOR')),
    vendor_id VARCHAR(50) REFERENCES vendors(vendor_id),
    vendor_code VARCHAR(50),
    vendor_name VARCHAR(150),
    amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (amount >= 0.00),
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    invoice_bill_ref VARCHAR(100),
    payable_id VARCHAR(50),
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'NOT_APPLICABLE')),
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, cost_entry_id)
);

CREATE INDEX idx_job_vendor_cost_job ON job_vendor_cost_entries (project_id, job_id);
CREATE INDEX idx_job_vendor_cost_vendor ON job_vendor_cost_entries (project_id, vendor_id);

ALTER TABLE job_vendor_cost_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_vendor_cost_entries FORCE ROW LEVEL SECURITY;

CREATE POLICY job_vendor_cost_tenant_isolation ON job_vendor_cost_entries
    FOR ALL USING (project_id = CURRENT_SETTING('app.current_project_id', true));
