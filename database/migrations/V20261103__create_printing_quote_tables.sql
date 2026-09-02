-- V20261103__create_printing_quote_tables.sql
-- Module 17 Step 02: Smart Printing Calculator — Quotation, Costing & Price Intelligence Engine
-- All tables are tenant-scoped with ENABLE ROW LEVEL SECURITY + FORCE ROW LEVEL SECURITY.

-- ============================================================
-- 1. printing_quotes  (canonical quote header)
-- ============================================================
CREATE TABLE IF NOT EXISTS printing_quotes (
    quote_id            VARCHAR(64)    PRIMARY KEY,
    tenant_id           VARCHAR(64)    NOT NULL,
    project_id          VARCHAR(64)    NOT NULL,
    quote_number        VARCHAR(64)    NOT NULL,
    job_title           VARCHAR(255)   NOT NULL,
    calculation_id      VARCHAR(64)    NOT NULL,       -- FK → printing_calculations.calculation_id (Step 01)
    request_fingerprint VARCHAR(128)   NOT NULL,       -- Step 01 request fingerprint (provenance)
    status              VARCHAR(32)    NOT NULL DEFAULT 'DRAFT',
    current_version     INT            NOT NULL DEFAULT 0,
    currency            VARCHAR(8)     NOT NULL DEFAULT 'BDT',
    ordered_quantity    BIGINT         NOT NULL,
    customer_ref        VARCHAR(128),                  -- optional customer context (no mutation)
    customer_note       TEXT,
    internal_note       TEXT,
    idempotency_key     VARCHAR(128),
    created_by          VARCHAR(128)   NOT NULL,
    created_at          BIGINT         NOT NULL,
    updated_at          BIGINT         NOT NULL,
    approved_at         BIGINT,
    approved_by         VARCHAR(128),
    expires_at          BIGINT,
    integrity_hash      VARCHAR(128)   NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_printing_quotes_number_tenant
    ON printing_quotes (tenant_id, quote_number);
CREATE INDEX IF NOT EXISTS idx_printing_quotes_tenant_status
    ON printing_quotes (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_printing_quotes_calculation_id
    ON printing_quotes (tenant_id, calculation_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_printing_quotes_idempotency
    ON printing_quotes (tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE printing_quotes ENABLE ROW LEVEL SECURITY;
ALTER TABLE printing_quotes FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS printing_quotes_tenant_isolation ON printing_quotes;
CREATE POLICY printing_quotes_tenant_isolation ON printing_quotes
    USING (project_id = current_setting('app.current_tenant', TRUE));

-- ============================================================
-- 2. printing_quote_versions  (immutable calculation snapshots)
-- ============================================================
CREATE TABLE IF NOT EXISTS printing_quote_versions (
    version_id                  VARCHAR(64)    PRIMARY KEY,
    quote_id                    VARCHAR(64)    NOT NULL REFERENCES printing_quotes(quote_id),
    tenant_id                   VARCHAR(64)    NOT NULL,
    project_id                  VARCHAR(64)    NOT NULL,
    version_number              INT            NOT NULL,
    status                      VARCHAR(32)    NOT NULL,
    currency                    VARCHAR(8)     NOT NULL,

    -- Step 01 provenance
    calculation_id              VARCHAR(64)    NOT NULL,
    spec_fingerprint            VARCHAR(128)   NOT NULL,
    calc_fingerprint            VARCHAR(128)   NOT NULL,

    -- Quantity economics
    ordered_quantity            BIGINT         NOT NULL,
    produced_quantity           BIGINT         NOT NULL,
    sellable_quantity           BIGINT         NOT NULL,
    wastage_quantity            BIGINT         NOT NULL,
    wastage_percentage          NUMERIC(10, 4) NOT NULL,
    imposition_ups              INT            NOT NULL DEFAULT 1,

    -- Costing assumptions fingerprint (JSON blob)
    costing_assumptions_json    TEXT           NOT NULL,
    pricing_assumptions_json    TEXT           NOT NULL,

    -- Totals (scale=4)
    total_cost                  NUMERIC(18, 4) NOT NULL,
    unit_cost                   NUMERIC(18, 4) NOT NULL,

    -- Pricing
    base_selling_price          NUMERIC(18, 4) NOT NULL,
    discount_type               VARCHAR(32),
    discount_value              NUMERIC(18, 4) NOT NULL DEFAULT 0,
    discount_amount             NUMERIC(18, 4) NOT NULL DEFAULT 0,
    tax_percentage              NUMERIC(10, 4) NOT NULL DEFAULT 0,
    tax_amount                  NUMERIC(18, 4) NOT NULL DEFAULT 0,
    final_quote_total           NUMERIC(18, 4) NOT NULL,

    -- Margin intelligence
    markup_amount               NUMERIC(18, 4) NOT NULL,
    markup_percentage           NUMERIC(10, 4) NOT NULL,
    gross_profit                NUMERIC(18, 4) NOT NULL,
    gross_margin_percentage     NUMERIC(10, 4) NOT NULL,
    contribution_amount         NUMERIC(18, 4) NOT NULL,
    contribution_margin_pct     NUMERIC(10, 4) NOT NULL,

    -- Break-even
    break_even_price            NUMERIC(18, 4) NOT NULL,
    break_even_quantity         BIGINT         NOT NULL,
    target_margin_price         NUMERIC(18, 4),
    target_margin_percentage    NUMERIC(10, 4),

    -- Integrity
    integrity_hash              VARCHAR(128)   NOT NULL,
    created_by                  VARCHAR(128)   NOT NULL,
    created_at                  BIGINT         NOT NULL,
    is_approved                 BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_quote_versions_quote_id
    ON printing_quote_versions (tenant_id, quote_id, version_number);
CREATE INDEX IF NOT EXISTS idx_quote_versions_calc_id
    ON printing_quote_versions (tenant_id, calculation_id);

ALTER TABLE printing_quote_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE printing_quote_versions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS printing_quote_versions_tenant_isolation ON printing_quote_versions;
CREATE POLICY printing_quote_versions_tenant_isolation ON printing_quote_versions
    USING (project_id = current_setting('app.current_tenant', TRUE));

-- ============================================================
-- 3. printing_quote_cost_components  (canonical cost breakdown)
-- ============================================================
CREATE TABLE IF NOT EXISTS printing_quote_cost_components (
    component_id        VARCHAR(64)    PRIMARY KEY,
    version_id          VARCHAR(64)    NOT NULL REFERENCES printing_quote_versions(version_id),
    quote_id            VARCHAR(64)    NOT NULL,
    tenant_id           VARCHAR(64)    NOT NULL,
    project_id          VARCHAR(64)    NOT NULL,
    component_type      VARCHAR(64)    NOT NULL,
    component_code      VARCHAR(128)   NOT NULL,
    description         TEXT           NOT NULL,
    quantity            NUMERIC(18, 4) NOT NULL,
    unit                VARCHAR(32)    NOT NULL,
    unit_rate           NUMERIC(18, 4),
    amount              NUMERIC(18, 4) NOT NULL,
    formula_reference   TEXT,
    source_ref          TEXT,                          -- reference to Step 01 breakdown
    is_applicable       BOOLEAN        NOT NULL DEFAULT TRUE,
    sort_order          INT            NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cost_components_version_id
    ON printing_quote_cost_components (tenant_id, version_id);
CREATE INDEX IF NOT EXISTS idx_cost_components_quote_id
    ON printing_quote_cost_components (tenant_id, quote_id);

ALTER TABLE printing_quote_cost_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE printing_quote_cost_components FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS printing_quote_cost_components_tenant ON printing_quote_cost_components;
CREATE POLICY printing_quote_cost_components_tenant ON printing_quote_cost_components
    USING (project_id = current_setting('app.current_tenant', TRUE));

-- ============================================================
-- 4. printing_quote_pricing_components  (pricing breakdown)
-- ============================================================
CREATE TABLE IF NOT EXISTS printing_quote_pricing_components (
    pricing_component_id    VARCHAR(64)    PRIMARY KEY,
    version_id              VARCHAR(64)    NOT NULL REFERENCES printing_quote_versions(version_id),
    quote_id                VARCHAR(64)    NOT NULL,
    tenant_id               VARCHAR(64)    NOT NULL,
    project_id              VARCHAR(64)    NOT NULL,
    component_label         VARCHAR(128)   NOT NULL,
    component_type          VARCHAR(64)    NOT NULL,
    value                   NUMERIC(18, 4) NOT NULL,
    formula                 TEXT,
    sort_order              INT            NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_pricing_components_version_id
    ON printing_quote_pricing_components (tenant_id, version_id);

ALTER TABLE printing_quote_pricing_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE printing_quote_pricing_components FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS printing_quote_pricing_tenant ON printing_quote_pricing_components;
CREATE POLICY printing_quote_pricing_tenant ON printing_quote_pricing_components
    USING (project_id = current_setting('app.current_tenant', TRUE));

-- ============================================================
-- 5. printing_quote_quantity_tiers  (tiered quantity pricing)
-- ============================================================
CREATE TABLE IF NOT EXISTS printing_quote_quantity_tiers (
    tier_id             VARCHAR(64)    PRIMARY KEY,
    version_id          VARCHAR(64)    NOT NULL REFERENCES printing_quote_versions(version_id),
    quote_id            VARCHAR(64)    NOT NULL,
    tenant_id           VARCHAR(64)    NOT NULL,
    project_id          VARCHAR(64)    NOT NULL,
    tier_quantity       BIGINT         NOT NULL,
    unit_cost           NUMERIC(18, 4) NOT NULL,
    total_cost          NUMERIC(18, 4) NOT NULL,
    selling_price_unit  NUMERIC(18, 4) NOT NULL,
    final_total         NUMERIC(18, 4) NOT NULL,
    gross_margin_pct    NUMERIC(10, 4) NOT NULL,
    is_base_tier        BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_qty_tiers_version_id
    ON printing_quote_quantity_tiers (tenant_id, version_id);

ALTER TABLE printing_quote_quantity_tiers ENABLE ROW LEVEL SECURITY;
ALTER TABLE printing_quote_quantity_tiers FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS printing_quote_qty_tiers_tenant ON printing_quote_quantity_tiers;
CREATE POLICY printing_quote_qty_tiers_tenant ON printing_quote_quantity_tiers
    USING (project_id = current_setting('app.current_tenant', TRUE));

-- ============================================================
-- 6. printing_quote_audit_events  (full lifecycle audit trail)
-- ============================================================
CREATE TABLE IF NOT EXISTS printing_quote_audit_events (
    audit_id        VARCHAR(64)    PRIMARY KEY,
    quote_id        VARCHAR(64)    NOT NULL,
    version_id      VARCHAR(64),
    tenant_id       VARCHAR(64)    NOT NULL,
    project_id      VARCHAR(64)    NOT NULL,
    event_type      VARCHAR(64)    NOT NULL,
    actor           VARCHAR(128)   NOT NULL,
    description     TEXT           NOT NULL,
    before_status   VARCHAR(32),
    after_status    VARCHAR(32),
    metadata_json   TEXT,
    occurred_at     BIGINT         NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_events_quote_id
    ON printing_quote_audit_events (tenant_id, quote_id, occurred_at);

ALTER TABLE printing_quote_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE printing_quote_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS printing_quote_audit_tenant ON printing_quote_audit_events;
CREATE POLICY printing_quote_audit_tenant ON printing_quote_audit_events
    USING (project_id = current_setting('app.current_tenant', TRUE));

-- ============================================================
-- 7. printing_quote_provenance  (calculation source provenance)
-- ============================================================
CREATE TABLE IF NOT EXISTS printing_quote_provenance (
    provenance_id               VARCHAR(64)    PRIMARY KEY,
    quote_id                    VARCHAR(64)    NOT NULL REFERENCES printing_quotes(quote_id),
    version_id                  VARCHAR(64)    NOT NULL REFERENCES printing_quote_versions(version_id),
    tenant_id                   VARCHAR(64)    NOT NULL,
    project_id                  VARCHAR(64)    NOT NULL,
    calculation_id              VARCHAR(64)    NOT NULL,
    calculation_version         VARCHAR(32)    NOT NULL,
    calculation_status          VARCHAR(32)    NOT NULL,
    spec_fingerprint            VARCHAR(128)   NOT NULL,
    calc_fingerprint            VARCHAR(128)   NOT NULL,
    costing_engine_version      VARCHAR(32)    NOT NULL DEFAULT '2.0.0',
    pricing_engine_version      VARCHAR(32)    NOT NULL DEFAULT '2.0.0',
    assumptions_json            TEXT           NOT NULL,
    step01_breakdown_json       TEXT,
    captured_at                 BIGINT         NOT NULL,
    captured_by                 VARCHAR(128)   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_provenance_quote_version
    ON printing_quote_provenance (tenant_id, quote_id, version_id);

ALTER TABLE printing_quote_provenance ENABLE ROW LEVEL SECURITY;
ALTER TABLE printing_quote_provenance FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS printing_quote_provenance_tenant ON printing_quote_provenance;
CREATE POLICY printing_quote_provenance_tenant ON printing_quote_provenance
    USING (project_id = current_setting('app.current_tenant', TRUE));

-- ============================================================
-- 8. printing_quote_reconciliation_events  (math identity checks)
-- ============================================================
CREATE TABLE IF NOT EXISTS printing_quote_reconciliation_events (
    reconciliation_id       VARCHAR(64)    PRIMARY KEY,
    quote_id                VARCHAR(64)    NOT NULL REFERENCES printing_quotes(quote_id),
    version_id              VARCHAR(64)    NOT NULL REFERENCES printing_quote_versions(version_id),
    tenant_id               VARCHAR(64)    NOT NULL,
    project_id              VARCHAR(64)    NOT NULL,
    is_reconciled           BOOLEAN        NOT NULL DEFAULT FALSE,
    total_cost_check        BOOLEAN        NOT NULL DEFAULT FALSE,
    revenue_identity_check  BOOLEAN        NOT NULL DEFAULT FALSE,
    gross_profit_check      BOOLEAN        NOT NULL DEFAULT FALSE,
    margin_check            BOOLEAN        NOT NULL DEFAULT FALSE,
    markup_check            BOOLEAN        NOT NULL DEFAULT FALSE,
    breakeven_check         BOOLEAN        NOT NULL DEFAULT FALSE,
    discrepancies_json      TEXT,
    reconciled_at           BIGINT         NOT NULL,
    reconciled_by           VARCHAR(128)   NOT NULL,
    integrity_hash          VARCHAR(128)   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_quote_id
    ON printing_quote_reconciliation_events (tenant_id, quote_id, version_id);

ALTER TABLE printing_quote_reconciliation_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE printing_quote_reconciliation_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS printing_quote_reconciliation_tenant ON printing_quote_reconciliation_events;
CREATE POLICY printing_quote_reconciliation_tenant ON printing_quote_reconciliation_events
    USING (project_id = current_setting('app.current_tenant', TRUE));
