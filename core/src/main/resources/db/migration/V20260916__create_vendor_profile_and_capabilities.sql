-- =========================================================================
-- SUCHARU PRO ERP DATABASE MIGRATION
-- V20260916__create_vendor_profile_and_capabilities.sql
-- Module 12: Vendor Management — Step 02: Profile, Contacts, Addresses & Capabilities
-- =========================================================================

-- 1. Vendor Profiles Table
CREATE TABLE IF NOT EXISTS vendor_profiles (
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    legal_name VARCHAR(255),
    display_name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(150),
    primary_phone VARCHAR(50),
    alternate_phone VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(255),
    tax_id VARCHAR(100),
    business_registration_number VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_profiles PRIMARY KEY (project_id, vendor_id),
    CONSTRAINT fk_vendor_profiles_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_profiles_lookup ON vendor_profiles(project_id, vendor_id);

ALTER TABLE vendor_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_profiles FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_profiles_tenant_isolation ON vendor_profiles;
CREATE POLICY vendor_profiles_tenant_isolation ON vendor_profiles
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 2. Vendor Contacts Table
CREATE TABLE IF NOT EXISTS vendor_contacts (
    project_id VARCHAR(64) NOT NULL,
    contact_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    contact_type VARCHAR(32) NOT NULL DEFAULT 'PRIMARY',
    name VARCHAR(150) NOT NULL,
    designation VARCHAR(100),
    phone VARCHAR(50),
    alternate_phone VARCHAR(50),
    email VARCHAR(255),
    notes TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_contacts PRIMARY KEY (project_id, contact_id),
    CONSTRAINT fk_vendor_contacts_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_contacts_vendor ON vendor_contacts(project_id, vendor_id, active);

ALTER TABLE vendor_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_contacts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_contacts_tenant_isolation ON vendor_contacts;
CREATE POLICY vendor_contacts_tenant_isolation ON vendor_contacts
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 3. Vendor Addresses Table
CREATE TABLE IF NOT EXISTS vendor_addresses (
    project_id VARCHAR(64) NOT NULL,
    address_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    address_type VARCHAR(32) NOT NULL DEFAULT 'OFFICE',
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL DEFAULT 'Dhaka',
    district VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) NOT NULL DEFAULT 'Bangladesh',
    notes TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_addresses PRIMARY KEY (project_id, address_id),
    CONSTRAINT fk_vendor_addresses_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_addresses_vendor ON vendor_addresses(project_id, vendor_id, active);

ALTER TABLE vendor_addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_addresses FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_addresses_tenant_isolation ON vendor_addresses;
CREATE POLICY vendor_addresses_tenant_isolation ON vendor_addresses
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));

-- 4. Vendor Capabilities Table
CREATE TABLE IF NOT EXISTS vendor_capabilities (
    project_id VARCHAR(64) NOT NULL,
    capability_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    capability_type VARCHAR(64) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_vendor_capabilities PRIMARY KEY (project_id, capability_id),
    CONSTRAINT uq_vendor_capability_type UNIQUE (project_id, vendor_id, capability_type),
    CONSTRAINT fk_vendor_capabilities_vendor FOREIGN KEY (project_id, vendor_id)
        REFERENCES vendors(project_id, vendor_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vendor_capabilities_lookup ON vendor_capabilities(project_id, vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vendor_capabilities_type ON vendor_capabilities(project_id, capability_type, status);

ALTER TABLE vendor_capabilities ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_capabilities FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS vendor_capabilities_tenant_isolation ON vendor_capabilities;
CREATE POLICY vendor_capabilities_tenant_isolation ON vendor_capabilities
    AS RESTRICTIVE
    USING (project_id = current_setting('app.current_project_id', true));
