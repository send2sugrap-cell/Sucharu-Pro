-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- BI-07 — BANGLADESH FINANCE & COMPLIANCE READINESS DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE tax_rule_configurations (
    tax_rule_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    tax_code VARCHAR(50) NOT NULL,
    tax_name VARCHAR(150) NOT NULL,
    tax_category VARCHAR(50) NOT NULL DEFAULT 'VAT_STANDARD' CHECK (tax_category IN ('VAT_STANDARD', 'VAT_REDUCED', 'VAT_EXEMPT', 'WITHHOLDING_TAX_AIT', 'SUPPLEMENTARY_DUTY', 'ZERO_RATED')),
    rate_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00 CHECK (rate_percentage >= 0.00),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ,
    effective_until TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, tax_rule_id),
    UNIQUE (project_id, tax_code)
);

CREATE TABLE accounting_period_locks (
    period_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    period_name VARCHAR(100) NOT NULL,
    fiscal_year VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED', 'LOCKED_PERIOD')),
    closed_at TIMESTAMPTZ,
    closed_by VARCHAR(50),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, period_id),
    UNIQUE (project_id, period_name)
);

CREATE TABLE invoice_tax_snapshots (
    snapshot_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    invoice_id VARCHAR(50) NOT NULL,
    subtotal_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    applicable_tax_code VARCHAR(50) NOT NULL,
    tax_rate_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    customer_bin VARCHAR(50),
    customer_tin VARCHAR(50),
    is_immutable BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, snapshot_id),
    UNIQUE (project_id, invoice_id)
);

CREATE INDEX idx_tax_rules_code ON tax_rule_configurations (project_id, tax_code, is_active);
CREATE INDEX idx_period_locks_status ON accounting_period_locks (project_id, status);
