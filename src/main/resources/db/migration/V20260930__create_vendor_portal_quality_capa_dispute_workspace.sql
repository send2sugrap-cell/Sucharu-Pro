-- ============================================================
-- SUCHARU PRO ERP
-- MODULE 13 STEP 07: VENDOR QUALITY, CAPA, REJECTION & DISPUTE WORKSPACE
-- PostgreSQL Migration with FORCE ROW LEVEL SECURITY
-- ============================================================

-- 1. Vendor Portal Quality Cases (Projection & Portal Lifecycle)
CREATE TABLE IF NOT EXISTS vendor_portal_quality_cases (
    case_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(64),
    delivery_receipt_id VARCHAR(64),
    purchase_order_id VARCHAR(64),
    rejection_id VARCHAR(64),
    case_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    acknowledged_at BIGINT,
    acknowledged_by VARCHAR(64),
    closed_at BIGINT,
    closed_by VARCHAR(64),
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpqc_tenant_vendor ON vendor_portal_quality_cases(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpqc_inspection ON vendor_portal_quality_cases(tenant_id, inspection_id);
CREATE INDEX IF NOT EXISTS idx_vpqc_status ON vendor_portal_quality_cases(tenant_id, status);

ALTER TABLE vendor_portal_quality_cases ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_quality_cases FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpqc ON vendor_portal_quality_cases;
CREATE POLICY tenant_isolation_vpqc ON vendor_portal_quality_cases
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 2. Vendor Portal CAPA Plans
CREATE TABLE IF NOT EXISTS vendor_portal_capa_plans (
    capa_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64),
    inspection_id VARCHAR(64),
    rejection_id VARCHAR(64),
    capa_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    title VARCHAR(255) NOT NULL,
    root_cause TEXT NOT NULL,
    corrective_action TEXT NOT NULL,
    preventive_action TEXT NOT NULL,
    responsible_person VARCHAR(128) NOT NULL,
    target_completion_date BIGINT NOT NULL,
    actual_completion_date BIGINT,
    affected_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0,
    affected_unit VARCHAR(32) NOT NULL DEFAULT 'PIECE',
    verification_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED',
    verified_by VARCHAR(64),
    verified_at BIGINT,
    reviewer_comments TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpcp_tenant_vendor ON vendor_portal_capa_plans(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpcp_case ON vendor_portal_capa_plans(tenant_id, case_id);
CREATE INDEX IF NOT EXISTS idx_vpcp_status ON vendor_portal_capa_plans(tenant_id, status);

ALTER TABLE vendor_portal_capa_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_capa_plans FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpcp ON vendor_portal_capa_plans;
CREATE POLICY tenant_isolation_vpcp ON vendor_portal_capa_plans
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 3. Vendor Portal CAPA Actions
CREATE TABLE IF NOT EXISTS vendor_portal_capa_actions (
    action_id VARCHAR(64) PRIMARY KEY,
    capa_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    action_number INT NOT NULL DEFAULT 1,
    action_type VARCHAR(32) NOT NULL DEFAULT 'CORRECTIVE',
    description TEXT NOT NULL,
    owner VARCHAR(128) NOT NULL,
    target_date BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    completed_at BIGINT,
    evidence_references TEXT,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_vpca_capa ON vendor_portal_capa_actions(capa_id);
CREATE INDEX IF NOT EXISTS idx_vpca_tenant ON vendor_portal_capa_actions(tenant_id);

ALTER TABLE vendor_portal_capa_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_capa_actions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpca ON vendor_portal_capa_actions;
CREATE POLICY tenant_isolation_vpca ON vendor_portal_capa_actions
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 4. Vendor Portal Dispute Submissions
CREATE TABLE IF NOT EXISTS vendor_portal_dispute_submissions (
    dispute_submission_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    dispute_id VARCHAR(64),
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    dispute_type VARCHAR(32) NOT NULL DEFAULT 'QUALITY',
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requested_resolution VARCHAR(32) NOT NULL DEFAULT 'REPLACEMENT',
    disputed_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0,
    disputed_amount NUMERIC(15, 4) NOT NULL DEFAULT 0,
    vendor_response TEXT,
    vendor_response_at BIGINT,
    resolution_proposal TEXT,
    resolution_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    resolution_notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpds_tenant_vendor ON vendor_portal_dispute_submissions(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpds_source ON vendor_portal_dispute_submissions(tenant_id, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_vpds_status ON vendor_portal_dispute_submissions(tenant_id, status);

ALTER TABLE vendor_portal_dispute_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_dispute_submissions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpds ON vendor_portal_dispute_submissions;
CREATE POLICY tenant_isolation_vpds ON vendor_portal_dispute_submissions
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 5. Vendor Portal Resolution Responses
CREATE TABLE IF NOT EXISTS vendor_portal_resolution_responses (
    response_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    dispute_id VARCHAR(64) NOT NULL,
    proposal_action VARCHAR(32) NOT NULL,
    rationale TEXT NOT NULL,
    responded_by VARCHAR(64) NOT NULL,
    responded_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vprr_dispute ON vendor_portal_resolution_responses(dispute_id);
CREATE INDEX IF NOT EXISTS idx_vprr_tenant_vendor ON vendor_portal_resolution_responses(tenant_id, vendor_id);

ALTER TABLE vendor_portal_resolution_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_resolution_responses FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vprr ON vendor_portal_resolution_responses;
CREATE POLICY tenant_isolation_vprr ON vendor_portal_resolution_responses
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 6. Vendor Portal Quality Evidence
CREATE TABLE IF NOT EXISTS vendor_portal_quality_evidence (
    evidence_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    evidence_type VARCHAR(32) NOT NULL DEFAULT 'DOCUMENT',
    filename VARCHAR(255) NOT NULL,
    file_reference VARCHAR(512) NOT NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    checksum VARCHAR(128),
    description TEXT,
    uploaded_by VARCHAR(64) NOT NULL,
    uploaded_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vpqe_tenant_vendor ON vendor_portal_quality_evidence(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpqe_entity ON vendor_portal_quality_evidence(tenant_id, entity_type, entity_id);

ALTER TABLE vendor_portal_quality_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_quality_evidence FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpqe ON vendor_portal_quality_evidence;
CREATE POLICY tenant_isolation_vpqe ON vendor_portal_quality_evidence
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 7. Vendor Portal Quality Audit Events
CREATE TABLE IF NOT EXISTS vendor_portal_quality_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    details TEXT,
    occurred_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vpqa_tenant_vendor ON vendor_portal_quality_audit_events(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpqa_entity ON vendor_portal_quality_audit_events(tenant_id, entity_type, entity_id);

ALTER TABLE vendor_portal_quality_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_quality_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpqa ON vendor_portal_quality_audit_events;
CREATE POLICY tenant_isolation_vpqa ON vendor_portal_quality_audit_events
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));
