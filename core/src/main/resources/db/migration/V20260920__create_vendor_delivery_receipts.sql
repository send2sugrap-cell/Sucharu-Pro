-- =========================================================================
-- SUCHARU PRO ERP DATABASE MIGRATION
-- V20260920__create_vendor_delivery_receipts.sql
-- Module 12: Vendor Management — Step 06: Vendor Delivery / Receiving & Purchase Order Receipt Integration
-- =========================================================================

-- 1. Vendor Delivery Receipts Table
CREATE TABLE IF NOT EXISTS vendor_delivery_receipts (
    project_id VARCHAR(64) NOT NULL,
    delivery_receipt_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'TENANT-001',
    receipt_number VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    vendor_delivery_reference VARCHAR(100),
    receipt_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_at TIMESTAMP WITH TIME ZONE,
    received_by VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    warehouse_id VARCHAR(64),
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_delivery_receipts PRIMARY KEY (project_id, delivery_receipt_id),
    CONSTRAINT uq_vendor_delivery_receipt_number UNIQUE (project_id, receipt_number),
    CONSTRAINT fk_vendor_delivery_receipts_order FOREIGN KEY (project_id, purchase_order_id)
        REFERENCES vendor_purchase_orders(project_id, purchase_order_id) ON DELETE CASCADE,
    CONSTRAINT fk_vendor_delivery_receipts_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_delivery_receipts_po ON vendor_delivery_receipts(project_id, purchase_order_id, status);
CREATE INDEX IF NOT EXISTS idx_vendor_delivery_receipts_vendor ON vendor_delivery_receipts(project_id, vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vendor_delivery_receipts_date ON vendor_delivery_receipts(project_id, receipt_date);

ALTER TABLE vendor_delivery_receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_delivery_receipts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_delivery_receipts_tenant_isolation ON vendor_delivery_receipts;
CREATE POLICY vendor_delivery_receipts_tenant_isolation ON vendor_delivery_receipts
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 2. Vendor Delivery Receipt Items Table
CREATE TABLE IF NOT EXISTS vendor_delivery_receipt_items (
    project_id VARCHAR(64) NOT NULL,
    receipt_item_id VARCHAR(64) NOT NULL,
    delivery_receipt_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    purchase_order_item_id VARCHAR(64) NOT NULL,
    item_description VARCHAR(1000) NOT NULL,
    item_code VARCHAR(64),
    ordered_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    previously_received_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    received_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    accepted_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    rejected_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    damaged_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    short_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    excess_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'PIECE',
    unit_rate NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    remarks TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_delivery_receipt_items PRIMARY KEY (project_id, receipt_item_id),
    CONSTRAINT fk_vendor_delivery_receipt_items_receipt FOREIGN KEY (project_id, delivery_receipt_id)
        REFERENCES vendor_delivery_receipts(project_id, delivery_receipt_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_dr_items_receipt ON vendor_delivery_receipt_items(project_id, delivery_receipt_id);
CREATE INDEX IF NOT EXISTS idx_vendor_dr_items_po_item ON vendor_delivery_receipt_items(project_id, purchase_order_id, purchase_order_item_id);

ALTER TABLE vendor_delivery_receipt_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_delivery_receipt_items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_delivery_receipt_items_tenant_isolation ON vendor_delivery_receipt_items;
CREATE POLICY vendor_delivery_receipt_items_tenant_isolation ON vendor_delivery_receipt_items
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 3. Vendor Delivery Receipt Audits Table
CREATE TABLE IF NOT EXISTS vendor_delivery_receipt_audits (
    project_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    delivery_receipt_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    CONSTRAINT pk_vendor_delivery_receipt_audits PRIMARY KEY (project_id, audit_id),
    CONSTRAINT fk_vendor_dr_audits_receipt FOREIGN KEY (project_id, delivery_receipt_id)
        REFERENCES vendor_delivery_receipts(project_id, delivery_receipt_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_dr_audits_receipt ON vendor_delivery_receipt_audits(project_id, delivery_receipt_id, occurred_at);

ALTER TABLE vendor_delivery_receipt_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_delivery_receipt_audits FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_delivery_receipt_audits_tenant_isolation ON vendor_delivery_receipt_audits;
CREATE POLICY vendor_delivery_receipt_audits_tenant_isolation ON vendor_delivery_receipt_audits
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));
