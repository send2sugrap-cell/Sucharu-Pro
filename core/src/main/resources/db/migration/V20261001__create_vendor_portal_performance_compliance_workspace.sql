-- ============================================================
-- SUCHARU PRO ERP
-- MODULE 13 STEP 08: VENDOR PERFORMANCE & COMPLIANCE WORKSPACE
-- PostgreSQL Migration with FORCE ROW LEVEL SECURITY
-- ============================================================

-- 1. Vendor Portal Evaluation Responses
CREATE TABLE IF NOT EXISTS vendor_portal_evaluation_responses (
    response_id VARCHAR(64) PRIMARY KEY,
    evaluation_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    response_type VARCHAR(32) NOT NULL DEFAULT 'FORMAL_RESPONSE',
    subject VARCHAR(255) NOT NULL,
    remarks TEXT NOT NULL,
    proposed_remediation TEXT,
    evidence_references TEXT[],
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    submitted_by VARCHAR(64) NOT NULL,
    submitted_at BIGINT NOT NULL,
    reviewer_feedback TEXT,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vper_tenant_vendor ON vendor_portal_evaluation_responses(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vper_evaluation ON vendor_portal_evaluation_responses(tenant_id, evaluation_id);
CREATE INDEX IF NOT EXISTS idx_vper_status ON vendor_portal_evaluation_responses(tenant_id, status);

ALTER TABLE vendor_portal_evaluation_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_evaluation_responses FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vper ON vendor_portal_evaluation_responses;
CREATE POLICY tenant_isolation_vper ON vendor_portal_evaluation_responses
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 2. Vendor Portal Compliance Evidence
CREATE TABLE IF NOT EXISTS vendor_portal_compliance_evidence (
    evidence_id VARCHAR(64) PRIMARY KEY,
    record_id VARCHAR(64),
    requirement_id VARCHAR(64),
    action_id VARCHAR(64),
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    evidence_type VARCHAR(32) NOT NULL DEFAULT 'DOCUMENT',
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    checksum VARCHAR(128),
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(128),
    description TEXT,
    uploaded_by VARCHAR(64) NOT NULL,
    uploaded_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpce_tenant_vendor ON vendor_portal_compliance_evidence(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpce_record ON vendor_portal_compliance_evidence(tenant_id, record_id);
CREATE INDEX IF NOT EXISTS idx_vpce_action ON vendor_portal_compliance_evidence(tenant_id, action_id);

ALTER TABLE vendor_portal_compliance_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_compliance_evidence FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpce ON vendor_portal_compliance_evidence;
CREATE POLICY tenant_isolation_vpce ON vendor_portal_compliance_evidence
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 3. Vendor Portal Corrective Action Responses
CREATE TABLE IF NOT EXISTS vendor_portal_corrective_action_responses (
    response_id VARCHAR(64) PRIMARY KEY,
    action_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    remediation_notes TEXT NOT NULL,
    root_cause_explanation TEXT,
    progress_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    is_completion_request BOOLEAN NOT NULL DEFAULT FALSE,
    evidence_references TEXT[],
    status VARCHAR(32) NOT NULL DEFAULT 'PLAN_SUBMITTED',
    submitted_by VARCHAR(64) NOT NULL,
    submitted_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vpcar_tenant_vendor ON vendor_portal_corrective_action_responses(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vpcar_action ON vendor_portal_corrective_action_responses(tenant_id, action_id);

ALTER TABLE vendor_portal_corrective_action_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_corrective_action_responses FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vpcar ON vendor_portal_corrective_action_responses;
CREATE POLICY tenant_isolation_vpcar ON vendor_portal_corrective_action_responses
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 4. Vendor Portal Performance & Compliance Audit Events
CREATE TABLE IF NOT EXISTS vendor_portal_performance_compliance_audit_events (
    activity_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64),
    description TEXT NOT NULL,
    occurred_at BIGINT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_vppcae_tenant_vendor ON vendor_portal_performance_compliance_audit_events(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_vppcae_entity ON vendor_portal_performance_compliance_audit_events(tenant_id, entity_type, entity_id);

ALTER TABLE vendor_portal_performance_compliance_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_performance_compliance_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_vppcae ON vendor_portal_performance_compliance_audit_events;
CREATE POLICY tenant_isolation_vppcae ON vendor_portal_performance_compliance_audit_events
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));
