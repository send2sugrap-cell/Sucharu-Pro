package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Master aggregate representing an operational commitment to an external vendor for a service/job (Module 12 Step 04).
 */
data class VendorWorkOrder(
    val workOrderId: String,
    val projectId: String,
    val workOrderNumber: String,
    val vendorId: String,
    val capabilityType: CapabilityType,
    val serviceRateId: String? = null,
    val sourceReferenceId: String? = null,
    val sourceReferenceType: String? = null,
    val title: String,
    val description: String? = null,
    val quantity: BigDecimal,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PIECE,
    val pricingMethod: PricingMethod = PricingMethod.PER_UNIT,
    val rateSnapshot: VendorWorkOrderRateSnapshot = VendorWorkOrderRateSnapshot(),
    val currency: String = "BDT",
    val estimatedAmount: Money,
    val scheduledStartAt: Long? = null,
    val scheduledDueAt: Long? = null,
    val priority: String = "NORMAL",
    val status: VendorWorkOrderStatus = VendorWorkOrderStatus.DRAFT,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)
