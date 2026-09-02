-- =============================================================================
-- Migration: V20261012__create_customer_collection_management.sql
-- Module 14 Step 08: Customer Payment Due Scheduling, Receivable Aging Actions & Collection Management Foundation
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_collection_actions (
    action_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64),
    action_type VARCHAR(32) NOT NULL DEFAULT 'REMINDER',
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at BIGINT NOT NULL,
    performed_at BIGINT,
    next_follow_up_at BIGINT,
    assigned_user_id VARCHAR(64),
    outcome VARCHAR(32),
    outcome_notes TEXT,
    cancellation_reason TEXT,
    notes TEXT,
    idempotency_key VARCHAR(64),
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_cca_tenant_proj_cust
    ON customer_collection_actions(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cca_status_scheduled
    ON customer_collection_actions(tenant_id, project_id, status, scheduled_at);

CREATE INDEX IF NOT EXISTS idx_cca_assigned_user
    ON customer_collection_actions(tenant_id, project_id, assigned_user_id, status);

CREATE INDEX IF NOT EXISTS idx_cca_invoice
    ON customer_collection_actions(tenant_id, project_id, invoice_id);

CREATE TABLE IF NOT EXISTS customer_payment_promises (
    promise_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64),
    action_id VARCHAR(64),
    promised_amount NUMERIC(18, 4) NOT NULL,
    promised_date BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    fulfilled_at BIGINT,
    fulfilled_payment_id VARCHAR(64),
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_cpp_tenant_proj_cust
    ON customer_payment_promises(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cpp_status_date
    ON customer_payment_promises(tenant_id, project_id, status, promised_date);

CREATE TABLE IF NOT EXISTS customer_collection_action_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    action_id VARCHAR(64),
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_value_json TEXT,
    new_value_json TEXT,
    reason TEXT,
    occurred_at BIGINT NOT NULL,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_ccaa_lookup
    ON customer_collection_action_audit_events(tenant_id, project_id, customer_id, occurred_at DESC);

-- Enable and Force Row Level Security (RLS)
ALTER TABLE customer_collection_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_collection_actions FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_collection_actions ON customer_collection_actions
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_payment_promises ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_payment_promises FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_payment_promises ON customer_payment_promises
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE customer_collection_action_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_collection_action_audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_customer_collection_action_audit_events ON customer_collection_action_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
