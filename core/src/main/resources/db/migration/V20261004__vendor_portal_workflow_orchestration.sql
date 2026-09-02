-- =============================================================================
-- Migration: V20261004__vendor_portal_workflow_orchestration.sql
-- Module 13 Step 11: Vendor Portal End-to-End Workflow Orchestration, Cross-Module Integration & Operational Consistency
-- =============================================================================

CREATE TABLE IF NOT EXISTS vendor_portal_workflows (
    workflow_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    workflow_title VARCHAR(255) NOT NULL,
    current_stage VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    sla_status VARCHAR(32) NOT NULL DEFAULT 'ON_TRACK',
    rfq_id VARCHAR(64),
    quotation_id VARCHAR(64),
    purchase_order_id VARCHAR(64),
    work_order_id VARCHAR(64),
    delivery_notice_id VARCHAR(64),
    invoice_id VARCHAR(64),
    quality_case_id VARCHAR(64),
    settlement_id VARCHAR(64),
    started_at BIGINT NOT NULL,
    completed_at BIGINT,
    target_delivery_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_vp_workflows_vendor_tenant
    ON vendor_portal_workflows(tenant_id, vendor_id, status, current_stage);
CREATE INDEX IF NOT EXISTS idx_vp_workflows_correlation
    ON vendor_portal_workflows(tenant_id, correlation_id);

CREATE TABLE IF NOT EXISTS vendor_portal_workflow_events (
    event_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    causation_id VARCHAR(128),
    stage VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    source_module VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL DEFAULT 'VENDOR',
    occurred_at BIGINT NOT NULL,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_vp_workflow_events_timeline
    ON vendor_portal_workflow_events(tenant_id, vendor_id, workflow_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS vendor_portal_workflow_exceptions (
    exception_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    detected_at BIGINT NOT NULL,
    resolved_at BIGINT,
    resolved_by VARCHAR(64),
    resolution_notes TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_vp_workflow_exceptions_lookup
    ON vendor_portal_workflow_exceptions(tenant_id, vendor_id, status, severity);

CREATE TABLE IF NOT EXISTS vendor_portal_workflow_actions (
    action_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    required_role VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    due_at BIGINT,
    deep_link_target VARCHAR(255),
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at BIGINT,
    completed_by VARCHAR(64),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vp_workflow_actions_pending
    ON vendor_portal_workflow_actions(tenant_id, vendor_id, is_completed, due_at);

CREATE TABLE IF NOT EXISTS vendor_portal_workflow_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    reason TEXT,
    occurred_at BIGINT NOT NULL,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_vp_workflow_audit_events_lookup
    ON vendor_portal_workflow_audit_events(tenant_id, vendor_id, occurred_at DESC);

-- Enable & Force Row-Level Security (RLS) on all Step 11 tables
ALTER TABLE vendor_portal_workflows ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_workflows FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_vendor_portal_workflows ON vendor_portal_workflows
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE vendor_portal_workflow_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_workflow_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_vendor_portal_workflow_events ON vendor_portal_workflow_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE vendor_portal_workflow_exceptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_workflow_exceptions FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_vendor_portal_workflow_exceptions ON vendor_portal_workflow_exceptions
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE vendor_portal_workflow_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_workflow_actions FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_vendor_portal_workflow_actions ON vendor_portal_workflow_actions
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE vendor_portal_workflow_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_workflow_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_vendor_portal_workflow_audit_events ON vendor_portal_workflow_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
