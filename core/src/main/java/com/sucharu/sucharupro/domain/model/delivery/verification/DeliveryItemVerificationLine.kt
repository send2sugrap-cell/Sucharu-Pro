package com.sucharu.sucharupro.domain.model.delivery.verification

/**
 * Line item for Delivery Item Verification (Module 08 Step 04).
 */
data class DeliveryItemVerificationLine(
    val verificationLineId: String,
    val verificationId: String,
    val projectId: String,
    val dispatchExecutionLineId: String,
    val challanLineId: String,
    val deliveryOrderLineId: String,
    val productId: String,
    val batchId: String? = null,
    val lotId: String? = null,
    val expectedQuantity: Double,
    val verifiedQuantity: Double,
    val issueQuantity: Double = 0.0,
    val resultType: DeliveryItemVerificationResultType = DeliveryItemVerificationResultType.VERIFIED,
    val issueType: DeliveryItemVerificationIssueType = DeliveryItemVerificationIssueType.NONE,
    val remarks: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(verificationLineId.isNotBlank()) { "Verification line ID cannot be blank." }
        require(verificationId.isNotBlank()) { "Verification ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(dispatchExecutionLineId.isNotBlank()) { "Dispatch execution line ID cannot be blank." }
        require(challanLineId.isNotBlank()) { "Challan line ID cannot be blank." }
        require(deliveryOrderLineId.isNotBlank()) { "Delivery order line ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(expectedQuantity > 0.0) { "Expected quantity must be strictly positive (> 0)." }
        require(verifiedQuantity >= 0.0) { "Verified quantity cannot be negative." }
        require(issueQuantity >= 0.0) { "Issue quantity cannot be negative." }
        require(createdAt > 0) { "Created timestamp must be positive." }
    }
}
