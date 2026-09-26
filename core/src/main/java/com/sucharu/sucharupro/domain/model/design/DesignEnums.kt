package com.sucharu.sucharupro.domain.model.design

enum class LayoutType { GRID, LIST, CAROUSEL }
enum class ContentPosition { TOP, BOTTOM, OVERLAY }
enum class ImagePosition { TOP, BOTTOM, BACKGROUND }
enum class ImageRatio { RATIO_1_1, RATIO_4_3, RATIO_16_9, CUSTOM }
enum class Alignment { LEFT, CENTER, RIGHT }
enum class ButtonStyle { FILLED, OUTLINED, TONAL, TEXT }
enum class GradientDirection { TOP_TO_BOTTOM, LEFT_TO_RIGHT, DIAGIONAL }
enum class DesignTargetType {
    CONTENT_FOUNDATION,
    PRODUCT_GALLERY_CARD,
    GALLERY_SECTION,
    WALL_CARD,
    OFFER_CARD,
    PRODUCT_GRID,
    PRODUCT_LIST,
    PRODUCT_CAROUSEL
}
enum class DesignPublishStatus { DRAFT, PUBLISHED, ARCHIVED }
