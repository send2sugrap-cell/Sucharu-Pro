package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Immutable pricing snapshot captured on a Work Order (Module 12 Step 04).
 * Prevents downstream calculation drift even if the active rate card is modified.
 */
data class VendorWorkOrderRateSnapshot(
    val sourceRateId: String? = null,
    val pricingMethod: PricingMethod = PricingMethod.PER_UNIT,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PIECE,
    val currency: String = "BDT",
    val baseRate: Money = Money.ZERO,
    val resolvedUnitRate: Money = Money.ZERO,
    val tierMetadata: String? = null,
    val quantityBasis: BigDecimal = BigDecimal.ONE,
    val resolvedAt: Long = System.currentTimeMillis()
)
