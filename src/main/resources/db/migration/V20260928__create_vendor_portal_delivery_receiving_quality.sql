-- ============================================================
-- SUCHARU PRO ERP
-- MODULE 13 STEP 05: VENDOR DELIVERY, RECEIVING & QUALITY COLLABORATION
-- ============================================================

-- 1. Vendor Portal Delivery Notices (Advance Shipping Information)
CREATE TABLE IF NOT EXISTS vendor_portal_delivery_notices (
    notice_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    notice_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    planned_delivery_date BIGINT NOT NULL,
    carrier_name VARCHAR(128),
    tracking_number VARCHAR(128),
    vehicle_number VARCHAR(64),
    driver_name VARCHAR(128),
    driver_phone VARCHAR(32),
    vendor_notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    submitted_at BIGINT,
    submitted_by VARCHAR(64),
    cancelled_at BIGINT,
    cancelled_by VARCHAR(64),
    cancellation_reason TEXT,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpdn_tenant_vendor ON vendor_portal_delivery_notices(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpdn_tenant_po ON vendor_portal_delivery_notices(tenant_id, purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_vpdn_status ON vendor_portal_delivery_notices(tenant_id, status);

ALTER TABLE vendor_portal_delivery_notices ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_delivery_notices FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpdn ON vendor_portal_delivery_notices;
CREATE POLICY tenant_isolation_vpdn ON vendor_portal_delivery_notices
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 2. Delivery Notice Items
CREATE TABLE IF NOT EXISTS vendor_portal_delivery_notice_items (
    item_id VARCHAR(64) PRIMARY KEY,
    notice_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    purchase_order_item_id VARCHAR(64) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    item_code VARCHAR(64),
    ordered_quantity NUMERIC(15, 4) NOT NULL,
    previously_delivered_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0,
    delivery_quantity NUMERIC(15, 4) NOT NULL,
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'PIECE',
    lot_number VARCHAR(64),
    package_count INT,
    remarks TEXT
);

CREATE INDEX IF NOT EXISTS idx_vpdni_notice ON vendor_portal_delivery_notice_items(notice_id);
CREATE INDEX IF NOT EXISTS idx_vpdni_tenant ON vendor_portal_delivery_notice_items(tenant_id);

ALTER TABLE vendor_portal_delivery_notice_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_delivery_notice_items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpdni ON vendor_portal_delivery_notice_items;
CREATE POLICY tenant_isolation_vpdni ON vendor_portal_delivery_notice_items
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 3. Delivery Acknowledgements (Internal Receiving Ops acknowledgment)
CREATE TABLE IF NOT EXISTS vendor_portal_delivery_acknowledgements (
    acknowledgement_id VARCHAR(64) PRIMARY KEY,
    notice_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    delivery_receipt_id VARCHAR(64),
    acknowledged_by VARCHAR(64) NOT NULL,
    acknowledged_at BIGINT NOT NULL,
    receiving_gate VARCHAR(64),
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_vpda_notice ON vendor_portal_delivery_acknowledgements(notice_id);
CREATE INDEX IF NOT EXISTS idx_vpda_tenant_vendor ON vendor_portal_delivery_acknowledgements(tenant_id, vendor_id);

ALTER TABLE vendor_portal_delivery_acknowledgements ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_delivery_acknowledgements FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpda ON vendor_portal_delivery_acknowledgements;
CREATE POLICY tenant_isolation_vpda ON vendor_portal_delivery_acknowledgements
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 4. Vendor Quality Responses
CREATE TABLE IF NOT EXISTS vendor_portal_quality_responses (
    response_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(64) NOT NULL,
    rejection_id VARCHAR(64),
    response_type VARCHAR(32) NOT NULL,
    comment TEXT NOT NULL,
    corrective_action_plan TEXT,
    promised_replacement_date BIGINT,
    evidence_references TEXT,
    responded_by VARCHAR(64) NOT NULL,
    responded_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpqr_tenant_vendor ON vendor_portal_quality_responses(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpqr_inspection ON vendor_portal_quality_responses(inspection_id);

ALTER TABLE vendor_portal_quality_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_quality_responses FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpqr ON vendor_portal_quality_responses;
CREATE POLICY tenant_isolation_vpqr ON vendor_portal_quality_responses
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 5. Delivery & Quality Exceptions
CREATE TABLE IF NOT EXISTS vendor_portal_delivery_exceptions (
    exception_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    exception_type VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    required_vendor_action TEXT,
    due_at BIGINT,
    resolved_at BIGINT,
    resolved_by VARCHAR(64),
    resolution_notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpde_tenant_vendor ON vendor_portal_delivery_exceptions(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpde_status ON vendor_portal_delivery_exceptions(tenant_id, status);

ALTER TABLE vendor_portal_delivery_exceptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_delivery_exceptions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpde ON vendor_portal_delivery_exceptions;
CREATE POLICY tenant_isolation_vpde ON vendor_portal_delivery_exceptions
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 6. Delivery Evidence Metadata
CREATE TABLE IF NOT EXISTS vendor_portal_delivery_evidence (
    evidence_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_reference VARCHAR(512) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    description TEXT,
    uploaded_by VARCHAR(64) NOT NULL,
    uploaded_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vpdev_tenant_vendor ON vendor_portal_delivery_evidence(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpdev_entity ON vendor_portal_delivery_evidence(entity_type, entity_id);

ALTER TABLE vendor_portal_delivery_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_delivery_evidence FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpdev ON vendor_portal_delivery_evidence;
CREATE POLICY tenant_isolation_vpdev ON vendor_portal_delivery_evidence
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 7. Delivery & Quality Collaboration Audit Events
CREATE TABLE IF NOT EXISTS vendor_portal_delivery_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    previous_state VARCHAR(64),
    new_state VARCHAR(64),
    correlation_id VARCHAR(64),
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vpdae_tenant_entity ON vendor_portal_delivery_audit_events(tenant_id, entity_type, entity_id);

ALTER TABLE vendor_portal_delivery_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_delivery_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpdae ON vendor_portal_delivery_audit_events;
CREATE POLICY tenant_isolation_vpdae ON vendor_portal_delivery_audit_events
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));
