-- =============================================================================
-- Migration: V20261014__create_customer_financial_alerts_and_schedules.sql
-- Module 14 Step 12: Customer Financial Alerts, Scheduled Reports & Automated Follow-Up Foundation
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_financial_alerts (
    alert_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(255) NOT NULL,
    safe_message TEXT NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    detected_at BIGINT NOT NULL,
    due_at BIGINT,
    resolved_at BIGINT,
    acknowledged_at BIGINT,
    acknowledged_by VARCHAR(64),
    dismissed_at BIGINT,
    dismissed_by VARCHAR(64),
    dismissal_reason TEXT,
    expires_at BIGINT,
    correlation_id VARCHAR(64),
    deduplication_key VARCHAR(255) NOT NULL,
    metadata_json TEXT,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_cfa_dedup_active
    ON customer_financial_alerts(tenant_id, project_id, deduplication_key)
    WHERE status IN ('OPEN', 'ACKNOWLEDGED');

CREATE INDEX IF NOT EXISTS idx_cfa_tenant_proj_cust
    ON customer_financial_alerts(tenant_id, project_id, customer_id, detected_at DESC);

CREATE INDEX IF NOT EXISTS idx_cfa_status_severity
    ON customer_financial_alerts(tenant_id, project_id, status, severity);

CREATE TABLE IF NOT EXISTS customer_financial_alert_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    alert_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    details_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_cfa_audit_alert
    ON customer_financial_alert_audit_events(tenant_id, project_id, alert_id, timestamp ASC);

CREATE TABLE IF NOT EXISTS customer_financial_report_schedules (
    schedule_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    report_type VARCHAR(64) NOT NULL,
    format VARCHAR(32) NOT NULL,
    frequency VARCHAR(32) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Dhaka',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    next_run_at BIGINT NOT NULL,
    last_run_at BIGINT,
    last_run_status VARCHAR(32),
    consecutive_failures INT NOT NULL DEFAULT 0,
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_cfrs_tenant_proj_cust
    ON customer_financial_report_schedules(tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_cfrs_due_active
    ON customer_financial_report_schedules(tenant_id, project_id, status, next_run_at ASC)
    WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS customer_financial_report_schedule_executions (
    execution_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    schedule_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    report_type VARCHAR(64) NOT NULL,
    format VARCHAR(32) NOT NULL,
    executed_at BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    document_delivery_id VARCHAR(64),
    error_message TEXT,
    correlation_id VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_cfrse_schedule_time
    ON customer_financial_report_schedule_executions(tenant_id, project_id, schedule_id, executed_at DESC);

-- =============================================================================
-- Row Level Security (RLS) Enforcement
-- =============================================================================

ALTER TABLE customer_financial_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_financial_alerts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS customer_financial_alerts_tenant_isolation ON customer_financial_alerts;
CREATE POLICY customer_financial_alerts_tenant_isolation ON customer_financial_alerts
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_financial_alert_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_financial_alert_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS customer_financial_alert_audits_tenant_isolation ON customer_financial_alert_audit_events;
CREATE POLICY customer_financial_alert_audits_tenant_isolation ON customer_financial_alert_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_financial_report_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_financial_report_schedules FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS customer_financial_report_schedules_tenant_isolation ON customer_financial_report_schedules;
CREATE POLICY customer_financial_report_schedules_tenant_isolation ON customer_financial_report_schedules
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));

ALTER TABLE customer_financial_report_schedule_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_financial_report_schedule_executions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS customer_financial_report_schedule_executions_tenant_isolation ON customer_financial_report_schedule_executions;
CREATE POLICY customer_financial_report_schedule_executions_tenant_isolation ON customer_financial_report_schedule_executions
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
