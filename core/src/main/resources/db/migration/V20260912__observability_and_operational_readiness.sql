-- ============================================================
-- SUCHARU PRO — INFRA-04 STEP 09
-- Production Observability, Delivery Analytics & Operational Readiness
-- Migration: V20260912__observability_and_operational_readiness.sql
-- ============================================================

-- ------------------------------------------------------------
-- operational_alerts
-- Tracks active and historic operational alerts per tenant
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operational_alerts (
    alert_id            VARCHAR(64)  NOT NULL,
    project_id          VARCHAR(64)  NOT NULL,
    alert_key           VARCHAR(64)  NOT NULL,
    deduplication_key   VARCHAR(128) NOT NULL,
    title               VARCHAR(128) NOT NULL,
    summary             TEXT         NOT NULL,
    severity            VARCHAR(32)  NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    subsystem           VARCHAR(64)  NOT NULL,
    failure_class       VARCHAR(64),
    occurrences         INT          NOT NULL DEFAULT 1,
    first_occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMPTZ,
    acknowledged_by     VARCHAR(64),
    resolution_notes    TEXT,
    CONSTRAINT pk_operational_alerts PRIMARY KEY (project_id, alert_id),
    CONSTRAINT uq_operational_alert_dedup UNIQUE (project_id, deduplication_key)
);

CREATE INDEX IF NOT EXISTS idx_operational_alerts_status
    ON operational_alerts (project_id, status, last_occurred_at DESC);

ALTER TABLE operational_alerts ENABLE ROW LEVEL SECURITY;

CREATE POLICY operational_alerts_tenant_isolation ON operational_alerts
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

-- ------------------------------------------------------------
-- slo_definitions
-- Standard platform and tenant Service Level Objective configs
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS slo_definitions (
    slo_id                     VARCHAR(64)   NOT NULL PRIMARY KEY,
    name                       VARCHAR(128)  NOT NULL,
    subsystem                  VARCHAR(64)   NOT NULL,
    target_percentage          NUMERIC(5, 2) NOT NULL,
    warning_threshold          NUMERIC(5, 2) NOT NULL,
    critical_threshold         NUMERIC(5, 2) NOT NULL,
    measurement_window_seconds BIGINT        NOT NULL DEFAULT 3600
);

-- ------------------------------------------------------------
-- slo_measurements
-- Periodic aggregated SLO compliance snapshots
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS slo_measurements (
    measurement_id     VARCHAR(64)   NOT NULL PRIMARY KEY,
    slo_id             VARCHAR(64)   NOT NULL REFERENCES slo_definitions(slo_id),
    current_percentage NUMERIC(5, 2) NOT NULL,
    is_meeting_slo     BOOLEAN       NOT NULL,
    status             VARCHAR(32)   NOT NULL,
    evaluated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_slo_measurements_time
    ON slo_measurements (slo_id, evaluated_at DESC);
