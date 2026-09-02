package com.sucharu.sucharupro.domain.model.communication.customer

/**
 * Customer interaction, read, acknowledgment, and engagement telemetry events (Module 10 Step 02).
 */
data class CustomerEngagementEvent(
    val eventId: String,
    val projectId: String,
    val customerId: String,
    val communicationId: String,
    val eventType: CustomerEngagementEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}

enum class CustomerEngagementEventType(val defaultLabel: String) {
    COMMUNICATION_VIEWED("Communication Viewed"),
    COMMUNICATION_READ("Communication Read"),
    COMMUNICATION_ACKNOWLEDGED("Communication Acknowledged"),
    OFFER_VIEWED("Offer Viewed"),
    ANNOUNCEMENT_VIEWED("Announcement Viewed"),
    ORDER_UPDATE_VIEWED("Order Update Viewed"),
    DELIVERY_UPDATE_VIEWED("Delivery Update Viewed")
}

/**
 * Aggregated engagement metrics for a customer or project (Module 10 Step 02).
 */
data class CustomerEngagementSummary(
    val projectId: String,
    val customerId: String? = null,
    val messagesSent: Int = 0,
    val messagesDelivered: Int = 0,
    val messagesRead: Int = 0,
    val messagesAcknowledged: Int = 0,
    val readRatePercent: Double = 0.0,
    val acknowledgementRatePercent: Double = 0.0,
    val offerViews: Int = 0,
    val announcementViews: Int = 0,
    val lastEngagementAt: Long? = null
)
