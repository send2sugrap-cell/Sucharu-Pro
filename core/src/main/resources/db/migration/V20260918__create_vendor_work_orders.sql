-- =========================================================================
-- SUCHARU PRO ERP DATABASE MIGRATION
-- V20260918__create_vendor_work_orders.sql
-- Module 12: Vendor Management — Step 04: Vendor Job Assignment & Work Order
-- =========================================================================

-- 1. Vendor Work Orders Table
CREATE TABLE IF NOT EXISTS vendor_work_orders (
    project_id VARCHAR(64) NOT NULL,
    work_order_id VARCHAR(64) NOT NULL,
    work_order_number VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    capability_type VARCHAR(64) NOT NULL,
    service_rate_id VARCHAR(64),
    source_reference_id VARCHAR(64),
    source_reference_type VARCHAR(64),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    quantity NUMERIC(14, 2) NOT NULL DEFAULT 1.00,
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'PIECE',
    pricing_method VARCHAR(32) NOT NULL DEFAULT 'PER_UNIT',
    rate_snapshot_base_rate NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    rate_snapshot_resolved_unit_rate NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    rate_snapshot_currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    rate_snapshot_source_rate_id VARCHAR(64),
    rate_snapshot_tier_metadata TEXT,
    rate_snapshot_quantity_basis NUMERIC(14, 2) NOT NULL DEFAULT 1.00,
    rate_snapshot_resolved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    estimated_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    scheduled_start_at TIMESTAMP WITH TIME ZONE,
    scheduled_due_at TIMESTAMP WITH TIME ZONE,
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_work_orders PRIMARY KEY (project_id, work_order_id),
    CONSTRAINT uq_vendor_work_order_number UNIQUE (project_id, work_order_number),
    CONSTRAINT fk_vendor_work_orders_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_work_orders_lookup ON vendor_work_orders(project_id, vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vendor_work_orders_capability ON vendor_work_orders(project_id, capability_type);
CREATE INDEX IF NOT EXISTS idx_vendor_work_orders_source_ref ON vendor_work_orders(project_id, source_reference_type, source_reference_id);
CREATE INDEX IF NOT EXISTS idx_vendor_work_orders_created ON vendor_work_orders(project_id, created_at);

ALTER TABLE vendor_work_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_work_orders FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_work_orders_tenant_isolation ON vendor_work_orders;
CREATE POLICY vendor_work_orders_tenant_isolation ON vendor_work_orders
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 2. Vendor Work Order Audits Table
CREATE TABLE IF NOT EXISTS vendor_work_order_audits (
    project_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    work_order_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    CONSTRAINT pk_vendor_work_order_audits PRIMARY KEY (project_id, audit_id),
    CONSTRAINT fk_vendor_work_order_audits_order FOREIGN KEY (project_id, work_order_id)
        REFERENCES vendor_work_orders(project_id, work_order_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_work_order_audits_order ON vendor_work_order_audits(project_id, work_order_id, occurred_at);

ALTER TABLE vendor_work_order_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_work_order_audits FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_work_order_audits_tenant_isolation ON vendor_work_order_audits;
CREATE POLICY vendor_work_order_audits_tenant_isolation ON vendor_work_order_audits
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));
