package com.sucharu.sucharupro.domain.model.vendor

/**
 * Pricing methodology used to compute service/processing charges from an external vendor (Module 12 Step 03).
 */
enum class PricingMethod {
    FIXED,
    PER_UNIT,
    PER_QUANTITY,
    PER_AREA,
    PER_WEIGHT,
    PER_TIME,
    TIERED
}
