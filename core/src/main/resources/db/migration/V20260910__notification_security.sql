-- ============================================================
-- SUCHARU PRO — INFRA-04 STEP 07
-- Notification Security: Audit, Suppression, Rate Limit Tables
-- Migration: V20260910__notification_security.sql
-- ============================================================

-- ------------------------------------------------------------
-- notification_security_audit
-- Append-only immutable audit trail for notification security decisions.
-- NO UPDATE or DELETE policies are applied.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_security_audit (
    audit_id         VARCHAR(64)  NOT NULL,
    project_id       VARCHAR(64)  NOT NULL,
    operation        VARCHAR(64)  NOT NULL,
    decision         VARCHAR(32)  NOT NULL,
    reason           TEXT,
    event_id         VARCHAR(64),
    notification_id  VARCHAR(64),
    actor_id         VARCHAR(64),
    actor_role       VARCHAR(32),
    channel          VARCHAR(32),
    recipient_id     VARCHAR(64),
    correlation_id   VARCHAR(64),
    request_id       VARCHAR(64),
    safe_details     JSONB,
    occurred_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_notification_security_audit PRIMARY KEY (project_id, audit_id)
);

-- Index for tenant-scoped audit lookups (most recent first)
CREATE INDEX IF NOT EXISTS idx_nsa_project_time
    ON notification_security_audit (project_id, occurred_at DESC);

-- Index for event-level correlation
CREATE INDEX IF NOT EXISTS idx_nsa_event
    ON notification_security_audit (project_id, event_id)
    WHERE event_id IS NOT NULL;

-- Index for recipient-level forensic investigation
CREATE INDEX IF NOT EXISTS idx_nsa_recipient
    ON notification_security_audit (project_id, recipient_id)
    WHERE recipient_id IS NOT NULL;

-- Enable Row-Level Security (tenant isolation)
ALTER TABLE notification_security_audit ENABLE ROW LEVEL SECURITY;

CREATE POLICY nsa_tenant_isolation ON notification_security_audit
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

-- Immutability: no UPDATE or DELETE allowed via RLS policies
CREATE POLICY nsa_deny_update ON notification_security_audit
    FOR UPDATE
    USING (false);

CREATE POLICY nsa_deny_delete ON notification_security_audit
    FOR DELETE
    USING (false);

-- ------------------------------------------------------------
-- notification_suppressions
-- Persistent suppression records with soft-delete semantics.
-- Hard-delete is never performed; is_active = false is the tombstone.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_suppressions (
    suppression_id   VARCHAR(64)  NOT NULL,
    project_id       VARCHAR(64)  NOT NULL,
    recipient_id     VARCHAR(64)  NOT NULL,
    channel          VARCHAR(32),
    reason           VARCHAR(64)  NOT NULL,
    suppression_type VARCHAR(32)  NOT NULL,
    created_by       VARCHAR(64)  NOT NULL,
    expires_at       TIMESTAMPTZ,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    removed_at       TIMESTAMPTZ,
    removed_by       VARCHAR(64),
    CONSTRAINT pk_notification_suppressions PRIMARY KEY (project_id, suppression_id),
    CONSTRAINT uq_notification_suppression_scope
        UNIQUE (project_id, recipient_id, channel, suppression_type)
);

-- Index for suppression lookups during dispatch
CREATE INDEX IF NOT EXISTS idx_ns_lookup
    ON notification_suppressions (project_id, recipient_id, channel, is_active);

-- Enable Row-Level Security
ALTER TABLE notification_suppressions ENABLE ROW LEVEL SECURITY;

CREATE POLICY ns_tenant_isolation ON notification_suppressions
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

-- No hard-delete allowed
CREATE POLICY ns_deny_delete ON notification_suppressions
    FOR DELETE
    USING (false);

-- ------------------------------------------------------------
-- notification_rate_limit_state
-- Persistent sliding-window rate limit counters for multi-node deployments.
-- In-process AtomicInteger counters handle single-node; this table is
-- used for cross-node coordination when horizontally scaled.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_rate_limit_state (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    project_id       VARCHAR(64)  NOT NULL,
    dimension_key    TEXT         NOT NULL,
    window_start     TIMESTAMPTZ  NOT NULL,
    count            BIGINT       NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_notification_rate_limit PRIMARY KEY (id),
    CONSTRAINT uq_notification_rate_limit_window
        UNIQUE (project_id, dimension_key, window_start)
);

-- Index for window-based lookups
CREATE INDEX IF NOT EXISTS idx_nrl_lookup
    ON notification_rate_limit_state (project_id, dimension_key, window_start);

-- Enable Row-Level Security
ALTER TABLE notification_rate_limit_state ENABLE ROW LEVEL SECURITY;

CREATE POLICY nrl_tenant_isolation ON notification_rate_limit_state
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));
