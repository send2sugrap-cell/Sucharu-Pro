package com.sucharu.sucharupro.domain.model.pricing

enum class PricingModel {
    UNIT_RATE,
    PER_100,
    PER_1000,
    BREAK_PRICE
}

enum class CommercialDiscountType {
    NONE,
    PERCENTAGE,
    FIXED_AMOUNT,
    SPECIAL_PRICE
}

enum class RoundingRule {
    NO_ROUNDING,
    NEAREST,
    UP,
    DOWN,
    HALF_UP
}
