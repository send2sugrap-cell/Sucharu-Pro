-- Module 22 Step 01: Preflight Engine Foundation
-- Establishes preflight runs, rule definitions, rule executions, and findings tables with RLS policies

-- 1. Preflight Runs
CREATE TABLE IF NOT EXISTS preflight_runs (
    preflight_run_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    artwork_id VARCHAR(64) NOT NULL,
    artwork_version_id VARCHAR(64),
    proof_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    overall_result VARCHAR(32) NOT NULL DEFAULT 'NOT_EVALUATED',
    engine_version VARCHAR(32) NOT NULL DEFAULT 'v1.0',
    idempotency_key VARCHAR(128),
    requested_by VARCHAR(64) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_preflight_runs_tenant_artwork ON preflight_runs(tenant_id, artwork_id);
CREATE INDEX IF NOT EXISTS idx_preflight_runs_tenant_job ON preflight_runs(tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_preflight_runs_idempotency ON preflight_runs(tenant_id, idempotency_key);

ALTER TABLE preflight_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE preflight_runs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS preflight_runs_tenant_isolation ON preflight_runs;
CREATE POLICY preflight_runs_tenant_isolation ON preflight_runs
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );

-- 2. Preflight Rule Definitions
CREATE TABLE IF NOT EXISTS preflight_rule_definitions (
    rule_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'ERROR',
    description TEXT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version VARCHAR(32) NOT NULL DEFAULT 'v1.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_preflight_rules_tenant_code ON preflight_rule_definitions(tenant_id, rule_code);

ALTER TABLE preflight_rule_definitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE preflight_rule_definitions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS preflight_rule_definitions_tenant_isolation ON preflight_rule_definitions;
CREATE POLICY preflight_rule_definitions_tenant_isolation ON preflight_rule_definitions
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );

-- 3. Preflight Rule Executions
CREATE TABLE IF NOT EXISTS preflight_rule_executions (
    execution_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    preflight_run_id VARCHAR(64) NOT NULL REFERENCES preflight_runs(preflight_run_id) ON DELETE CASCADE,
    rule_id VARCHAR(64) NOT NULL REFERENCES preflight_rule_definitions(rule_id) ON DELETE CASCADE,
    rule_code VARCHAR(64) NOT NULL,
    result VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_preflight_exec_tenant_run ON preflight_rule_executions(tenant_id, preflight_run_id);

ALTER TABLE preflight_rule_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE preflight_rule_executions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS preflight_rule_executions_tenant_isolation ON preflight_rule_executions;
CREATE POLICY preflight_rule_executions_tenant_isolation ON preflight_rule_executions
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );

-- 4. Preflight Findings
CREATE TABLE IF NOT EXISTS preflight_findings (
    finding_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    preflight_run_id VARCHAR(64) NOT NULL REFERENCES preflight_runs(preflight_run_id) ON DELETE CASCADE,
    execution_id VARCHAR(64) REFERENCES preflight_rule_executions(execution_id) ON DELETE CASCADE,
    rule_code VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    expected_value TEXT,
    actual_value TEXT,
    location_context TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_preflight_findings_tenant_run ON preflight_findings(tenant_id, preflight_run_id);

ALTER TABLE preflight_findings ENABLE ROW LEVEL SECURITY;
ALTER TABLE preflight_findings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS preflight_findings_tenant_isolation ON preflight_findings;
CREATE POLICY preflight_findings_tenant_isolation ON preflight_findings
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );
