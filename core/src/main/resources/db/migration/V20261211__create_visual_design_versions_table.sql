-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- FORM 02 — VISUAL DESIGN VERSIONS DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE visual_design_versions (
    version_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    design_id VARCHAR(50) NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    configuration_snapshot_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    PRIMARY KEY (project_id, version_id),
    UNIQUE (project_id, design_id, version_number)
);

CREATE INDEX idx_visual_design_versions_design ON visual_design_versions (project_id, design_id, version_number DESC);
