-- ============================================================================
-- SUCHARU PRO — INFRA-04 STEP 05 MIGRATION
-- PRODUCTION-GRADE WORKFLOW ORCHESTRATION, SAGA/COMPENSATION & APPROVAL TABLES
-- ============================================================================

-- 1. WORKFLOW DEFINITIONS TABLE
CREATE TABLE IF NOT EXISTS workflow_definitions (
    definition_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    workflow_name VARCHAR(128) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, definition_id)
);

ALTER TABLE workflow_definitions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_definitions_tenant_isolation ON workflow_definitions;
CREATE POLICY workflow_definitions_tenant_isolation ON workflow_definitions
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

-- 2. WORKFLOW VERSIONS TABLE
CREATE TABLE IF NOT EXISTS workflow_versions (
    definition_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    version_id VARCHAR(32) NOT NULL,
    definition_json TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_by VARCHAR(64) NOT NULL,
    PRIMARY KEY (project_id, definition_id, version_id),
    FOREIGN KEY (project_id, definition_id) REFERENCES workflow_definitions(project_id, definition_id) ON DELETE CASCADE
);

ALTER TABLE workflow_versions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_versions_tenant_isolation ON workflow_versions;
CREATE POLICY workflow_versions_tenant_isolation ON workflow_versions
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

-- 3. WORKFLOW INSTANCES TABLE
CREATE TABLE IF NOT EXISTS workflow_instances (
    workflow_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    definition_id VARCHAR(64) NOT NULL,
    version_id VARCHAR(32) NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    current_step_id VARCHAR(64),
    context_json TEXT NOT NULL DEFAULT '{}',
    correlation_id VARCHAR(64) NOT NULL,
    causation_id VARCHAR(64),
    request_id VARCHAR(64),
    actor_type VARCHAR(32) NOT NULL DEFAULT 'HUMAN',
    actor_id VARCHAR(64) NOT NULL,
    principal_type VARCHAR(32) NOT NULL DEFAULT 'HUMAN',
    idempotency_key VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    PRIMARY KEY (project_id, workflow_id)
);

ALTER TABLE workflow_instances ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_instances_tenant_isolation ON workflow_instances;
CREATE POLICY workflow_instances_tenant_isolation ON workflow_instances
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE INDEX IF NOT EXISTS idx_workflow_instances_tenant_status
    ON workflow_instances (project_id, status);
CREATE INDEX IF NOT EXISTS idx_workflow_instances_correlation
    ON workflow_instances (project_id, correlation_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_workflow_instances_idempotency
    ON workflow_instances (project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- 4. WORKFLOW STEPS TABLE
CREATE TABLE IF NOT EXISTS workflow_steps (
    step_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    definition_id VARCHAR(64) NOT NULL,
    version_id VARCHAR(32) NOT NULL,
    step_name VARCHAR(128) NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    sequence_order INT NOT NULL,
    config_json TEXT NOT NULL DEFAULT '{}',
    retry_policy_json TEXT,
    timeout_ms BIGINT NOT NULL DEFAULT 60000,
    compensation_step_id VARCHAR(64),
    required_capability VARCHAR(64),
    PRIMARY KEY (project_id, definition_id, version_id, step_id),
    FOREIGN KEY (project_id, definition_id, version_id) REFERENCES workflow_versions(project_id, definition_id, version_id) ON DELETE CASCADE
);

ALTER TABLE workflow_steps ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_steps_tenant_isolation ON workflow_steps;
CREATE POLICY workflow_steps_tenant_isolation ON workflow_steps
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

-- 5. WORKFLOW STEP EXECUTIONS TABLE
CREATE TABLE IF NOT EXISTS workflow_step_executions (
    step_execution_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    step_name VARCHAR(128) NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_number INT NOT NULL DEFAULT 1,
    input_json TEXT,
    output_json TEXT,
    error_message TEXT,
    failure_classification VARCHAR(32),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (project_id, step_execution_id),
    FOREIGN KEY (project_id, workflow_id) REFERENCES workflow_instances(project_id, workflow_id) ON DELETE CASCADE
);

ALTER TABLE workflow_step_executions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_step_executions_tenant_isolation ON workflow_step_executions;
CREATE POLICY workflow_step_executions_tenant_isolation ON workflow_step_executions
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE INDEX IF NOT EXISTS idx_workflow_step_exec_wf
    ON workflow_step_executions (project_id, workflow_id, execution_id);

-- 6. WORKFLOW TRANSITIONS TABLE (Immutable Audit Trail)
CREATE TABLE IF NOT EXISTS workflow_transitions (
    transition_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    from_status VARCHAR(32) NOT NULL,
    to_status VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    principal_type VARCHAR(32) NOT NULL,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    transitioned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, transition_id),
    FOREIGN KEY (project_id, workflow_id) REFERENCES workflow_instances(project_id, workflow_id) ON DELETE CASCADE
);

ALTER TABLE workflow_transitions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_transitions_tenant_isolation ON workflow_transitions;
CREATE POLICY workflow_transitions_tenant_isolation ON workflow_transitions
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE INDEX IF NOT EXISTS idx_workflow_transitions_wf
    ON workflow_transitions (project_id, workflow_id, transitioned_at);

-- 7. WORKFLOW VARIABLES TABLE
CREATE TABLE IF NOT EXISTS workflow_variables (
    variable_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    variable_key VARCHAR(128) NOT NULL,
    variable_value TEXT,
    value_type VARCHAR(32) NOT NULL DEFAULT 'STRING',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, workflow_id, variable_key),
    FOREIGN KEY (project_id, workflow_id) REFERENCES workflow_instances(project_id, workflow_id) ON DELETE CASCADE
);

ALTER TABLE workflow_variables ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_variables_tenant_isolation ON workflow_variables;
CREATE POLICY workflow_variables_tenant_isolation ON workflow_variables
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

-- 8. WORKFLOW COMPENSATIONS TABLE (Saga Reverse Rollback)
CREATE TABLE IF NOT EXISTS workflow_compensations (
    compensation_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    step_execution_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPENSATION_PENDING',
    attempt_number INT NOT NULL DEFAULT 1,
    payload_json TEXT,
    result_message TEXT,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (project_id, compensation_id),
    FOREIGN KEY (project_id, workflow_id) REFERENCES workflow_instances(project_id, workflow_id) ON DELETE CASCADE
);

ALTER TABLE workflow_compensations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_compensations_tenant_isolation ON workflow_compensations;
CREATE POLICY workflow_compensations_tenant_isolation ON workflow_compensations
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE INDEX IF NOT EXISTS idx_workflow_compensations_wf
    ON workflow_compensations (project_id, workflow_id, status);

-- 9. WORKFLOW APPROVAL POLICIES TABLE
CREATE TABLE IF NOT EXISTS workflow_approval_policies (
    policy_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    policy_name VARCHAR(128) NOT NULL,
    required_role VARCHAR(32) NOT NULL,
    required_capability VARCHAR(64),
    minimum_approvals INT NOT NULL DEFAULT 1,
    allow_self_approval BOOLEAN NOT NULL DEFAULT FALSE,
    timeout_ms BIGINT NOT NULL DEFAULT 86400000,
    escalation_role VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, policy_id)
);

ALTER TABLE workflow_approval_policies ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_approval_policies_tenant_isolation ON workflow_approval_policies;
CREATE POLICY workflow_approval_policies_tenant_isolation ON workflow_approval_policies
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

-- 10. WORKFLOW APPROVAL REQUESTS TABLE
CREATE TABLE IF NOT EXISTS workflow_approval_requests (
    approval_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    policy_id VARCHAR(64) NOT NULL,
    requester_id VARCHAR(64) NOT NULL,
    requester_role VARCHAR(32) NOT NULL,
    requester_principal_type VARCHAR(32) NOT NULL DEFAULT 'HUMAN',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    title VARCHAR(256) NOT NULL,
    summary TEXT,
    payload_json TEXT NOT NULL DEFAULT '{}',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, approval_id),
    FOREIGN KEY (project_id, workflow_id) REFERENCES workflow_instances(project_id, workflow_id) ON DELETE CASCADE
);

ALTER TABLE workflow_approval_requests ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_approval_requests_tenant_isolation ON workflow_approval_requests;
CREATE POLICY workflow_approval_requests_tenant_isolation ON workflow_approval_requests
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE INDEX IF NOT EXISTS idx_workflow_approvals_status
    ON workflow_approval_requests (project_id, status);

-- 11. WORKFLOW APPROVAL DECISIONS TABLE (Immutable Vote / Decision Log)
CREATE TABLE IF NOT EXISTS workflow_approval_decisions (
    decision_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    approval_id VARCHAR(64) NOT NULL,
    approver_id VARCHAR(64) NOT NULL,
    approver_role VARCHAR(32) NOT NULL,
    approver_principal_type VARCHAR(32) NOT NULL DEFAULT 'HUMAN',
    decision_type VARCHAR(32) NOT NULL,
    notes TEXT,
    human_confirmation_json TEXT,
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, decision_id),
    FOREIGN KEY (project_id, approval_id) REFERENCES workflow_approval_requests(project_id, approval_id) ON DELETE CASCADE
);

ALTER TABLE workflow_approval_decisions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_approval_decisions_tenant_isolation ON workflow_approval_decisions;
CREATE POLICY workflow_approval_decisions_tenant_isolation ON workflow_approval_decisions
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));

CREATE INDEX IF NOT EXISTS idx_workflow_decisions_approval
    ON workflow_approval_decisions (project_id, approval_id);

-- 12. WORKFLOW ESCALATIONS TABLE
CREATE TABLE IF NOT EXISTS workflow_escalations (
    escalation_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    approval_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    from_role VARCHAR(32) NOT NULL,
    to_role VARCHAR(32) NOT NULL,
    reason TEXT NOT NULL,
    escalated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, escalation_id),
    FOREIGN KEY (project_id, approval_id) REFERENCES workflow_approval_requests(project_id, approval_id) ON DELETE CASCADE
);

ALTER TABLE workflow_escalations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_escalations_tenant_isolation ON workflow_escalations;
CREATE POLICY workflow_escalations_tenant_isolation ON workflow_escalations
    FOR ALL USING (project_id = current_setting('app.current_project_id', true));
