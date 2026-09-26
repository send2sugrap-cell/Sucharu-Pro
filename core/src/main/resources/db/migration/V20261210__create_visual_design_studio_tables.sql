-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- FORM 02 — VISUAL DESIGN STUDIO DDL SPECIFICATION
-- ====================================================================================

CREATE TABLE visual_design_configurations (
    design_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    design_name VARCHAR(150) NOT NULL,
    target_type VARCHAR(50) NOT NULL DEFAULT 'PRODUCT_GALLERY_CARD' CHECK (target_type IN ('CONTENT_FOUNDATION', 'PRODUCT_GALLERY_CARD', 'GALLERY_SECTION', 'WALL_CARD', 'OFFER_CARD', 'PRODUCT_GRID', 'PRODUCT_LIST', 'PRODUCT_CAROUSEL')),
    target_id VARCHAR(50),
    version_number INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    is_active_published BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,

    -- Layout Configuration
    card_width_dp INT NOT NULL DEFAULT 320,
    card_height_dp INT NOT NULL DEFAULT 420,
    layout_type VARCHAR(30) NOT NULL DEFAULT 'GRID' CHECK (layout_type IN ('GRID', 'LIST', 'CAROUSEL')),
    content_position VARCHAR(30) NOT NULL DEFAULT 'BOTTOM' CHECK (content_position IN ('TOP', 'BOTTOM', 'OVERLAY')),
    image_position VARCHAR(30) NOT NULL DEFAULT 'TOP' CHECK (image_position IN ('TOP', 'BOTTOM', 'BACKGROUND')),
    image_ratio VARCHAR(20) NOT NULL DEFAULT 'RATIO_4_3' CHECK (image_ratio IN ('RATIO_1_1', 'RATIO_4_3', 'RATIO_16_9', 'CUSTOM')),
    alignment VARCHAR(20) NOT NULL DEFAULT 'CENTER' CHECK (alignment IN ('LEFT', 'CENTER', 'RIGHT')),
    column_count INT NOT NULL DEFAULT 2,

    -- Background, Gradient, Border & Shadow
    background_color_hex VARCHAR(10) NOT NULL DEFAULT '#1E293B',
    gradient_enable BOOLEAN NOT NULL DEFAULT FALSE,
    gradient_start_color_hex VARCHAR(10) DEFAULT '#0F172A',
    gradient_end_color_hex VARCHAR(10) DEFAULT '#1E293B',
    gradient_direction VARCHAR(20) DEFAULT 'TOP_TO_BOTTOM',
    background_opacity FLOAT NOT NULL DEFAULT 1.0,

    border_enable BOOLEAN NOT NULL DEFAULT TRUE,
    border_color_hex VARCHAR(10) NOT NULL DEFAULT '#334155',
    border_width_dp INT NOT NULL DEFAULT 1,
    border_radius_dp INT NOT NULL DEFAULT 12,

    shadow_enable BOOLEAN NOT NULL DEFAULT TRUE,
    shadow_color_hex VARCHAR(10) NOT NULL DEFAULT '#000000',
    shadow_blur_dp INT NOT NULL DEFAULT 8,
    shadow_offset_y_dp INT NOT NULL DEFAULT 4,
    shadow_opacity FLOAT NOT NULL DEFAULT 0.2,

    -- Spacing
    padding_top_dp INT NOT NULL DEFAULT 12,
    padding_bottom_dp INT NOT NULL DEFAULT 12,
    padding_left_dp INT NOT NULL DEFAULT 12,
    padding_right_dp INT NOT NULL DEFAULT 12,
    element_gap_dp INT NOT NULL DEFAULT 8,

    -- Typography
    font_family VARCHAR(50) NOT NULL DEFAULT 'SOLAIMANLIPI',
    font_size_sp INT NOT NULL DEFAULT 14,
    font_weight VARCHAR(20) NOT NULL DEFAULT 'BOLD',
    text_color_hex VARCHAR(10) NOT NULL DEFAULT '#FFFFFF',
    text_opacity FLOAT NOT NULL DEFAULT 1.0,

    -- Element Visibility Toggles
    show_image BOOLEAN NOT NULL DEFAULT TRUE,
    show_title BOOLEAN NOT NULL DEFAULT TRUE,
    show_subtitle BOOLEAN NOT NULL DEFAULT TRUE,
    show_description BOOLEAN NOT NULL DEFAULT TRUE,
    show_badge BOOLEAN NOT NULL DEFAULT TRUE,
    show_specs BOOLEAN NOT NULL DEFAULT TRUE,
    show_price BOOLEAN NOT NULL DEFAULT TRUE,
    show_cta BOOLEAN NOT NULL DEFAULT TRUE,
    show_rating BOOLEAN NOT NULL DEFAULT FALSE,
    show_favorite_button BOOLEAN NOT NULL DEFAULT FALSE,
    show_share_button BOOLEAN NOT NULL DEFAULT FALSE,

    -- CTA Styling
    cta_button_text VARCHAR(100) NOT NULL DEFAULT 'অর্ডার করুন',
    cta_button_style VARCHAR(30) NOT NULL DEFAULT 'FILLED' CHECK (cta_button_style IN ('FILLED', 'OUTLINED', 'TONAL', 'TEXT')),
    cta_button_color_hex VARCHAR(10) NOT NULL DEFAULT '#EA580C',
    cta_text_color_hex VARCHAR(10) NOT NULL DEFAULT '#FFFFFF',
    cta_border_radius_dp INT NOT NULL DEFAULT 8,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, design_id)
);

CREATE INDEX idx_visual_design_target ON visual_design_configurations (project_id, target_type, is_active_published);
CREATE INDEX idx_visual_design_status ON visual_design_configurations (project_id, status);
