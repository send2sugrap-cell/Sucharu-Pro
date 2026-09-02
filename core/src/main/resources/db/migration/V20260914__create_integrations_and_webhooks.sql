-- ============================================================
-- SUCHARU PRO — INFRA-05 STEP 05
-- External Integration Runtime & Webhook Dispatch Platform Tables
-- Migration: V20260914__create_integrations_and_webhooks.sql
-- ============================================================

-- 1. External Integrations Catalog Table
CREATE TABLE IF NOT EXISTS external_integrations (
    integration_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    integration_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    base_url VARCHAR(512) NOT NULL,
    configuration_reference VARCHAR(128),
    allowed_event_types TEXT NOT NULL DEFAULT '',
    version VARCHAR(32) NOT NULL DEFAULT 'v1',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_successful_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    CONSTRAINT pk_external_integrations PRIMARY KEY (project_id, integration_id)
);

CREATE INDEX IF NOT EXISTS idx_external_integrations_provider
    ON external_integrations (project_id, provider, status);

-- 2. Durable Inbound Webhook Events Table
CREATE TABLE IF NOT EXISTS webhook_events (
    event_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    integration_id VARCHAR(64) NOT NULL,
    external_event_id VARCHAR(128),
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    verified_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    attempt_count INT NOT NULL DEFAULT 0,
    processed_at TIMESTAMPTZ,
    correlation_id VARCHAR(64) NOT NULL,
    causation_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_webhook_events PRIMARY KEY (project_id, event_id),
    CONSTRAINT uq_webhook_provider_event UNIQUE (project_id, provider, external_event_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_events_status
    ON webhook_events (project_id, status, received_at);

CREATE INDEX IF NOT EXISTS idx_webhook_events_integration
    ON webhook_events (project_id, integration_id, event_type);

-- 3. External Integration Audit Log Table
CREATE TABLE IF NOT EXISTS integration_audit_log (
    audit_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    integration_id VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL, -- INBOUND or OUTBOUND
    status VARCHAR(32) NOT NULL,
    sanitized_error TEXT,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    correlation_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_integration_audit_log PRIMARY KEY (project_id, audit_id)
);

CREATE INDEX IF NOT EXISTS idx_integration_audit_lookup
    ON integration_audit_log (project_id, integration_id, created_at DESC);

-- Enable Multi-Tenant Row-Level Security (RLS)
ALTER TABLE external_integrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE external_integrations FORCE ROW LEVEL SECURITY;

ALTER TABLE webhook_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhook_events FORCE ROW LEVEL SECURITY;

ALTER TABLE integration_audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE integration_audit_log FORCE ROW LEVEL SECURITY;

-- Tenant Isolation Policies
CREATE POLICY external_integrations_tenant_isolation ON external_integrations
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

CREATE POLICY webhook_events_tenant_isolation ON webhook_events
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

CREATE POLICY integration_audit_log_tenant_isolation ON integration_audit_log
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));
