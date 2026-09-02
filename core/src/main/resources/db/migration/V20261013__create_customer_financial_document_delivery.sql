-- =============================================================================
-- Migration: V20261013__create_customer_financial_document_delivery.sql
-- Module 14 Step 11: Customer Financial Document Delivery, Secure Access & Notification Foundation
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_financial_document_deliveries (
    delivery_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    document_type VARCHAR(64) NOT NULL,
    document_format VARCHAR(32) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    storage_reference VARCHAR(512) NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(128) NOT NULL DEFAULT 'application/octet-stream',
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    access_count INT NOT NULL DEFAULT 0,
    last_accessed_at BIGINT,
    last_accessed_by VARCHAR(64),
    expires_at BIGINT,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at BIGINT,
    revoked_by VARCHAR(64),
    revocation_reason TEXT,
    notification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    notified_at BIGINT,
    notification_id VARCHAR(64),
    failure_reason TEXT,
    idempotency_key VARCHAR(128),
    metadata_json TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_cfdd_tenant_proj_cust
    ON customer_financial_document_deliveries(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cfdd_document_id
    ON customer_financial_document_deliveries(tenant_id, project_id, document_id);

CREATE INDEX IF NOT EXISTS idx_cfdd_status_created
    ON customer_financial_document_deliveries(tenant_id, project_id, delivery_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cfdd_idempotency
    ON customer_financial_document_deliveries(tenant_id, project_id, idempotency_key);

CREATE TABLE IF NOT EXISTS customer_financial_document_delivery_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    delivery_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    correlation_id VARCHAR(64),
    details_json TEXT,
    checksum VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_cfdd_audit_delivery
    ON customer_financial_document_delivery_audit_events(tenant_id, project_id, delivery_id, timestamp ASC);

CREATE INDEX IF NOT EXISTS idx_cfdd_audit_cust
    ON customer_financial_document_delivery_audit_events(tenant_id, project_id, customer_id, timestamp ASC);

-- =============================================================================
-- Row Level Security (RLS) Enforcement
-- =============================================================================

ALTER TABLE customer_financial_document_deliveries ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_financial_document_deliveries FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS customer_financial_document_deliveries_tenant_isolation ON customer_financial_document_deliveries;
CREATE POLICY customer_financial_document_deliveries_tenant_isolation ON customer_financial_document_deliveries
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_financial_document_delivery_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_financial_document_delivery_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS customer_financial_document_delivery_audits_tenant_isolation ON customer_financial_document_delivery_audit_events;
CREATE POLICY customer_financial_document_delivery_audits_tenant_isolation ON customer_financial_document_delivery_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
