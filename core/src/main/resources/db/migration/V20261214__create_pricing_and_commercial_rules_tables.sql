-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- FORM 04 — PRICING & COMMERCIAL RULES DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE product_price_configurations (
    price_config_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    product_id VARCHAR(50) NOT NULL,
    price_version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',

    -- Base Pricing
    base_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (base_price >= 0.00),
    base_quantity INT NOT NULL DEFAULT 1000 CHECK (base_quantity > 0),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    min_order_quantity INT NOT NULL DEFAULT 1000 CHECK (min_order_quantity > 0),
    max_order_quantity INT NOT NULL DEFAULT 1000000 CHECK (max_order_quantity >= min_order_quantity),

    -- Pricing Model & Discounts
    pricing_model VARCHAR(30) NOT NULL DEFAULT 'BREAK_PRICE' CHECK (pricing_model IN ('UNIT_RATE', 'PER_100', 'PER_1000', 'BREAK_PRICE')),
    discount_type VARCHAR(30) NOT NULL DEFAULT 'NONE' CHECK (discount_type IN ('NONE', 'PERCENTAGE', 'FIXED_AMOUNT', 'SPECIAL_PRICE')),
    discount_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (discount_value >= 0.00),

    -- Additional Commercial Charges & Taxes
    surcharge_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (surcharge_amount >= 0.00),
    inside_dhaka_delivery_charge NUMERIC(15, 2) NOT NULL DEFAULT 60.00 CHECK (inside_dhaka_delivery_charge >= 0.00),
    outside_dhaka_delivery_charge NUMERIC(15, 2) NOT NULL DEFAULT 120.00 CHECK (outside_dhaka_delivery_charge >= 0.00),
    tax_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00 CHECK (tax_percentage >= 0.00),
    rounding_rule VARCHAR(30) NOT NULL DEFAULT 'NEAREST' CHECK (rounding_rule IN ('NO_ROUNDING', 'NEAREST', 'UP', 'DOWN', 'HALF_UP')),

    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, price_config_id),
    UNIQUE (project_id, product_id, price_version)
);

CREATE TABLE product_price_quantity_tiers (
    tier_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    price_config_id VARCHAR(50) NOT NULL,
    quantity_break INT NOT NULL CHECK (quantity_break > 0),
    tier_price NUMERIC(15, 2) NOT NULL CHECK (tier_price >= 0.00),
    unit_rate NUMERIC(15, 4) NOT NULL CHECK (unit_rate >= 0.0000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, tier_id),
    FOREIGN KEY (project_id, price_config_id) REFERENCES product_price_configurations(project_id, price_config_id) ON DELETE CASCADE
);

CREATE TABLE order_price_snapshots (
    snapshot_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    order_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    price_config_id VARCHAR(50) NOT NULL,
    price_version INT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',

    order_quantity INT NOT NULL CHECK (order_quantity > 0),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    base_price_snapshot NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    matched_tier_quantity_break INT,
    matched_tier_price NUMERIC(15, 2),

    subtotal_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    surcharge_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    delivery_charge NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    rounding_adjustment NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    grand_total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,

    applied_offer_id VARCHAR(50),
    applied_offer_code VARCHAR(50),
    applied_offer_discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,

    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, snapshot_id),
    UNIQUE (project_id, order_id)
);

CREATE TABLE applied_offer_snapshots (
    applied_offer_snapshot_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    snapshot_id VARCHAR(50) NOT NULL,
    offer_id VARCHAR(50) NOT NULL,
    offer_code VARCHAR(50) NOT NULL,
    offer_name VARCHAR(150) NOT NULL,
    applied_discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    eligibility_audience VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, applied_offer_snapshot_id),
    FOREIGN KEY (project_id, snapshot_id) REFERENCES order_price_snapshots(project_id, snapshot_id) ON DELETE CASCADE
);

CREATE INDEX idx_price_configs_prod ON product_price_configurations (project_id, product_id, is_active);
CREATE INDEX idx_order_snapshots_order ON order_price_snapshots (project_id, order_id);
