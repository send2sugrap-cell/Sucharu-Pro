package com.sucharu.sucharupro.domain.model.delivery.governance

/**
 * Immutable audit trail event for delivery governance actions.
 */
data class DeliveryGovernanceActivityEvent(
    val eventId: String,
    val alertId: String,
    val projectId: String,
    val activityType: DeliveryGovernanceActivityType,
    val actorId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(alertId.isNotBlank()) { "Alert ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(details.isNotBlank()) { "Details cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
