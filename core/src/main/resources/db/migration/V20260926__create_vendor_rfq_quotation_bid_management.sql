-- =========================================================================
-- SUCHARU PRO ERP
-- Migration: V20260926__create_vendor_rfq_quotation_bid_management.sql
-- Module 13 Step 03: Vendor RFQ / Quotation & Bid Management
-- =========================================================================

-- 1. Vendor RFQs Table
CREATE TABLE IF NOT EXISTS vendor_rfqs (
    rfq_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    rfq_number VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    requested_by VARCHAR(64) NOT NULL,
    issue_date BIGINT NOT NULL,
    response_deadline BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    delivery_requirements TEXT,
    payment_terms TEXT,
    shipping_terms TEXT,
    required_capabilities TEXT, -- Comma-separated or JSON string
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    award_winning_vendor_id VARCHAR(64),
    award_winning_quotation_id VARCHAR(64),
    award_reason TEXT,
    award_amount NUMERIC(15, 2),
    award_by VARCHAR(64),
    award_at BIGINT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_rfq_tenant_number UNIQUE (tenant_id, rfq_number)
);

CREATE INDEX IF NOT EXISTS idx_rfq_tenant_proj ON vendor_rfqs(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_rfq_status_deadline ON vendor_rfqs(status, response_deadline);

-- 2. Vendor RFQ Items Table
CREATE TABLE IF NOT EXISTS vendor_rfq_items (
    rfq_item_id VARCHAR(64) PRIMARY KEY,
    rfq_id VARCHAR(64) NOT NULL REFERENCES vendor_rfqs(rfq_id) ON DELETE CASCADE,
    sequence_number INT NOT NULL,
    item_code VARCHAR(64),
    description VARCHAR(255) NOT NULL,
    required_capability_type VARCHAR(64),
    quantity NUMERIC(15, 4) NOT NULL,
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'UNIT',
    target_unit_price NUMERIC(15, 2),
    target_delivery_date BIGINT,
    specifications TEXT,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_rfq_items_rfq_id ON vendor_rfq_items(rfq_id);

-- 3. Vendor RFQ Invitations Table
CREATE TABLE IF NOT EXISTS vendor_rfq_invitations (
    invitation_id VARCHAR(64) PRIMARY KEY,
    rfq_id VARCHAR(64) NOT NULL REFERENCES vendor_rfqs(rfq_id) ON DELETE CASCADE,
    vendor_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INVITED',
    invited_at BIGINT NOT NULL,
    viewed_at BIGINT,
    acknowledged_at BIGINT,
    responded_at BIGINT,
    decline_reason TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_rfq_invitation_vendor UNIQUE (rfq_id, vendor_id)
);

CREATE INDEX IF NOT EXISTS idx_rfq_inv_tenant_vendor ON vendor_rfq_invitations(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_rfq_inv_status ON vendor_rfq_invitations(status);

-- 4. Vendor Quotations Table
CREATE TABLE IF NOT EXISTS vendor_quotations (
    quotation_id VARCHAR(64) PRIMARY KEY,
    rfq_id VARCHAR(64) NOT NULL REFERENCES vendor_rfqs(rfq_id) ON DELETE CASCADE,
    invitation_id VARCHAR(64) NOT NULL REFERENCES vendor_rfq_invitations(invitation_id),
    vendor_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    quotation_number VARCHAR(64) NOT NULL,
    vendor_reference_number VARCHAR(64),
    revision_number INT NOT NULL DEFAULT 1,
    currency VARCHAR(16) NOT NULL DEFAULT 'BDT',
    validity_period_days INT NOT NULL DEFAULT 30,
    payment_terms TEXT,
    delivery_lead_time_days INT NOT NULL DEFAULT 0,
    shipping_terms TEXT,
    notes TEXT,
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    grand_total NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    submitted_at BIGINT,
    submitted_by VARCHAR(64),
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_quotation_tenant_number UNIQUE (tenant_id, quotation_number)
);

CREATE INDEX IF NOT EXISTS idx_quotation_tenant_rfq ON vendor_quotations(tenant_id, rfq_id);
CREATE INDEX IF NOT EXISTS idx_quotation_tenant_vendor ON vendor_quotations(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_quotation_status ON vendor_quotations(status);

-- 5. Vendor Quotation Items Table
CREATE TABLE IF NOT EXISTS vendor_quotation_items (
    quotation_item_id VARCHAR(64) PRIMARY KEY,
    quotation_id VARCHAR(64) NOT NULL REFERENCES vendor_quotations(quotation_id) ON DELETE CASCADE,
    rfq_item_id VARCHAR(64) NOT NULL REFERENCES vendor_rfq_items(rfq_item_id),
    quantity NUMERIC(15, 4) NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(15, 2) NOT NULL,
    delivery_lead_time_days INT NOT NULL DEFAULT 0,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_quotation_rfq_item UNIQUE (quotation_id, rfq_item_id)
);

CREATE INDEX IF NOT EXISTS idx_quotation_items_quotation ON vendor_quotation_items(quotation_id);

-- 6. Vendor Quotation Revisions Table
CREATE TABLE IF NOT EXISTS vendor_quotation_revisions (
    revision_id VARCHAR(64) PRIMARY KEY,
    quotation_id VARCHAR(64) NOT NULL REFERENCES vendor_quotations(quotation_id) ON DELETE CASCADE,
    rfq_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    revision_number INT NOT NULL,
    reason_for_revision TEXT NOT NULL,
    snapshot_subtotal NUMERIC(15, 2) NOT NULL,
    snapshot_grand_total NUMERIC(15, 2) NOT NULL,
    items_snapshot_json TEXT NOT NULL,
    revised_by VARCHAR(64) NOT NULL,
    revised_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quotation_rev_quotation ON vendor_quotation_revisions(quotation_id, revision_number);

-- 7. Vendor RFQ Clarifications Table
CREATE TABLE IF NOT EXISTS vendor_rfq_clarifications (
    clarification_id VARCHAR(64) PRIMARY KEY,
    rfq_id VARCHAR(64) NOT NULL REFERENCES vendor_rfqs(rfq_id) ON DELETE CASCADE,
    vendor_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    asked_by VARCHAR(64) NOT NULL,
    asked_at BIGINT NOT NULL,
    answer TEXT,
    answered_by VARCHAR(64),
    answered_at BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    visibility VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_TO_ALL_INVITED',
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_rfq_clar_rfq_id ON vendor_rfq_clarifications(rfq_id);

-- 8. Vendor RFQ Evaluations Table
CREATE TABLE IF NOT EXISTS vendor_rfq_evaluations (
    evaluation_id VARCHAR(64) PRIMARY KEY,
    rfq_id VARCHAR(64) NOT NULL REFERENCES vendor_rfqs(rfq_id) ON DELETE CASCADE,
    quotation_id VARCHAR(64) NOT NULL REFERENCES vendor_quotations(quotation_id) ON DELETE CASCADE,
    vendor_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    evaluator_user_id VARCHAR(64) NOT NULL,
    scores_json TEXT NOT NULL,
    total_score NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    decision VARCHAR(32) NOT NULL DEFAULT 'UNDER_CONSIDERATION',
    remarks TEXT,
    evaluated_at BIGINT NOT NULL,
    approved_by VARCHAR(64),
    approved_at BIGINT,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_rfq_eval_quotation UNIQUE (rfq_id, quotation_id)
);

CREATE INDEX IF NOT EXISTS idx_rfq_eval_rfq_id ON vendor_rfq_evaluations(rfq_id);

-- 9. Vendor RFQ Audit Events Table
CREATE TABLE IF NOT EXISTS vendor_rfq_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    rfq_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64),
    quotation_id VARCHAR(64),
    actor_user_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    details TEXT,
    ip_address VARCHAR(64),
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rfq_audit_rfq ON vendor_rfq_audit_events(rfq_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_rfq_audit_tenant_actor ON vendor_rfq_audit_events(tenant_id, actor_user_id);

-- 10. Enable & Force Row Level Security
ALTER TABLE vendor_rfqs ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_rfqs FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_rfq_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_rfq_items FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_rfq_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_rfq_invitations FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_quotations ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_quotations FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_quotation_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_quotation_items FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_quotation_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_quotation_revisions FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_rfq_clarifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_rfq_clarifications FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_rfq_evaluations ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_rfq_evaluations FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_rfq_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_rfq_audit_events FORCE ROW LEVEL SECURITY;
