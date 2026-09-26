package com.sucharu.sucharupro.domain.model.design

/**
 * Form 02 — Visual Design Studio Canonical Domain Entity.
 *
 * Persists complete visual presentation configurations for Gallery Cards, Hero Banners,
 * Offer Cards, and Product Cards without duplicating business truth or Product Master data.
 */
data class VisualDesignConfiguration(
    val designId: String,
    val designName: String,
    val targetType: DesignTargetType = DesignTargetType.PRODUCT_GALLERY_CARD,
    val targetId: String? = null,
    val versionNumber: Int = 1,
    val status: DesignPublishStatus = DesignPublishStatus.DRAFT,
    val isActivePublished: Boolean = false,
    val displayOrder: Int = 0,

    // Layout
    val cardWidthDp: Int = 320,
    val cardHeightDp: Int = 420,
    val layoutType: LayoutType = LayoutType.GRID,
    val contentPosition: ContentPosition = ContentPosition.BOTTOM,
    val imagePosition: ImagePosition = ImagePosition.TOP,
    val imageRatio: ImageRatio = ImageRatio.RATIO_4_3,
    val alignment: Alignment = Alignment.CENTER,
    val columnCount: Int = 2,

    // Background, Gradient, Border & Shadow
    val backgroundColorHex: String = "#1E293B",
    val gradientEnable: Boolean = false,
    val gradientStartColorHex: String? = "#0F172A",
    val gradientEndColorHex: String? = "#1E293B",
    val gradientDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
    val backgroundOpacity: Float = 1.0f,

    val borderEnable: Boolean = true,
    val borderColorHex: String = "#334155",
    val borderWidthDp: Int = 1,
    val borderRadiusDp: Int = 12,

    val shadowEnable: Boolean = true,
    val shadowColorHex: String = "#000000",
    val shadowBlurDp: Int = 8,
    val shadowOffsetYDp: Int = 4,
    val shadowOpacity: Float = 0.2f,

    // Spacing
    val paddingTopDp: Int = 12,
    val paddingBottomDp: Int = 12,
    val paddingLeftDp: Int = 12,
    val paddingRightDp: Int = 12,
    val elementGapDp: Int = 8,

    // Typography
    val fontFamily: String = "SOLAIMANLIPI",
    val fontSizeSp: Int = 14,
    val fontWeight: String = "BOLD",
    val textColorHex: String = "#FFFFFF",
    val textOpacity: Float = 1.0f,

    // Element Visibility Toggles
    val showImage: Boolean = true,
    val showTitle: Boolean = true,
    val showSubtitle: Boolean = true,
    val showDescription: Boolean = true,
    val showBadge: Boolean = true,
    val showSpecs: Boolean = true,
    val showPrice: Boolean = true,
    val showCta: Boolean = true,
    val showRating: Boolean = false,
    val showFavoriteButton: Boolean = false,
    val showShareButton: Boolean = false,

    // CTA Styling
    val ctaButtonText: String = "অর্ডার করুন",
    val ctaButtonStyle: ButtonStyle = ButtonStyle.FILLED,
    val ctaButtonColorHex: String = "#EA580C",
    val ctaTextColorHex: String = "#FFFFFF",
    val ctaBorderRadiusDp: Int = 8,

    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(designId.isNotBlank()) { "Design ID cannot be blank." }
        require(designName.isNotBlank()) { "Design Name cannot be blank." }
        require(cardWidthDp > 0) { "Card Width must be greater than zero." }
        require(cardHeightDp > 0) { "Card Height must be greater than zero." }
        require(borderRadiusDp >= 0) { "Border Radius cannot be negative." }
        require(borderWidthDp >= 0) { "Border Width cannot be negative." }
        require(paddingTopDp >= 0 && paddingBottomDp >= 0 && paddingLeftDp >= 0 && paddingRightDp >= 0) { "Padding values cannot be negative." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
    }
}
