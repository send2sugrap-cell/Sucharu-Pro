package com.sucharu.sucharupro.domain.model.delivery.verification

/**
 * Derived read-side reconciliation summary for a Delivery Item Verification (Module 08 Step 04).
 */
data class DeliveryItemVerificationSummary(
    val verificationId: String,
    val projectId: String,
    val expectedTotalQuantity: Double,
    val verifiedTotalQuantity: Double,
    val shortageTotalQuantity: Double,
    val excessTotalQuantity: Double,
    val damagedTotalQuantity: Double,
    val missingTotalQuantity: Double,
    val mismatchCount: Int,
    val verifiedLineCount: Int,
    val totalLineCount: Int,
    val hasDiscrepancies: Boolean
)
