package com.sucharu.sucharupro.domain.model.delivery.verification

/**
 * Immutable audit event for delivery verification lifecycle and line actions (Module 08 Step 04).
 */
data class DeliveryItemVerificationActivityEvent(
    val activityId: String,
    val projectId: String,
    val verificationId: String,
    val activityType: DeliveryItemVerificationActivityType,
    val performedBy: String,
    val performedAt: Long,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val lineId: String? = null,
    val details: String? = null
) {
    init {
        require(activityId.isNotBlank()) { "Activity ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(verificationId.isNotBlank()) { "Verification ID cannot be blank." }
        require(performedBy.isNotBlank()) { "Performed by user ID cannot be blank." }
        require(performedAt > 0) { "Performed timestamp must be positive." }
    }
}
