package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Quantity tier configuration for tiered vendor service rates (Module 12 Step 03).
 */
data class VendorServiceRateTier(
    val tierId: String,
    val projectId: String,
    val rateId: String,
    val minimumQuantity: BigDecimal,
    val maximumQuantity: BigDecimal? = null,
    val rateAmount: Money,
    val version: Long = 1L
)
