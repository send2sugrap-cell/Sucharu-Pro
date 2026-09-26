-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- FORM 05 — ERP / ORDER / FULFILLMENT INTEGRATION DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE erp_workflow_orchestrations (
    orchestration_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    customer_action VARCHAR(50) NOT NULL CHECK (customer_action IN ('VIEW_PRODUCT', 'SELECT_QUANTITY', 'APPLY_OFFER', 'GET_QUOTE', 'ORDER_NOW', 'REQUEST_CUSTOM_QUOTE')),
    customer_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    offer_id VARCHAR(50),
    price_config_id VARCHAR(50),

    -- ERP Workflow Status Tracking
    workflow_status VARCHAR(50) NOT NULL DEFAULT 'INITIATED' CHECK (workflow_status IN ('INITIATED', 'QUOTATION_CREATED', 'CUSTOMER_APPROVED', 'ORDER_CONFIRMED', 'PRODUCTION_HANDOFF', 'IN_PRODUCTION', 'QC_PASSED', 'STOCK_ALLOCATED', 'DISPATCHED', 'INVOICED', 'COMPLETED', 'FAILED')),
    quotation_id VARCHAR(50),
    order_id VARCHAR(50),
    job_card_id VARCHAR(50),
    qc_inspection_id VARCHAR(50),
    challan_id VARCHAR(50),
    invoice_id VARCHAR(50),

    order_quantity INT NOT NULL CHECK (order_quantity > 0),
    order_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    special_instructions TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, orchestration_id)
);

CREATE TABLE erp_workflow_action_logs (
    log_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    orchestration_id VARCHAR(50) NOT NULL,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    performed_by VARCHAR(50) NOT NULL,
    action_notes TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, log_id),
    FOREIGN KEY (project_id, orchestration_id) REFERENCES erp_workflow_orchestrations(project_id, orchestration_id) ON DELETE CASCADE
);

CREATE INDEX idx_erp_orch_order ON erp_workflow_orchestrations (project_id, order_id);
CREATE INDEX idx_erp_orch_status ON erp_workflow_orchestrations (project_id, workflow_status);
