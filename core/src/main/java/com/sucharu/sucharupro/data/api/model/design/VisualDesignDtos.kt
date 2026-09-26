package com.sucharu.sucharupro.data.api.model.design

import kotlinx.serialization.Serializable

@Serializable
data class VisualDesignConfigurationDto(
    val designId: String,
    val designName: String,
    val targetType: String = "PRODUCT_GALLERY_CARD",
    val targetId: String? = null,
    val versionNumber: Int = 1,
    val status: String = "DRAFT",
    val isActivePublished: Boolean = false,
    val displayOrder: Int = 0,

    val cardWidthDp: Int = 320,
    val cardHeightDp: Int = 420,
    val imageAreaDp: Int = 160,
    val contentAreaPaddingDp: Int = 12,
    val sectionHeightDp: Int = 480,
    val layoutType: String = "GRID",
    val contentPosition: String = "BOTTOM",
    val imagePosition: String = "TOP",
    val imageRatio: String = "RATIO_4_3",
    val alignment: String = "CENTER",
    val columnCount: Int = 2,

    val backgroundColorHex: String = "#1E293B",
    val backgroundImageUri: String? = null,
    val gradientEnable: Boolean = false,
    val gradientStartColorHex: String? = "#0F172A",
    val gradientEndColorHex: String? = "#1E293B",
    val gradientDirection: String = "TOP_TO_BOTTOM",
    val backgroundOpacity: Float = 1.0f,

    val borderEnable: Boolean = true,
    val borderColorHex: String = "#334155",
    val borderWidthDp: Int = 1,
    val borderRadiusDp: Int = 12,

    val shadowEnable: Boolean = true,
    val shadowColorHex: String = "#000000",
    val shadowBlurDp: Int = 8,
    val shadowSpreadDp: Int = 2,
    val shadowOffsetYDp: Int = 4,
    val shadowOpacity: Float = 0.2f,

    val paddingTopDp: Int = 12,
    val paddingBottomDp: Int = 12,
    val paddingLeftDp: Int = 12,
    val paddingRightDp: Int = 12,
    val marginTopDp: Int = 8,
    val marginBottomDp: Int = 8,
    val elementGapDp: Int = 8,

    val fontFamily: String = "SOLAIMANLIPI",
    val fontSizeSp: Int = 14,
    val fontWeight: String = "BOLD",
    val fontStyle: String = "NORMAL",
    val letterSpacingSp: Float = 0.0f,
    val lineHeightSp: Int = 20,
    val textColorHex: String = "#FFFFFF",
    val textOpacity: Float = 1.0f,

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

    val ctaButtonText: String = "অর্ডার করুন",
    val ctaButtonStyle: String = "FILLED",
    val ctaButtonColorHex: String = "#EA580C",
    val ctaTextColorHex: String = "#FFFFFF",
    val ctaBorderRadiusDp: Int = 8,
    val ctaButtonSizeDp: Int = 44,
    val ctaActionType: String = "CHECKOUT",
    val ctaIconName: String = "SHOPPING_BAG",

    val createdAt: String = "",
    val updatedAt: String = "",
    val createdBy: String = ""
)

@Serializable
data class CreateVisualDesignRequestDto(
    val designName: String,
    val targetType: String = "PRODUCT_GALLERY_CARD",
    val targetId: String? = null,
    val cardWidthDp: Int = 320,
    val cardHeightDp: Int = 420,
    val layoutType: String = "GRID",
    val backgroundColorHex: String = "#1E293B",
    val borderColorHex: String = "#334155",
    val borderRadiusDp: Int = 12,
    val fontFamily: String = "SOLAIMANLIPI",
    val textColorHex: String = "#FFFFFF",
    val ctaButtonText: String = "অর্ডার করুন",
    val ctaButtonColorHex: String = "#EA580C"
)
