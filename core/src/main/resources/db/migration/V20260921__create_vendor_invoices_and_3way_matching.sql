-- =========================================================================
-- SUCHARU PRO ERP DATABASE MIGRATION
-- V20260921__create_vendor_invoices_and_3way_matching.sql
-- Module 12: Vendor Management — Step 07: Vendor Invoice & 3-Way Matching
-- =========================================================================

-- 1. Vendor Invoices Table
CREATE TABLE IF NOT EXISTS vendor_invoices (
    project_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    vendor_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    invoice_number VARCHAR(64) NOT NULL,
    vendor_invoice_number VARCHAR(64) NOT NULL,
    invoice_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    subtotal NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    shipping_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    other_charges NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    match_status VARCHAR(32) NOT NULL DEFAULT 'NOT_MATCHED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_invoices PRIMARY KEY (project_id, invoice_id),
    CONSTRAINT uq_vendor_invoice_number UNIQUE (project_id, invoice_number),
    CONSTRAINT uq_vendor_vendor_invoice UNIQUE (project_id, vendor_id, vendor_invoice_number),
    CONSTRAINT fk_vendor_invoices_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE,
    CONSTRAINT fk_vendor_invoices_po FOREIGN KEY (project_id, purchase_order_id)
        REFERENCES vendor_purchase_orders(project_id, purchase_order_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_invoices_vendor ON vendor_invoices(project_id, vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vendor_invoices_po ON vendor_invoices(project_id, purchase_order_id, status);
CREATE INDEX IF NOT EXISTS idx_vendor_invoices_status ON vendor_invoices(project_id, status, match_status);

ALTER TABLE vendor_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_invoices FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_invoices_tenant_isolation ON vendor_invoices;
CREATE POLICY vendor_invoices_tenant_isolation ON vendor_invoices
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 2. Vendor Invoice Items Table
CREATE TABLE IF NOT EXISTS vendor_invoice_items (
    project_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    purchase_order_item_id VARCHAR(64) NOT NULL,
    delivery_receipt_item_id VARCHAR(64),
    description VARCHAR(1000) NOT NULL,
    quantity NUMERIC(14, 2) NOT NULL DEFAULT 1.00,
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'PIECE',
    unit_price NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    tax_rate NUMERIC(6, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    sequence INT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_invoice_items PRIMARY KEY (project_id, item_id),
    CONSTRAINT fk_vendor_invoice_items_inv FOREIGN KEY (project_id, invoice_id)
        REFERENCES vendor_invoices(project_id, invoice_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_inv_items_inv ON vendor_invoice_items(project_id, invoice_id);

ALTER TABLE vendor_invoice_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_invoice_items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_invoice_items_tenant_isolation ON vendor_invoice_items;
CREATE POLICY vendor_invoice_items_tenant_isolation ON vendor_invoice_items
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 3. Vendor Invoice Matches Table
CREATE TABLE IF NOT EXISTS vendor_invoice_matches (
    project_id VARCHAR(64) NOT NULL,
    match_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    match_status VARCHAR(32) NOT NULL DEFAULT 'NOT_MATCHED',
    matched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    matched_by VARCHAR(64) NOT NULL DEFAULT 'system',
    subtotal_variance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    quantity_variance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    price_variance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    tax_variance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_variance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    currency_mismatch BOOLEAN NOT NULL DEFAULT FALSE,
    vendor_mismatch BOOLEAN NOT NULL DEFAULT FALSE,
    unmatched_line_count INT NOT NULL DEFAULT 0,
    exception_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_invoice_matches PRIMARY KEY (project_id, match_id),
    CONSTRAINT fk_vendor_invoice_matches_inv FOREIGN KEY (project_id, invoice_id)
        REFERENCES vendor_invoices(project_id, invoice_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_inv_matches_inv ON vendor_invoice_matches(project_id, invoice_id);

ALTER TABLE vendor_invoice_matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_invoice_matches FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_invoice_matches_tenant_isolation ON vendor_invoice_matches;
CREATE POLICY vendor_invoice_matches_tenant_isolation ON vendor_invoice_matches
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 4. Vendor Invoice Match Lines Table
CREATE TABLE IF NOT EXISTS vendor_invoice_match_lines (
    project_id VARCHAR(64) NOT NULL,
    match_line_id VARCHAR(64) NOT NULL,
    match_id VARCHAR(64) NOT NULL,
    invoice_item_id VARCHAR(64) NOT NULL,
    purchase_order_item_id VARCHAR(64) NOT NULL,
    delivery_receipt_item_id VARCHAR(64),
    description VARCHAR(1000) NOT NULL,
    ordered_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    received_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    invoiced_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    ordered_unit_price NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    invoiced_unit_price NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    quantity_variance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    price_variance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    amount_variance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    match_status VARCHAR(32) NOT NULL DEFAULT 'NOT_MATCHED',
    exception_reason TEXT,
    CONSTRAINT pk_vendor_invoice_match_lines PRIMARY KEY (project_id, match_line_id),
    CONSTRAINT fk_vendor_inv_match_lines_match FOREIGN KEY (project_id, match_id)
        REFERENCES vendor_invoice_matches(project_id, match_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_inv_match_lines ON vendor_invoice_match_lines(project_id, match_id);

ALTER TABLE vendor_invoice_match_lines ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_invoice_match_lines FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_invoice_match_lines_tenant_isolation ON vendor_invoice_match_lines;
CREATE POLICY vendor_invoice_match_lines_tenant_isolation ON vendor_invoice_match_lines
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 5. Vendor Invoice Exceptions Table
CREATE TABLE IF NOT EXISTS vendor_invoice_exceptions (
    project_id VARCHAR(64) NOT NULL,
    exception_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    match_id VARCHAR(64) NOT NULL,
    exception_type VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by VARCHAR(64),
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_vendor_invoice_exceptions PRIMARY KEY (project_id, exception_id),
    CONSTRAINT fk_vendor_inv_exceptions_inv FOREIGN KEY (project_id, invoice_id)
        REFERENCES vendor_invoices(project_id, invoice_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_inv_exceptions ON vendor_invoice_exceptions(project_id, invoice_id, resolved);

ALTER TABLE vendor_invoice_exceptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_invoice_exceptions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_invoice_exceptions_tenant_isolation ON vendor_invoice_exceptions;
CREATE POLICY vendor_invoice_exceptions_tenant_isolation ON vendor_invoice_exceptions
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 6. Vendor Invoice Audits Table
CREATE TABLE IF NOT EXISTS vendor_invoice_audits (
    project_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    CONSTRAINT pk_vendor_invoice_audits PRIMARY KEY (project_id, audit_id),
    CONSTRAINT fk_vendor_inv_audits_inv FOREIGN KEY (project_id, invoice_id)
        REFERENCES vendor_invoices(project_id, invoice_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_inv_audits ON vendor_invoice_audits(project_id, invoice_id, occurred_at);

ALTER TABLE vendor_invoice_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_invoice_audits FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_invoice_audits_tenant_isolation ON vendor_invoice_audits;
CREATE POLICY vendor_invoice_audits_tenant_isolation ON vendor_invoice_audits
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));
