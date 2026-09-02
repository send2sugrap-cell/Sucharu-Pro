package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Master aggregate representing a negotiated vendor service rate and pricing contract (Module 12 Step 03).
 */
data class VendorServiceRate(
    val rateId: String,
    val projectId: String,
    val vendorId: String,
    val capabilityType: CapabilityType,
    val rateCode: String,
    val serviceName: String,
    val pricingMethod: PricingMethod = PricingMethod.PER_UNIT,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PIECE,
    val rateAmount: Money,
    val currency: String = "BDT",
    val minimumQuantity: BigDecimal = BigDecimal.ZERO,
    val maximumQuantity: BigDecimal? = null,
    val effectiveFrom: Long,
    val effectiveTo: Long? = null,
    val status: RateStatus = RateStatus.ACTIVE,
    val tiers: List<VendorServiceRateTier> = emptyList(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedBy: String = "system",
    val version: Long = 1L
)
