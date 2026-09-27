-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- PERSISTENT AI USER MEMORY & CONVERSATION CONTEXT DDL SPECIFICATION (V20261221)
-- ====================================================================================

CREATE TABLE ai_user_memory (
    memory_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    customer_id VARCHAR(50) NOT NULL,
    context_category VARCHAR(50) NOT NULL DEFAULT 'PREFERENCE',
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, memory_id),
    UNIQUE (project_id, customer_id, preference_key)
);

CREATE TABLE ai_conversation_context (
    context_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    customer_id VARCHAR(50) NOT NULL,
    conversation_topic VARCHAR(150),
    current_order_id VARCHAR(50),
    current_quotation_id VARCHAR(50),
    active_workflow VARCHAR(50) DEFAULT 'GENERAL_ASSISTANCE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, context_id),
    UNIQUE (project_id, customer_id)
);

CREATE INDEX idx_ai_user_memory_cust ON ai_user_memory (project_id, customer_id, is_active);
CREATE INDEX idx_ai_conversation_context_cust ON ai_conversation_context (project_id, customer_id);

ALTER TABLE ai_user_memory ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_user_memory FORCE ROW LEVEL SECURITY;

ALTER TABLE ai_conversation_context ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_conversation_context FORCE ROW LEVEL SECURITY;

CREATE POLICY ai_user_memory_tenant_isolation ON ai_user_memory
    FOR ALL USING (project_id = CURRENT_SETTING('app.current_project_id', true));

CREATE POLICY ai_conversation_context_tenant_isolation ON ai_conversation_context
    FOR ALL USING (project_id = CURRENT_SETTING('app.current_project_id', true));
