-- ============================================================================
-- Migration: V20260929__create_vendor_portal_invoice_billing_payment_workspace.sql
-- Module 13: Vendor Portal -> Step 06: Vendor Invoice, Billing & Payment Workspace
-- ============================================================================

-- 1. Vendor Portal Invoice Submissions (Vendor-originated draft/submitted invoices)
CREATE TABLE IF NOT EXISTS vendor_portal_invoice_submissions (
    submission_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(128) NOT NULL,
    vendor_invoice_number VARCHAR(128) NOT NULL,
    invoice_date BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    subtotal_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    tax_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    discount_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    shipping_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    other_charges NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    notes TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    canonical_invoice_id VARCHAR(64),
    rejection_reason TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    submitted_at BIGINT,
    submitted_by VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_vpis_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'CONVERTED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_vpis_total CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_vpis_tenant_proj_vnd ON vendor_portal_invoice_submissions(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpis_po ON vendor_portal_invoice_submissions(tenant_id, purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_vpis_status ON vendor_portal_invoice_submissions(tenant_id, status);

-- 2. Vendor Portal Invoice Submission Items
CREATE TABLE IF NOT EXISTS vendor_portal_invoice_submission_items (
    item_id VARCHAR(64) PRIMARY KEY,
    submission_id VARCHAR(64) NOT NULL REFERENCES vendor_portal_invoice_submissions(submission_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    purchase_order_item_id VARCHAR(64) NOT NULL,
    delivery_receipt_item_id VARCHAR(64),
    item_name VARCHAR(256) NOT NULL,
    item_code VARCHAR(128),
    invoiced_quantity NUMERIC(19, 4) NOT NULL,
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'PIECE',
    unit_price NUMERIC(19, 4) NOT NULL,
    tax_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    line_total NUMERIC(19, 4) NOT NULL,
    remarks TEXT,
    CONSTRAINT chk_vpisi_qty CHECK (invoiced_quantity > 0),
    CONSTRAINT chk_vpisi_price CHECK (unit_price >= 0),
    CONSTRAINT chk_vpisi_total CHECK (line_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_vpisi_submission ON vendor_portal_invoice_submission_items(tenant_id, submission_id);

-- 3. Vendor Portal Invoice Responses (Responses to match exceptions / clarifications / rejections)
CREATE TABLE IF NOT EXISTS vendor_portal_invoice_responses (
    response_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    exception_id VARCHAR(64),
    response_type VARCHAR(32) NOT NULL,
    comment TEXT NOT NULL,
    proposed_correction TEXT,
    evidence_references TEXT,
    responded_by VARCHAR(64) NOT NULL,
    responded_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_vpir_type CHECK (response_type IN ('CLARIFY_EXCEPTION', 'ACCEPT_VARIANCE', 'DISPUTE_VARIANCE', 'PROPOSE_CORRECTION', 'SUBMIT_ADDITIONAL_DOCS'))
);

CREATE INDEX IF NOT EXISTS idx_vpir_tenant_invoice ON vendor_portal_invoice_responses(tenant_id, project_id, invoice_id);

-- 4. Vendor Portal Financial Evidence
CREATE TABLE IF NOT EXISTS vendor_portal_financial_evidence (
    evidence_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    evidence_type VARCHAR(64) NOT NULL,
    filename VARCHAR(256) NOT NULL,
    file_reference VARCHAR(512) NOT NULL,
    file_hash VARCHAR(128),
    mime_type VARCHAR(128) NOT NULL DEFAULT 'application/pdf',
    size_bytes BIGINT NOT NULL,
    uploaded_by VARCHAR(64) NOT NULL,
    uploaded_at BIGINT NOT NULL,
    CONSTRAINT chk_vpfe_type CHECK (evidence_type IN ('INVOICE_DOCUMENT', 'TAX_DOCUMENT', 'DELIVERY_PROOF', 'CLARIFICATION_ATTACHMENT', 'PAYMENT_REMITTANCE', 'DISPUTE_EVIDENCE')),
    CONSTRAINT chk_vpfe_size CHECK (size_bytes > 0)
);

CREATE INDEX IF NOT EXISTS idx_vpfe_entity ON vendor_portal_financial_evidence(tenant_id, project_id, entity_type, entity_id);

-- 5. Vendor Portal Invoice & Financial Audit Events
CREATE TABLE IF NOT EXISTS vendor_portal_invoice_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    payload TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vpiae_target ON vendor_portal_invoice_audit_events(tenant_id, project_id, target_type, target_id);

-- ============================================================================
-- Row-Level Security (RLS) Configuration
-- ============================================================================

ALTER TABLE vendor_portal_invoice_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_invoice_submissions FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_portal_invoice_submission_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_invoice_submission_items FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_portal_invoice_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_invoice_responses FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_portal_financial_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_financial_evidence FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_portal_invoice_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_invoice_audit_events FORCE ROW LEVEL SECURITY;

CREATE POLICY vpis_tenant_isolation_policy ON vendor_portal_invoice_submissions
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true));

CREATE POLICY vpisi_tenant_isolation_policy ON vendor_portal_invoice_submission_items
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true));

CREATE POLICY vpir_tenant_isolation_policy ON vendor_portal_invoice_responses
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true));

CREATE POLICY vpfe_tenant_isolation_policy ON vendor_portal_financial_evidence
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true));

CREATE POLICY vpiae_tenant_isolation_policy ON vendor_portal_invoice_audit_events
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true));
