package com.sucharu.sucharupro.domain.model.delivery.verification

/**
 * Aggregate Root representing a Delivery Item Verification (Module 08 Step 04).
 */
data class DeliveryItemVerification(
    val verificationId: String,
    val projectId: String,
    val verificationNo: String,
    val deliveryOrderId: String,
    val deliveryChallanId: String,
    val dispatchExecutionId: String,
    val status: DeliveryItemVerificationStatus = DeliveryItemVerificationStatus.DRAFT,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val remarks: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(verificationId.isNotBlank()) { "Verification ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(verificationNo.isNotBlank()) { "Verification Number cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(deliveryChallanId.isNotBlank()) { "Delivery Challan ID cannot be blank." }
        require(dispatchExecutionId.isNotBlank()) { "Dispatch Execution ID cannot be blank." }
        require(createdBy.isNotBlank()) { "Created by user ID cannot be blank." }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp must be greater than or equal to created timestamp." }
    }
}
