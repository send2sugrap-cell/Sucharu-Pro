-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- FORM 01 — CONTENT & PRODUCT FOUNDATION DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE content_foundations (
    content_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    content_type VARCHAR(50) NOT NULL DEFAULT 'FINISHED_PRODUCT' CHECK (content_type IN ('FINISHED_PRODUCT', 'PRINTING_JOB', 'GIFT_PROMOTIONAL', 'SERVICE', 'CUSTOM_JOB')),
    product_id VARCHAR(50),
    product_name VARCHAR(150) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    category_id VARCHAR(50),
    category_name VARCHAR(100) NOT NULL,
    sub_category_name VARCHAR(100),
    template_code VARCHAR(50) NOT NULL,
    internal_reference VARCHAR(100),

    -- Content Text Fields
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(300),
    description TEXT,
    short_description TEXT,
    badge_text VARCHAR(100),
    specification_list JSONB NOT NULL DEFAULT '[]'::jsonb,
    features_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    search_keywords_json JSONB NOT NULL DEFAULT '[]'::jsonb,

    -- Product Information & Availability
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    minimum_quantity INT NOT NULL DEFAULT 1 CHECK (minimum_quantity > 0),
    available_quantity INT NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Publication Lifecycle
    publication_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (publication_status IN ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'UNPUBLISHED')),
    scheduled_start_at TIMESTAMPTZ,
    scheduled_end_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, content_id),
    UNIQUE (project_id, product_code)
);

CREATE TABLE content_media_references (
    media_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    content_id VARCHAR(50) NOT NULL,
    media_type VARCHAR(30) NOT NULL CHECK (media_type IN ('MAIN_IMAGE', 'GALLERY_IMAGE', 'THUMBNAIL', 'HERO_IMAGE', 'MOBILE_IMAGE', 'DESKTOP_IMAGE', 'VIDEO')),
    media_uri VARCHAR(500) NOT NULL,
    alt_text VARCHAR(200),
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, media_id),
    FOREIGN KEY (project_id, content_id) REFERENCES content_foundations(project_id, content_id) ON DELETE CASCADE
);

CREATE INDEX idx_content_foundations_pub_status ON content_foundations (project_id, publication_status, is_active);
CREATE INDEX idx_content_foundations_cat ON content_foundations (project_id, category_name);
CREATE INDEX idx_content_foundations_product ON content_foundations (project_id, product_id);
