-- Module 12 Step 08: Vendor Quality, Rejection & Dispute Management
-- Canonical PostgreSQL Schema with FORCE ROW LEVEL SECURITY

-- 1. Vendor Quality Inspections
CREATE TABLE IF NOT EXISTS vendor_quality_inspections (
    project_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64),
    delivery_receipt_id VARCHAR(64),
    inspection_reference VARCHAR(64) NOT NULL,
    inspection_type VARCHAR(64) NOT NULL DEFAULT 'RECEIVING_INSPECTION',
    inspection_status VARCHAR(64) NOT NULL DEFAULT 'DRAFT',
    inspected_by VARCHAR(64),
    inspection_started_at TIMESTAMP WITH TIME ZONE,
    inspection_completed_at TIMESTAMP WITH TIME ZONE,
    received_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    accepted_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    rejected_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    conditional_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    overall_result VARCHAR(64),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_quality_inspections PRIMARY KEY (project_id, inspection_id),
    CONSTRAINT uq_vendor_quality_inspection_ref UNIQUE (project_id, inspection_reference)
);

-- 2. Vendor Quality Inspection Items
CREATE TABLE IF NOT EXISTS vendor_quality_inspection_items (
    project_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(64) NOT NULL,
    purchase_order_item_id VARCHAR(64),
    delivery_receipt_item_id VARCHAR(64),
    item_description TEXT NOT NULL,
    received_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    accepted_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    rejected_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    conditional_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    defect_count INT NOT NULL DEFAULT 0,
    defect_rate NUMERIC(9, 4) NOT NULL DEFAULT 0,
    inspection_result VARCHAR(64) NOT NULL DEFAULT 'ACCEPTED',
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_quality_inspection_items PRIMARY KEY (project_id, item_id),
    CONSTRAINT fk_vqii_inspection FOREIGN KEY (project_id, inspection_id)
        REFERENCES vendor_quality_inspections(project_id, inspection_id) ON DELETE CASCADE
);

-- 3. Vendor Quality Defects
CREATE TABLE IF NOT EXISTS vendor_defects (
    project_id VARCHAR(64) NOT NULL,
    defect_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(64) NOT NULL,
    inspection_item_id VARCHAR(64),
    vendor_id VARCHAR(64) NOT NULL,
    defect_type VARCHAR(64) NOT NULL DEFAULT 'QUALITY_DEFECT',
    severity VARCHAR(64) NOT NULL DEFAULT 'MEDIUM',
    description TEXT NOT NULL,
    quantity_affected NUMERIC(19, 4) NOT NULL DEFAULT 0,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detected_by VARCHAR(64) NOT NULL DEFAULT 'system',
    evidence_reference VARCHAR(256),
    status VARCHAR(64) NOT NULL DEFAULT 'OPEN',
    resolution_reference VARCHAR(256),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_defects PRIMARY KEY (project_id, defect_id),
    CONSTRAINT fk_vd_inspection FOREIGN KEY (project_id, inspection_id)
        REFERENCES vendor_quality_inspections(project_id, inspection_id) ON DELETE CASCADE
);

-- 4. Vendor Rejections
CREATE TABLE IF NOT EXISTS vendor_rejections (
    project_id VARCHAR(64) NOT NULL,
    rejection_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64),
    delivery_receipt_id VARCHAR(64),
    delivery_receipt_item_id VARCHAR(64),
    inspection_id VARCHAR(64),
    rejection_reference VARCHAR(64) NOT NULL,
    rejection_type VARCHAR(64) NOT NULL DEFAULT 'QUALITY_REJECTION',
    rejection_reason TEXT NOT NULL,
    rejected_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    rejected_value NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL DEFAULT 'DRAFT',
    disposition VARCHAR(64) NOT NULL DEFAULT 'RETURN_TO_VENDOR',
    replacement_required BOOLEAN NOT NULL DEFAULT FALSE,
    return_required BOOLEAN NOT NULL DEFAULT TRUE,
    credit_required BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    vendor_response TEXT,
    vendor_response_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_rejections PRIMARY KEY (project_id, rejection_id),
    CONSTRAINT uq_vendor_rejection_ref UNIQUE (project_id, rejection_reference)
);

-- 5. Vendor Disputes
CREATE TABLE IF NOT EXISTS vendor_disputes (
    project_id VARCHAR(64) NOT NULL,
    dispute_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    purchase_order_id VARCHAR(64),
    delivery_receipt_id VARCHAR(64),
    invoice_id VARCHAR(64),
    inspection_id VARCHAR(64),
    rejection_id VARCHAR(64),
    dispute_reference VARCHAR(64) NOT NULL,
    dispute_type VARCHAR(64) NOT NULL DEFAULT 'QUALITY',
    priority VARCHAR(64) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(64) NOT NULL DEFAULT 'OPEN',
    subject VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    disputed_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    disputed_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    raised_by VARCHAR(64) NOT NULL,
    assigned_to VARCHAR(64),
    vendor_response_due_at TIMESTAMP WITH TIME ZONE,
    vendor_response TEXT,
    vendor_response_at TIMESTAMP WITH TIME ZONE,
    resolution_proposal TEXT,
    resolution TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(64),
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_disputes PRIMARY KEY (project_id, dispute_id),
    CONSTRAINT uq_vendor_dispute_ref UNIQUE (project_id, dispute_reference)
);

-- 6. Vendor Dispute Events
CREATE TABLE IF NOT EXISTS vendor_dispute_events (
    project_id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    dispute_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    notes TEXT,
    payload_json TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_vendor_dispute_events PRIMARY KEY (project_id, event_id),
    CONSTRAINT fk_vde_dispute FOREIGN KEY (project_id, dispute_id)
        REFERENCES vendor_disputes(project_id, dispute_id) ON DELETE CASCADE
);

-- 7. Vendor Quality Evidence
CREATE TABLE IF NOT EXISTS vendor_quality_evidence (
    project_id VARCHAR(64) NOT NULL,
    evidence_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    file_reference VARCHAR(512) NOT NULL,
    file_name VARCHAR(256) NOT NULL,
    file_type VARCHAR(64) NOT NULL,
    description TEXT,
    uploaded_by VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checksum VARCHAR(128),
    CONSTRAINT pk_vendor_quality_evidence PRIMARY KEY (project_id, evidence_id)
);

-- 8. Vendor Quality Audits
CREATE TABLE IF NOT EXISTS vendor_quality_audits (
    project_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    CONSTRAINT pk_vendor_quality_audits PRIMARY KEY (project_id, audit_id)
);

-- Enable & Force Row Level Security on all 8 tables
ALTER TABLE vendor_quality_inspections ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_quality_inspections FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_quality_inspection_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_quality_inspection_items FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_defects ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_defects FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_rejections ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_rejections FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_disputes ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_disputes FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_dispute_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_dispute_events FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_quality_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_quality_evidence FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_quality_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_quality_audits FORCE ROW LEVEL SECURITY;

-- Create Tenant Policies
CREATE POLICY vqi_tenant_isolation ON vendor_quality_inspections
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vqii_tenant_isolation ON vendor_quality_inspection_items
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vd_tenant_isolation ON vendor_defects
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vr_tenant_isolation ON vendor_rejections
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vdisp_tenant_isolation ON vendor_disputes
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vde_tenant_isolation ON vendor_dispute_events
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vqe_tenant_isolation ON vendor_quality_evidence
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vqa_tenant_isolation ON vendor_quality_audits
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

-- Indexes for performance and lookups
CREATE INDEX idx_vqi_lookup ON vendor_quality_inspections(project_id, vendor_id, delivery_receipt_id, inspection_status);
CREATE INDEX idx_vqii_lookup ON vendor_quality_inspection_items(project_id, inspection_id);
CREATE INDEX idx_vd_lookup ON vendor_defects(project_id, inspection_id, vendor_id);
CREATE INDEX idx_vr_lookup ON vendor_rejections(project_id, vendor_id, delivery_receipt_id, status);
CREATE INDEX idx_vdisp_lookup ON vendor_disputes(project_id, vendor_id, status, dispute_type);
CREATE INDEX idx_vde_lookup ON vendor_dispute_events(project_id, dispute_id, occurred_at);
CREATE INDEX idx_vqe_lookup ON vendor_quality_evidence(project_id, source_type, source_id);
CREATE INDEX idx_vqa_lookup ON vendor_quality_audits(project_id, entity_id, occurred_at);
