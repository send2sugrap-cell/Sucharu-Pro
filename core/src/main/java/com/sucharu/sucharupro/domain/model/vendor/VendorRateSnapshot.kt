package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Immutable pricing snapshot captured at work order / job creation time (Module 12 Step 03 foundation for Steps 04-07).
 */
data class VendorRateSnapshot(
    val rateId: String,
    val vendorId: String,
    val capabilityType: CapabilityType,
    val pricingMethod: PricingMethod,
    val unitOfMeasure: UnitOfMeasure,
    val rateAmount: Money,
    val currency: String = "BDT",
    val effectiveDate: Long = System.currentTimeMillis(),
    val rateVersion: Long = 1L
)

fun VendorServiceRate.toSnapshot(effectiveDate: Long = System.currentTimeMillis()): VendorRateSnapshot = VendorRateSnapshot(
    rateId = rateId,
    vendorId = vendorId,
    capabilityType = capabilityType,
    pricingMethod = pricingMethod,
    unitOfMeasure = unitOfMeasure,
    rateAmount = rateAmount,
    currency = currency,
    effectiveDate = effectiveDate,
    rateVersion = version
)
