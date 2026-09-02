package com.sucharu.sucharupro.domain.model.vendor

import java.math.BigDecimal

/**
 * Line item in a vendor quality inspection.
 */
data class VendorQualityInspectionItem(
    val inspectionItemId: String,
    val inspectionId: String,
    val purchaseOrderItemId: String? = null,
    val deliveryReceiptItemId: String? = null,
    val itemDescription: String,
    val receivedQuantity: BigDecimal,
    val acceptedQuantity: BigDecimal = BigDecimal.ZERO,
    val rejectedQuantity: BigDecimal = BigDecimal.ZERO,
    val conditionalQuantity: BigDecimal = BigDecimal.ZERO,
    val defectCount: Int = 0,
    val defectRate: BigDecimal = BigDecimal.ZERO,
    val inspectionResult: InspectionResult = InspectionResult.ACCEPTED,
    val notes: String? = null,
    val version: Long = 1L
)
