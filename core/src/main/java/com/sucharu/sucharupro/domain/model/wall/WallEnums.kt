package com.sucharu.sucharupro.domain.model.wall

enum class WallCategoryType {
    PUBLIC,
    GUEST,
    CUSTOMER,
    AFFILIATE
}

enum class SectionType {
    HERO_BANNER,
    PRODUCT_GALLERY,
    OFFER_CARD,
    PRODUCT_CARD,
    ANNOUNCEMENT,
    SERVICE_CARD,
    CTA_SECTION
}

enum class SectionLayoutType {
    GRID,
    LIST,
    CAROUSEL,
    HORIZONTAL_SCROLL
}

enum class WallPublishStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
