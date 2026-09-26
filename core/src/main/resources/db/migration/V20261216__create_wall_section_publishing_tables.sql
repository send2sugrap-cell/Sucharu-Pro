-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- FORM 06 — WALL / SECTION / PUBLISHING CONTROL DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE wall_configurations (
    wall_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    wall_type VARCHAR(30) NOT NULL DEFAULT 'PUBLIC' CHECK (wall_type IN ('PUBLIC', 'GUEST', 'CUSTOMER', 'AFFILIATE')),
    wall_title VARCHAR(150) NOT NULL,
    version_number INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    is_active_published BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audience Visibility Flags
    public_visibility BOOLEAN NOT NULL DEFAULT TRUE,
    guest_visibility BOOLEAN NOT NULL DEFAULT TRUE,
    customer_visibility BOOLEAN NOT NULL DEFAULT TRUE,
    affiliate_visibility BOOLEAN NOT NULL DEFAULT TRUE,

    -- Scheduling
    scheduled_start_at TIMESTAMPTZ,
    scheduled_end_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, wall_id)
);

CREATE TABLE wall_sections (
    section_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    wall_id VARCHAR(50) NOT NULL,
    section_type VARCHAR(50) NOT NULL CHECK (section_type IN ('HERO_BANNER', 'PRODUCT_GALLERY', 'OFFER_CARD', 'PRODUCT_CARD', 'ANNOUNCEMENT', 'SERVICE_CARD', 'CTA_SECTION')),
    section_name VARCHAR(150) NOT NULL,
    section_title VARCHAR(200),
    section_subtitle VARCHAR(300),
    layout_type VARCHAR(30) NOT NULL DEFAULT 'GRID' CHECK (layout_type IN ('GRID', 'LIST', 'CAROUSEL', 'HORIZONTAL_SCROLL')),
    visual_design_id VARCHAR(50),
    display_order INT NOT NULL DEFAULT 0,
    priority_level INT NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, section_id),
    FOREIGN KEY (project_id, wall_id) REFERENCES wall_configurations(project_id, wall_id) ON DELETE CASCADE
);

CREATE TABLE wall_section_items (
    item_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    section_id VARCHAR(50) NOT NULL,
    content_foundation_id VARCHAR(50),
    product_id VARCHAR(50),
    offer_id VARCHAR(50),
    visual_design_id VARCHAR(50),
    display_order INT NOT NULL DEFAULT 0,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, item_id),
    FOREIGN KEY (project_id, section_id) REFERENCES wall_sections(project_id, section_id) ON DELETE CASCADE
);

CREATE TABLE wall_publishing_versions (
    version_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    wall_id VARCHAR(50) NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    configuration_snapshot_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    PRIMARY KEY (project_id, version_id),
    UNIQUE (project_id, wall_id, version_number)
);

CREATE INDEX idx_wall_configs_type ON wall_configurations (project_id, wall_type, is_active_published);
CREATE INDEX idx_wall_sections_order ON wall_sections (project_id, wall_id, display_order ASC);
