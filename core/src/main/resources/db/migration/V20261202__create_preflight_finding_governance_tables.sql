-- Module 22 Step 08: Preflight Findings & Correction Governance Tables
-- Adds finding lifecycle columns to preflight_findings and creates preflight_finding_corrections

ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'OPEN';
ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS acknowledged_by VARCHAR(64);
ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS acknowledged_at TIMESTAMPTZ;
ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;
ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS resolved_by VARCHAR(64);
ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS waiver_reason TEXT;
ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS waived_by VARCHAR(64);
ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS waived_at TIMESTAMPTZ;
ALTER TABLE preflight_findings ADD COLUMN IF NOT EXISTS revalidation_run_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_preflight_findings_tenant_status ON preflight_findings(tenant_id, status);

CREATE TABLE IF NOT EXISTS preflight_finding_corrections (
    correction_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    finding_id VARCHAR(64) NOT NULL REFERENCES preflight_findings(finding_id) ON DELETE CASCADE,
    preflight_run_id VARCHAR(64) NOT NULL REFERENCES preflight_runs(preflight_run_id) ON DELETE CASCADE,
    correction_type VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    artwork_version_id VARCHAR(64),
    submitted_by VARCHAR(64) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revalidation_run_id VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_preflight_corrections_tenant_finding ON preflight_finding_corrections(tenant_id, finding_id);

ALTER TABLE preflight_finding_corrections ENABLE ROW LEVEL SECURITY;
ALTER TABLE preflight_finding_corrections FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS preflight_finding_corrections_tenant_isolation ON preflight_finding_corrections;
CREATE POLICY preflight_finding_corrections_tenant_isolation ON preflight_finding_corrections
    FOR ALL
    USING (
        tenant_id = current_setting('app.current_project_id', true)
        OR current_setting('app.current_project_id', true) = 'GLOBAL_ADMIN'
    );
