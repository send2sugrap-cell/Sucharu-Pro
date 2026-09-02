package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Operational delivery attempt record for a Delivery Shipment (Module 08 Step 05).
 *
 * @param attemptId Unique identifier for this delivery attempt.
 * @param projectId Project boundary context.
 * @param shipmentId Associated shipment identifier.
 * @param attemptNo Sequential attempt number (1, 2, 3...).
 * @param attemptedAt Timestamp when attempt took place (epoch millis).
 * @param status Outcome status of the attempt.
 * @param reason Reason for unsuccessful outcome if applicable.
 * @param notes Operational notes.
 * @param createdBy User ID recording the attempt.
 * @param createdAt Creation timestamp (epoch millis).
 */
data class DeliveryShipmentAttempt(
    val attemptId: String,
    val projectId: String,
    val shipmentId: String,
    val attemptNo: Int,
    val attemptedAt: Long = System.currentTimeMillis(),
    val status: DeliveryShipmentAttemptStatus,
    val reason: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(attemptId.isNotBlank()) { "Attempt ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(shipmentId.isNotBlank()) { "Shipment ID cannot be blank." }
        require(attemptNo > 0) { "Attempt number must be positive (>= 1)." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(attemptedAt > 0) { "Attempted At timestamp must be positive." }
        require(createdAt > 0) { "Created At timestamp must be positive." }
    }
}
