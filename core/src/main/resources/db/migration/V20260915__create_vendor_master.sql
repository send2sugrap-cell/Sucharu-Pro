-- ============================================================
-- SUCHARU PRO — MODULE 12 STEP 01
-- Vendor Domain Foundation & Vendor Master Schema
-- Migration: V20260915__create_vendor_master.sql
-- ============================================================

-- 1. Vendor Master Table
CREATE TABLE IF NOT EXISTS vendors (
    vendor_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    vendor_code VARCHAR(64) NOT NULL,
    vendor_name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    vendor_type VARCHAR(32) NOT NULL DEFAULT 'SERVICE_PROVIDER',
    vendor_category VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    primary_contact_name VARCHAR(100),
    primary_phone VARCHAR(50),
    primary_email VARCHAR(100),
    notes TEXT,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendors PRIMARY KEY (project_id, vendor_id),
    CONSTRAINT uq_vendors_code UNIQUE (project_id, vendor_code)
);

-- 2. Indexes for High-Performance Indexed Lookups
CREATE INDEX IF NOT EXISTS idx_vendors_lookup
    ON vendors (project_id, vendor_id);

CREATE INDEX IF NOT EXISTS idx_vendors_code
    ON vendors (project_id, vendor_code);

CREATE INDEX IF NOT EXISTS idx_vendors_type_status
    ON vendors (project_id, vendor_type, status);

CREATE INDEX IF NOT EXISTS idx_vendors_name
    ON vendors (project_id, vendor_name);

-- 3. Enable Multi-Tenant Row-Level Security (RLS)
ALTER TABLE vendors ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendors FORCE ROW LEVEL SECURITY;

-- 4. Tenant Isolation Policy
CREATE POLICY vendors_tenant_isolation ON vendors
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));
