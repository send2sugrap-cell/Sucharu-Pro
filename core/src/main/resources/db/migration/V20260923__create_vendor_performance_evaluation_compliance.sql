-- Module 12 Step 09: Vendor Performance, Evaluation & Compliance
-- Canonical PostgreSQL Schema with FORCE ROW LEVEL SECURITY

-- 1. Vendor Performance KPIs
CREATE TABLE IF NOT EXISTS vendor_performance_kpis (
    project_id VARCHAR(64) NOT NULL,
    kpi_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    kpi_type VARCHAR(64) NOT NULL DEFAULT 'OPERATIONAL',
    measurement_method VARCHAR(64) NOT NULL DEFAULT 'AUTOMATED',
    target_value NUMERIC(19, 4) NOT NULL,
    minimum_acceptable_value NUMERIC(19, 4),
    maximum_acceptable_value NUMERIC(19, 4),
    unit VARCHAR(32) NOT NULL DEFAULT '%',
    direction VARCHAR(64) NOT NULL DEFAULT 'HIGHER_IS_BETTER',
    weight NUMERIC(9, 4) NOT NULL DEFAULT 1.0,
    status VARCHAR(64) NOT NULL DEFAULT 'ACTIVE',
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    CONSTRAINT pk_vendor_performance_kpis PRIMARY KEY (project_id, kpi_id),
    CONSTRAINT uq_vendor_performance_kpi_code UNIQUE (project_id, code)
);

-- 2. Vendor Performance Measurements
CREATE TABLE IF NOT EXISTS vendor_performance_measurements (
    project_id VARCHAR(64) NOT NULL,
    measurement_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    kpi_id VARCHAR(64) NOT NULL,
    kpi_code VARCHAR(64) NOT NULL,
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    actual_value NUMERIC(19, 4) NOT NULL,
    numerator NUMERIC(19, 4) NOT NULL DEFAULT 0,
    denominator NUMERIC(19, 4) NOT NULL DEFAULT 0,
    unit VARCHAR(32) NOT NULL DEFAULT '%',
    sample_size INT NOT NULL DEFAULT 1,
    confidence_state VARCHAR(64) NOT NULL DEFAULT 'SUFFICIENT_DATA',
    calculation_version VARCHAR(32) NOT NULL DEFAULT '1.0',
    measured_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    measured_by VARCHAR(64) NOT NULL DEFAULT 'system',
    CONSTRAINT pk_vendor_performance_measurements PRIMARY KEY (project_id, measurement_id),
    CONSTRAINT fk_vpm_kpi FOREIGN KEY (project_id, kpi_id)
        REFERENCES vendor_performance_kpis(project_id, kpi_id) ON DELETE CASCADE
);

-- 3. Vendor Performance Scorecards
CREATE TABLE IF NOT EXISTS vendor_performance_scorecards (
    project_id VARCHAR(64) NOT NULL,
    scorecard_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    period_type VARCHAR(64) NOT NULL DEFAULT 'MONTHLY',
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    overall_score NUMERIC(9, 4) NOT NULL,
    rating VARCHAR(64) NOT NULL DEFAULT 'ACCEPTABLE',
    risk_level VARCHAR(64) NOT NULL DEFAULT 'LOW',
    data_completeness NUMERIC(9, 4) NOT NULL DEFAULT 100.0,
    sample_size INT NOT NULL DEFAULT 0,
    calculation_version VARCHAR(32) NOT NULL DEFAULT '1.0',
    status VARCHAR(64) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(64),
    CONSTRAINT pk_vendor_performance_scorecards PRIMARY KEY (project_id, scorecard_id)
);

-- 4. Vendor Performance Scorecard Items
CREATE TABLE IF NOT EXISTS vendor_performance_scorecard_items (
    project_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    scorecard_id VARCHAR(64) NOT NULL,
    kpi_id VARCHAR(64) NOT NULL,
    kpi_code VARCHAR(64) NOT NULL,
    kpi_name VARCHAR(255) NOT NULL,
    kpi_type VARCHAR(64) NOT NULL,
    weight NUMERIC(9, 4) NOT NULL,
    direction VARCHAR(64) NOT NULL,
    target_value NUMERIC(19, 4) NOT NULL,
    actual_value NUMERIC(19, 4) NOT NULL,
    normalized_score NUMERIC(9, 4) NOT NULL,
    weighted_score NUMERIC(9, 4) NOT NULL,
    numerator NUMERIC(19, 4) NOT NULL DEFAULT 0,
    denominator NUMERIC(19, 4) NOT NULL DEFAULT 0,
    unit VARCHAR(32) NOT NULL DEFAULT '%',
    sample_size INT NOT NULL DEFAULT 1,
    confidence_state VARCHAR(64) NOT NULL DEFAULT 'SUFFICIENT_DATA',
    CONSTRAINT pk_vendor_performance_scorecard_items PRIMARY KEY (project_id, item_id),
    CONSTRAINT fk_vpsi_scorecard FOREIGN KEY (project_id, scorecard_id)
        REFERENCES vendor_performance_scorecards(project_id, scorecard_id) ON DELETE CASCADE
);

-- 5. Vendor Evaluations
CREATE TABLE IF NOT EXISTS vendor_evaluations (
    project_id VARCHAR(64) NOT NULL,
    evaluation_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    scorecard_id VARCHAR(64),
    period_type VARCHAR(64) NOT NULL DEFAULT 'MONTHLY',
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    evaluator_id VARCHAR(64) NOT NULL,
    evaluator_name VARCHAR(255) NOT NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'DRAFT',
    decision VARCHAR(64),
    evaluation_score NUMERIC(9, 4) NOT NULL DEFAULT 0,
    rating VARCHAR(64) NOT NULL DEFAULT 'ACCEPTABLE',
    evaluator_comments TEXT,
    review_comments TEXT,
    rejection_reason TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE,
    submitted_by VARCHAR(64),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by VARCHAR(64),
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(64),
    finalized_at TIMESTAMP WITH TIME ZONE,
    finalized_by VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    CONSTRAINT pk_vendor_evaluations PRIMARY KEY (project_id, evaluation_id)
);

-- 6. Vendor Evaluation Criteria
CREATE TABLE IF NOT EXISTS vendor_evaluation_criteria (
    project_id VARCHAR(64) NOT NULL,
    criterion_id VARCHAR(64) NOT NULL,
    evaluation_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    weight NUMERIC(9, 4) NOT NULL,
    score NUMERIC(9, 4) NOT NULL,
    comments TEXT,
    CONSTRAINT pk_vendor_evaluation_criteria PRIMARY KEY (project_id, criterion_id),
    CONSTRAINT fk_vec_evaluation FOREIGN KEY (project_id, evaluation_id)
        REFERENCES vendor_evaluations(project_id, evaluation_id) ON DELETE CASCADE
);

-- 7. Vendor Compliance Requirements
CREATE TABLE IF NOT EXISTS vendor_compliance_requirements (
    project_id VARCHAR(64) NOT NULL,
    requirement_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    requirement_type VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    risk_level VARCHAR(64) NOT NULL DEFAULT 'HIGH',
    validity_days INT DEFAULT 365,
    status VARCHAR(64) NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    CONSTRAINT pk_vendor_compliance_requirements PRIMARY KEY (project_id, requirement_id),
    CONSTRAINT uq_vendor_compliance_req_code UNIQUE (project_id, code)
);

-- 8. Vendor Compliance Records
CREATE TABLE IF NOT EXISTS vendor_compliance_records (
    project_id VARCHAR(64) NOT NULL,
    record_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    requirement_id VARCHAR(64) NOT NULL,
    requirement_code VARCHAR(64) NOT NULL,
    requirement_name VARCHAR(255) NOT NULL,
    requirement_type VARCHAR(64) NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    effective_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(64) NOT NULL DEFAULT 'PENDING',
    risk_level VARCHAR(64) NOT NULL DEFAULT 'LOW',
    verification_status VARCHAR(64) NOT NULL DEFAULT 'PENDING',
    verified_by VARCHAR(64),
    verified_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    CONSTRAINT pk_vendor_compliance_records PRIMARY KEY (project_id, record_id),
    CONSTRAINT fk_vcr_requirement FOREIGN KEY (project_id, requirement_id)
        REFERENCES vendor_compliance_requirements(project_id, requirement_id) ON DELETE RESTRICT
);

-- 9. Vendor Compliance Evidence
CREATE TABLE IF NOT EXISTS vendor_compliance_evidence (
    project_id VARCHAR(64) NOT NULL,
    evidence_id VARCHAR(64) NOT NULL,
    record_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    evidence_type VARCHAR(64) NOT NULL DEFAULT 'DOCUMENT',
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    checksum VARCHAR(128),
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(128),
    uploaded_by VARCHAR(64) NOT NULL DEFAULT 'system',
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_vendor_compliance_evidence PRIMARY KEY (project_id, evidence_id),
    CONSTRAINT fk_vce_record FOREIGN KEY (project_id, record_id)
        REFERENCES vendor_compliance_records(project_id, record_id) ON DELETE CASCADE
);

-- 10. Vendor Corrective Actions (CAPA)
CREATE TABLE IF NOT EXISTS vendor_corrective_actions (
    project_id VARCHAR(64) NOT NULL,
    action_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64),
    issue_description TEXT NOT NULL,
    root_cause TEXT,
    action_plan TEXT NOT NULL,
    assigned_to VARCHAR(64) NOT NULL,
    assigned_to_name VARCHAR(255) NOT NULL,
    priority VARCHAR(64) NOT NULL DEFAULT 'MEDIUM',
    due_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'OPEN',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    verification_notes TEXT,
    verified_by VARCHAR(64),
    verified_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    CONSTRAINT pk_vendor_corrective_actions PRIMARY KEY (project_id, action_id)
);

-- 11. Vendor Risk Indicators
CREATE TABLE IF NOT EXISTS vendor_performance_risk_indicators (
    project_id VARCHAR(64) NOT NULL,
    risk_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    risk_type VARCHAR(64) NOT NULL,
    severity VARCHAR(64) NOT NULL DEFAULT 'MEDIUM',
    source VARCHAR(64) NOT NULL,
    source_id VARCHAR(64),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    evidence_reference TEXT,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(64) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_vendor_risk_indicators PRIMARY KEY (project_id, risk_id)
);

-- 12. Vendor Performance Audit Events
CREATE TABLE IF NOT EXISTS vendor_performance_audit_events (
    project_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    action VARCHAR(255) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64),
    details TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_vendor_performance_audit_events PRIMARY KEY (project_id, audit_id)
);

-- Enable & Force Row Level Security on all 12 tables
ALTER TABLE vendor_performance_kpis ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_performance_kpis FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_performance_measurements ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_performance_measurements FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_performance_scorecards ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_performance_scorecards FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_performance_scorecard_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_performance_scorecard_items FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_evaluations ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_evaluations FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_evaluation_criteria ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_evaluation_criteria FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_compliance_requirements ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_compliance_requirements FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_compliance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_compliance_records FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_compliance_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_compliance_evidence FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_corrective_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_corrective_actions FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_performance_risk_indicators ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_performance_risk_indicators FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_performance_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_performance_audit_events FORCE ROW LEVEL SECURITY;

-- Tenant Isolation Policies
CREATE POLICY vpk_tenant_isolation ON vendor_performance_kpis
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vpm_tenant_isolation ON vendor_performance_measurements
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vps_tenant_isolation ON vendor_performance_scorecards
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vpsi_tenant_isolation ON vendor_performance_scorecard_items
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY ve_tenant_isolation ON vendor_evaluations
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vec_tenant_isolation ON vendor_evaluation_criteria
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vcr_req_tenant_isolation ON vendor_compliance_requirements
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vcr_rec_tenant_isolation ON vendor_compliance_records
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vce_tenant_isolation ON vendor_compliance_evidence
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vca_tenant_isolation ON vendor_corrective_actions
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vri_tenant_isolation ON vendor_performance_risk_indicators
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE POLICY vpae_tenant_isolation ON vendor_performance_audit_events
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

-- Composite & Lookup Indexes for High-Performance Queries
CREATE INDEX idx_vpk_lookup ON vendor_performance_kpis(project_id, code, status);
CREATE INDEX idx_vpm_lookup ON vendor_performance_measurements(project_id, vendor_id, kpi_id, period_start, period_end);
CREATE INDEX idx_vps_lookup ON vendor_performance_scorecards(project_id, vendor_id, period_start, period_end, status);
CREATE INDEX idx_vpsi_lookup ON vendor_performance_scorecard_items(project_id, scorecard_id);
CREATE INDEX idx_ve_lookup ON vendor_evaluations(project_id, vendor_id, status, period_start, period_end);
CREATE INDEX idx_vec_lookup ON vendor_evaluation_criteria(project_id, evaluation_id);
CREATE INDEX idx_vcr_req_lookup ON vendor_compliance_requirements(project_id, code, status);
CREATE INDEX idx_vcr_rec_lookup ON vendor_compliance_records(project_id, vendor_id, requirement_id, status, expiry_date);
CREATE INDEX idx_vce_lookup ON vendor_compliance_evidence(project_id, record_id);
CREATE INDEX idx_vca_lookup ON vendor_corrective_actions(project_id, vendor_id, status, priority, due_date);
CREATE INDEX idx_vri_lookup ON vendor_performance_risk_indicators(project_id, vendor_id, status, severity);
CREATE INDEX idx_vpae_lookup ON vendor_performance_audit_events(project_id, entity_id, occurred_at);
