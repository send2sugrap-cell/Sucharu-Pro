package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Line item on a Vendor Purchase Order (Module 12 Step 05).
 */
data class VendorPurchaseOrderItem(
    val itemId: String,
    val purchaseOrderId: String,
    val vendorServiceRateId: String? = null,
    val capabilityType: CapabilityType? = null,
    val itemDescription: String,
    val itemCode: String? = null,
    val quantity: BigDecimal,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PIECE,
    val unitRate: Money,
    val pricingMethod: PricingMethod = PricingMethod.PER_UNIT,
    val currency: String = "BDT",
    val discount: Money = Money.ZERO,
    val taxAmount: Money = Money.ZERO,
    val lineTotal: Money,
    val expectedDeliveryDate: Long? = null,
    val notes: String? = null,
    val sourceWorkOrderId: String? = null,
    val version: Long = 1L
)
