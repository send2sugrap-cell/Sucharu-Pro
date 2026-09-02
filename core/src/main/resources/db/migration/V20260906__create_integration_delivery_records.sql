-- ============================================================
-- SUCHARU PRO — INFRA-04 STEP 03
-- Persistent Integration Delivery Records Table with RLS
-- Migration: V20260906__create_integration_delivery_records.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS integration_delivery_records (
    delivery_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    consumer_id VARCHAR(64) NOT NULL,
    integration_type VARCHAR(32) NOT NULL,
    destination VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failure_classification VARCHAR(32),
    sanitized_error TEXT,
    correlation_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_integration_delivery_records PRIMARY KEY (project_id, delivery_id),
    CONSTRAINT uq_integration_delivery_idempotency UNIQUE (project_id, consumer_id, event_id)
);

-- Index for polling pending or retryable integration deliveries
CREATE INDEX IF NOT EXISTS idx_integration_delivery_status
    ON integration_delivery_records (project_id, status, next_attempt_at);

-- Index for event correlation queries
CREATE INDEX IF NOT EXISTS idx_integration_delivery_event
    ON integration_delivery_records (project_id, event_id);

-- Enable Multi-Tenant Row-Level Security (RLS)
ALTER TABLE integration_delivery_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY integration_delivery_tenant_isolation ON integration_delivery_records
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));
