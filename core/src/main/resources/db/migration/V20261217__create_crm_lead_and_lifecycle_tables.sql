-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- BI-05 — LEAD -> CUSTOMER -> REPEAT CRM DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE crm_leads (
    lead_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    lead_name VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    company_name VARCHAR(150),
    interest_product VARCHAR(150),
    lead_status VARCHAR(30) NOT NULL DEFAULT 'NEW' CHECK (lead_status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'QUOTED', 'CONVERTED', 'LOST')),
    assigned_staff_id VARCHAR(50),
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, lead_id)
);

CREATE TABLE crm_lead_conversions (
    conversion_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    lead_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    initial_quotation_id VARCHAR(50),
    initial_order_id VARCHAR(50),
    converted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    converted_by VARCHAR(50) NOT NULL,
    PRIMARY KEY (project_id, conversion_id),
    FOREIGN KEY (project_id, lead_id) REFERENCES crm_leads(project_id, lead_id) ON DELETE CASCADE
);

CREATE INDEX idx_crm_leads_status ON crm_leads (project_id, lead_status);
CREATE INDEX idx_crm_conversions_cust ON crm_lead_conversions (project_id, customer_id);
