-- ============================================================
-- SUCHARU PRO — INFRA-04 STEP 08
-- AI Agent Notification Boundary, Confirmation & Action State Tables
-- Migration: V20260911__ai_agent_notification_boundary.sql
-- ============================================================

-- ------------------------------------------------------------
-- ai_notification_action_records
-- Idempotency tracking table for AI Agent notification actions
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_notification_action_records (
    action_id         VARCHAR(64)  NOT NULL,
    project_id        VARCHAR(64)  NOT NULL,
    agent_id          VARCHAR(64)  NOT NULL,
    action_type       VARCHAR(64)  NOT NULL,
    idempotency_key   VARCHAR(128) NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    response_summary  TEXT         NOT NULL,
    correlation_id    VARCHAR(64)  NOT NULL,
    executed_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ai_notification_action_records PRIMARY KEY (project_id, action_id),
    CONSTRAINT uq_ai_notification_action_idempotency
        UNIQUE (project_id, agent_id, action_type, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_ai_action_lookup
    ON ai_notification_action_records (project_id, agent_id, action_type);

ALTER TABLE ai_notification_action_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY ai_action_tenant_isolation ON ai_notification_action_records
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

-- ------------------------------------------------------------
-- ai_notification_confirmations
-- Persistent human confirmation requests for AI actions
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_notification_confirmations (
    confirmation_id      VARCHAR(64)  NOT NULL,
    project_id           VARCHAR(64)  NOT NULL,
    action_type          VARCHAR(64)  NOT NULL,
    requested_by_agent_id VARCHAR(64)  NOT NULL,
    payload_summary      TEXT         NOT NULL,
    target_recipient_id  VARCHAR(64)  NOT NULL,
    status               VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    approved_by_human_id VARCHAR(64),
    approver_role        VARCHAR(32),
    approved_at          TIMESTAMPTZ,
    rejection_reason     TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_notification_confirmations PRIMARY KEY (project_id, confirmation_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_conf_status
    ON ai_notification_confirmations (project_id, status, expires_at);

ALTER TABLE ai_notification_confirmations ENABLE ROW LEVEL SECURITY;

CREATE POLICY ai_conf_tenant_isolation ON ai_notification_confirmations
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

-- ------------------------------------------------------------
-- ai_notification_audit
-- Append-only audit table for all AI Agent notification actions
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_notification_audit (
    audit_id         VARCHAR(64)  NOT NULL,
    project_id       VARCHAR(64)  NOT NULL,
    operation        VARCHAR(64)  NOT NULL,
    decision         VARCHAR(32)  NOT NULL,
    agent_id         VARCHAR(64)  NOT NULL,
    action_type      VARCHAR(64),
    recipient_id     VARCHAR(64),
    correlation_id   VARCHAR(64)  NOT NULL,
    request_id       VARCHAR(64)  NOT NULL,
    confirmation_id  VARCHAR(64),
    reason_code      VARCHAR(64),
    safe_summary     TEXT,
    occurred_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ai_notification_audit PRIMARY KEY (project_id, audit_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_audit_time
    ON ai_notification_audit (project_id, occurred_at DESC);

ALTER TABLE ai_notification_audit ENABLE ROW LEVEL SECURITY;

CREATE POLICY ai_audit_tenant_isolation ON ai_notification_audit
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

CREATE POLICY ai_audit_deny_update ON ai_notification_audit
    FOR UPDATE
    USING (false);

CREATE POLICY ai_audit_deny_delete ON ai_notification_audit
    FOR DELETE
    USING (false);
