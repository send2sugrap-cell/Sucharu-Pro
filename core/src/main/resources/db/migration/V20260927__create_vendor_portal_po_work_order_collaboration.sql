-- ============================================================================
-- SUCHARU PRO ERP — V20260927: Vendor Portal PO & Work Order Collaboration
-- MODULE 13 STEP 04: Production-Grade Schema & Forced Row Level Security (RLS)
-- ============================================================================

-- 1. PO Acknowledgements Table
CREATE TABLE IF NOT EXISTS vendor_portal_po_acknowledgements (
    acknowledgement_id VARCHAR(64) PRIMARY KEY,
    purchase_order_id VARCHAR(64) NOT NULL REFERENCES vendor_purchase_orders(purchase_order_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    actor_id VARCHAR(64) NOT NULL,
    acknowledgement_type VARCHAR(32) NOT NULL,
    exception_details TEXT,
    decline_reason TEXT,
    promised_delivery_date BIGINT,
    comment TEXT,
    acknowledged_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpo_ack_tenant_proj_vnd ON vendor_portal_po_acknowledgements(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpo_ack_po_id ON vendor_portal_po_acknowledgements(purchase_order_id);

ALTER TABLE vendor_portal_po_acknowledgements ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_po_acknowledgements FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_po_ack_isolation_policy ON vendor_portal_po_acknowledgements
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));


-- 2. Work Order Acknowledgements Table
CREATE TABLE IF NOT EXISTS vendor_portal_wo_acknowledgements (
    acknowledgement_id VARCHAR(64) PRIMARY KEY,
    work_order_id VARCHAR(64) NOT NULL REFERENCES vendor_work_orders(work_order_id) ON DELETE CASCADE,
    purchase_order_id VARCHAR(64),
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    actor_id VARCHAR(64) NOT NULL,
    acknowledgement_type VARCHAR(32) NOT NULL,
    exception_details TEXT,
    decline_reason TEXT,
    promised_start_date BIGINT,
    promised_completion_date BIGINT,
    comment TEXT,
    acknowledged_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vwo_ack_tenant_proj_vnd ON vendor_portal_wo_acknowledgements(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vwo_ack_wo_id ON vendor_portal_wo_acknowledgements(work_order_id);

ALTER TABLE vendor_portal_wo_acknowledgements ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_wo_acknowledgements FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_wo_ack_isolation_policy ON vendor_portal_wo_acknowledgements
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));


-- 3. Progress Updates Table (Append-only)
CREATE TABLE IF NOT EXISTS vendor_portal_progress_updates (
    progress_update_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    work_order_id VARCHAR(64) NOT NULL REFERENCES vendor_work_orders(work_order_id) ON DELETE CASCADE,
    progress_percentage NUMERIC(5, 2),
    completed_quantity NUMERIC(16, 4) NOT NULL,
    remaining_quantity NUMERIC(16, 4) NOT NULL,
    authorized_quantity NUMERIC(16, 4) NOT NULL,
    status_summary VARCHAR(255) NOT NULL,
    notes TEXT,
    expected_completion_date BIGINT,
    blocker_reference_id VARCHAR(64),
    submitted_by VARCHAR(64) NOT NULL,
    submitted_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vprog_tenant_proj_vnd ON vendor_portal_progress_updates(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vprog_wo_id ON vendor_portal_progress_updates(work_order_id);
CREATE INDEX IF NOT EXISTS idx_vprog_submitted_at ON vendor_portal_progress_updates(submitted_at);

ALTER TABLE vendor_portal_progress_updates ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_progress_updates FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_progress_isolation_policy ON vendor_portal_progress_updates
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));


-- 4. Blockers Table
CREATE TABLE IF NOT EXISTS vendor_portal_blockers (
    blocker_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    work_order_id VARCHAR(64) NOT NULL REFERENCES vendor_work_orders(work_order_id) ON DELETE CASCADE,
    purchase_order_id VARCHAR(64),
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    resolution_notes TEXT,
    reported_by VARCHAR(64) NOT NULL,
    reported_at BIGINT NOT NULL,
    acknowledged_by VARCHAR(64),
    acknowledged_at BIGINT,
    resolved_by VARCHAR(64),
    resolved_at BIGINT,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vblocker_tenant_proj_vnd ON vendor_portal_blockers(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vblocker_wo_id ON vendor_portal_blockers(work_order_id);
CREATE INDEX IF NOT EXISTS idx_vblocker_status ON vendor_portal_blockers(status);

ALTER TABLE vendor_portal_blockers ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_blockers FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_blocker_isolation_policy ON vendor_portal_blockers
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));


-- 5. Collaboration Threads Table
CREATE TABLE IF NOT EXISTS vendor_portal_collaboration_threads (
    thread_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vthread_tenant_proj_vnd ON vendor_portal_collaboration_threads(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vthread_resource ON vendor_portal_collaboration_threads(resource_type, resource_id);

ALTER TABLE vendor_portal_collaboration_threads ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_collaboration_threads FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_thread_isolation_policy ON vendor_portal_collaboration_threads
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));


-- 6. Collaboration Messages Table (Append-only)
CREATE TABLE IF NOT EXISTS vendor_portal_collaboration_messages (
    message_id VARCHAR(64) PRIMARY KEY,
    thread_id VARCHAR(64) NOT NULL REFERENCES vendor_portal_collaboration_threads(thread_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    author_id VARCHAR(64) NOT NULL,
    author_name VARCHAR(128),
    is_internal_author BOOLEAN NOT NULL DEFAULT FALSE,
    message TEXT NOT NULL,
    visibility VARCHAR(32) NOT NULL DEFAULT 'VENDOR_VISIBLE',
    attachment_metadata_json TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vmsg_tenant_proj_vnd ON vendor_portal_collaboration_messages(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vmsg_thread_id ON vendor_portal_collaboration_messages(thread_id);
CREATE INDEX IF NOT EXISTS idx_vmsg_created_at ON vendor_portal_collaboration_messages(created_at);

ALTER TABLE vendor_portal_collaboration_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_collaboration_messages FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_message_isolation_policy ON vendor_portal_collaboration_messages
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));


-- 7. Collaboration Evidence / Document Metadata Table
CREATE TABLE IF NOT EXISTS vendor_portal_evidence (
    evidence_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    file_reference VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(128),
    description TEXT,
    visibility VARCHAR(32) NOT NULL DEFAULT 'VENDOR_VISIBLE',
    uploaded_by VARCHAR(64) NOT NULL,
    uploaded_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vevidence_tenant_proj_vnd ON vendor_portal_evidence(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vevidence_resource ON vendor_portal_evidence(resource_type, resource_id);

ALTER TABLE vendor_portal_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_evidence FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_evidence_isolation_policy ON vendor_portal_evidence
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));


-- 8. Completion Requests Table
CREATE TABLE IF NOT EXISTS vendor_portal_completion_requests (
    completion_request_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    work_order_id VARCHAR(64) NOT NULL REFERENCES vendor_work_orders(work_order_id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    completion_notes TEXT NOT NULL,
    final_completed_quantity NUMERIC(16, 4) NOT NULL,
    evidence_references TEXT,
    submitted_by VARCHAR(64) NOT NULL,
    submitted_at BIGINT NOT NULL,
    reviewed_by VARCHAR(64),
    reviewed_at BIGINT,
    review_notes TEXT,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vcompl_tenant_proj_vnd ON vendor_portal_completion_requests(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vcompl_wo_id ON vendor_portal_completion_requests(work_order_id);
CREATE INDEX IF NOT EXISTS idx_vcompl_status ON vendor_portal_completion_requests(status);

ALTER TABLE vendor_portal_completion_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_completion_requests FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_completion_isolation_policy ON vendor_portal_completion_requests
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));


-- 9. Collaboration Audit Events Table (Append-only)
CREATE TABLE IF NOT EXISTS vendor_portal_collaboration_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL REFERENCES vendors(vendor_id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    previous_state VARCHAR(64),
    new_state VARCHAR(64),
    correlation_id VARCHAR(64),
    metadata_json TEXT,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vcollab_audit_tenant_proj_vnd ON vendor_portal_collaboration_audit_events(tenant_id, project_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vcollab_audit_resource ON vendor_portal_collaboration_audit_events(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_vcollab_audit_timestamp ON vendor_portal_collaboration_audit_events(timestamp);

ALTER TABLE vendor_portal_collaboration_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_collaboration_audit_events FORCE ROW LEVEL SECURITY;

CREATE POLICY vendor_portal_collab_audit_isolation_policy ON vendor_portal_collaboration_audit_events
    AS RESTRICTIVE
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', TRUE));
