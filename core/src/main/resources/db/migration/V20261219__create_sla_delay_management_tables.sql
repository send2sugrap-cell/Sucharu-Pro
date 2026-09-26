-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- BI-09 — SLA & DELAY MANAGEMENT DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE sla_order_commitments (
    commitment_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    order_id VARCHAR(50) NOT NULL,
    job_id VARCHAR(50),
    customer_id VARCHAR(50) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    promised_delivery_date TIMESTAMPTZ NOT NULL,
    planned_production_completion_date TIMESTAMPTZ,
    actual_production_completion_date TIMESTAMPTZ,
    actual_delivery_date TIMESTAMPTZ,

    sla_status VARCHAR(30) NOT NULL DEFAULT 'ON_TRACK' CHECK (sla_status IN ('NOT_STARTED', 'ON_TRACK', 'AT_RISK', 'DUE_TODAY', 'OVERDUE', 'COMPLETED_ON_TIME', 'COMPLETED_LATE')),
    delay_days INT NOT NULL DEFAULT 0,
    current_stage VARCHAR(50) NOT NULL DEFAULT 'PRINTING',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, commitment_id),
    UNIQUE (project_id, order_id)
);

CREATE TABLE sla_delay_records (
    delay_record_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    commitment_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    delay_category VARCHAR(50) NOT NULL CHECK (delay_category IN ('CUSTOMER_APPROVAL_DELAY', 'DESIGN_CHANGE', 'QC_REWORK', 'PRODUCTION_DELAY', 'MACHINE_OPERATION_DELAY', 'OUTSOURCED_FINISHING_DELAY', 'DELIVERY_DISPATCH_DELAY', 'CUSTOMER_UNAVAILABLE', 'PAYMENT_HOLD', 'MATERIAL_SUPPLIER_DEPENDENCY', 'OTHER')),
    responsible_stage VARCHAR(50) NOT NULL DEFAULT 'PRODUCTION',
    delay_duration_days INT NOT NULL DEFAULT 1 CHECK (delay_duration_days > 0),
    root_cause_description TEXT,

    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    recorded_by VARCHAR(50) NOT NULL,
    PRIMARY KEY (project_id, delay_record_id),
    FOREIGN KEY (project_id, commitment_id) REFERENCES sla_order_commitments(project_id, commitment_id) ON DELETE CASCADE
);

CREATE INDEX idx_sla_commitments_status ON sla_order_commitments (project_id, sla_status);
CREATE INDEX idx_sla_delay_records_order ON sla_delay_records (project_id, order_id);
