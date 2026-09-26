-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- FORM 03 — OFFER & AUDIENCE ELIGIBILITY DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE promotional_offers (
    offer_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    offer_name VARCHAR(150) NOT NULL,
    offer_code VARCHAR(50) NOT NULL,
    offer_type VARCHAR(50) NOT NULL DEFAULT 'PROMOTIONAL_DISCOUNT' CHECK (offer_type IN ('PROMOTIONAL_DISCOUNT', 'BULK_PACKAGE', 'SEASONAL_SPECIAL', 'AFFILIATE_EXCLUSIVE', 'CUSTOM_DEAL')),
    description TEXT,
    badge_text VARCHAR(100),
    terms_and_conditions TEXT,

    -- Offer Eligibility Flags (Who can REDEEM the offer)
    is_everyone_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    is_guest_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    is_customer_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    is_affiliate_eligible BOOLEAN NOT NULL DEFAULT TRUE,

    -- Offer Rules & Constraints
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    max_redemption INT NOT NULL DEFAULT 0,
    per_customer_limit INT NOT NULL DEFAULT 0,
    min_order_quantity INT NOT NULL DEFAULT 1 CHECK (min_order_quantity > 0),
    min_order_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (min_order_value >= 0.00),

    -- Offer Presentation & Form 02 Design Linkage
    visual_design_id VARCHAR(50),
    promotional_text TEXT,
    cta_text VARCHAR(100) NOT NULL DEFAULT 'অর্ডার করুন',
    display_priority INT NOT NULL DEFAULT 0,

    -- Wall Visibility Flags (Where the offer is DISPLAYED)
    public_guest_wall_visible BOOLEAN NOT NULL DEFAULT TRUE,
    customer_wall_visible BOOLEAN NOT NULL DEFAULT TRUE,
    affiliate_wall_visible BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, offer_id),
    UNIQUE (project_id, offer_code)
);

CREATE TABLE offer_applicable_products (
    offer_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, offer_id, product_id),
    FOREIGN KEY (project_id, offer_id) REFERENCES promotional_offers(project_id, offer_id) ON DELETE CASCADE
);

CREATE TABLE offer_applicable_categories (
    offer_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, offer_id, category_id),
    FOREIGN KEY (project_id, offer_id) REFERENCES promotional_offers(project_id, offer_id) ON DELETE CASCADE
);

CREATE INDEX idx_promotional_offers_status ON promotional_offers (project_id, is_active, display_priority);
CREATE INDEX idx_promotional_offers_walls ON promotional_offers (project_id, public_guest_wall_visible, customer_wall_visible, affiliate_wall_visible);
