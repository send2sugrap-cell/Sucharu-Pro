-- =========================================================================
-- SUCHARU PRO ERP DATABASE MIGRATION
-- V20260917__create_vendor_service_rates.sql
-- Module 12: Vendor Management — Step 03: Service Rate & Pricing Management
-- =========================================================================

-- 1. Vendor Service Rates Table
CREATE TABLE IF NOT EXISTS vendor_service_rates (
    project_id VARCHAR(64) NOT NULL,
    rate_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    capability_type VARCHAR(64) NOT NULL,
    rate_code VARCHAR(64) NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    pricing_method VARCHAR(32) NOT NULL DEFAULT 'PER_UNIT',
    unit_of_measure VARCHAR(32) NOT NULL DEFAULT 'PIECE',
    rate_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    minimum_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    maximum_quantity NUMERIC(14, 2),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_service_rates PRIMARY KEY (project_id, rate_id),
    CONSTRAINT uq_vendor_service_rate_code UNIQUE (project_id, rate_code),
    CONSTRAINT fk_vendor_service_rates_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_service_rates_lookup ON vendor_service_rates(project_id, vendor_id, capability_type, status);
CREATE INDEX IF NOT EXISTS idx_vendor_service_rates_dates ON vendor_service_rates(project_id, effective_from, effective_to);

ALTER TABLE vendor_service_rates ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_service_rates FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_service_rates_tenant_isolation ON vendor_service_rates;
CREATE POLICY vendor_service_rates_tenant_isolation ON vendor_service_rates
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 2. Vendor Service Rate Tiers Table
CREATE TABLE IF NOT EXISTS vendor_service_rate_tiers (
    project_id VARCHAR(64) NOT NULL,
    tier_id VARCHAR(64) NOT NULL,
    rate_id VARCHAR(64) NOT NULL,
    minimum_quantity NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    maximum_quantity NUMERIC(14, 2),
    rate_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_service_rate_tiers PRIMARY KEY (project_id, tier_id),
    CONSTRAINT fk_vendor_service_rate_tiers_rate FOREIGN KEY (project_id, rate_id)
        REFERENCES vendor_service_rates(project_id, rate_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_service_rate_tiers_rate ON vendor_service_rate_tiers(project_id, rate_id);

ALTER TABLE vendor_service_rate_tiers ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_service_rate_tiers FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_service_rate_tiers_tenant_isolation ON vendor_service_rate_tiers;
CREATE POLICY vendor_service_rate_tiers_tenant_isolation ON vendor_service_rate_tiers
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));
