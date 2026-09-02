-- =========================================================================
-- SUCHARU PRO ERP DATABASE MIGRATION
-- V20260919__create_vendor_purchase_orders.sql
-- Module 12: Vendor Management — Step 05: Purchase Order / Vendor Order Management
-- =========================================================================

-- 1. Vendor Purchase Orders Table
CREATE TABLE IF NOT EXISTS vendor_purchase_orders (
    project_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    order_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    requested_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64),
    approved_at TIMESTAMP WITH TIME ZONE,
    issued_by VARCHAR(64),
    issued_at TIMESTAMP WITH TIME ZONE,
    expected_delivery_date TIMESTAMP WITH TIME ZONE,
    delivery_location VARCHAR(300),
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    subtotal NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    source_reference_type VARCHAR(64),
    source_reference_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_purchase_orders PRIMARY KEY (project_id, purchase_order_id),
    CONSTRAINT uq_vendor_purchase_order_number UNIQUE (project_id, order_number),
    CONSTRAINT fk_vendor_purchase_orders_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_purchase_orders_lookup ON vendor_purchase_orders(project_id, vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vendor_purchase_orders_date ON vendor_purchase_orders(project_id, order_date);
CREATE INDEX IF NOT EXISTS idx_vendor_purchase_orders_due ON vendor_purchase_orders(project_id, expected_delivery_date);
CREATE INDEX IF NOT EXISTS idx_vendor_purchase_orders_source ON vendor_purchase_orders(project_id, source_reference_type, source_reference_id);

ALTER TABLE vendor_purchase_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_purchase_orders FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_purchase_orders_tenant_isolation ON vendor_purchase_orders;
CREATE POLICY vendor_purchase_orders_tenant_isolation ON vendor_purchase_orders
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 2. Vendor Purchase Order Items Table
CREATE TABLE IF NOT EXISTS vendor_purchase_order_items (
    project_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    vendor_service_rate_id VARCHAR(64),
    capability_type VARCHAR(64),
    item_description VARCHAR(1000) NOT NULL,
    item_code VARCHAR(64),
    quantity NUMERIC(14, 2) NOT NULL DEFAULT 1.00,
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'PIECE',
    unit_rate NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    pricing_method VARCHAR(32) NOT NULL DEFAULT 'PER_UNIT',
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    discount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    expected_delivery_date TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    source_work_order_id VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_purchase_order_items PRIMARY KEY (project_id, item_id),
    CONSTRAINT fk_vendor_purchase_order_items_order FOREIGN KEY (project_id, purchase_order_id)
        REFERENCES vendor_purchase_orders(project_id, purchase_order_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_po_items_order ON vendor_purchase_order_items(project_id, purchase_order_id);

ALTER TABLE vendor_purchase_order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_purchase_order_items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_purchase_order_items_tenant_isolation ON vendor_purchase_order_items;
CREATE POLICY vendor_purchase_order_items_tenant_isolation ON vendor_purchase_order_items
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 3. Vendor Purchase Order Revisions Table
CREATE TABLE IF NOT EXISTS vendor_purchase_order_revisions (
    project_id VARCHAR(64) NOT NULL,
    revision_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    revision_number INT NOT NULL,
    previous_total_amount NUMERIC(14, 2) NOT NULL,
    new_total_amount NUMERIC(14, 2) NOT NULL,
    change_summary TEXT NOT NULL,
    revised_by VARCHAR(64) NOT NULL,
    revised_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_vendor_purchase_order_revisions PRIMARY KEY (project_id, revision_id),
    CONSTRAINT fk_vendor_po_revisions_order FOREIGN KEY (project_id, purchase_order_id)
        REFERENCES vendor_purchase_orders(project_id, purchase_order_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_po_revisions_order ON vendor_purchase_order_revisions(project_id, purchase_order_id, revision_number);

ALTER TABLE vendor_purchase_order_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_purchase_order_revisions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_purchase_order_revisions_tenant_isolation ON vendor_purchase_order_revisions;
CREATE POLICY vendor_purchase_order_revisions_tenant_isolation ON vendor_purchase_order_revisions
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 4. Vendor Purchase Order Audits Table
CREATE TABLE IF NOT EXISTS vendor_purchase_order_audits (
    project_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    CONSTRAINT pk_vendor_purchase_order_audits PRIMARY KEY (project_id, audit_id),
    CONSTRAINT fk_vendor_po_audits_order FOREIGN KEY (project_id, purchase_order_id)
        REFERENCES vendor_purchase_orders(project_id, purchase_order_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_po_audits_order ON vendor_purchase_order_audits(project_id, purchase_order_id, occurred_at);

ALTER TABLE vendor_purchase_order_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_purchase_order_audits FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_purchase_order_audits_tenant_isolation ON vendor_purchase_order_audits;
CREATE POLICY vendor_purchase_order_audits_tenant_isolation ON vendor_purchase_order_audits
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));
