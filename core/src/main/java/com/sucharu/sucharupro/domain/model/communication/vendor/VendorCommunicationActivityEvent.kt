package com.sucharu.sucharupro.domain.model.communication.vendor

import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

/**
 * Immutable audit event record for vendor communication operations (Module 10 Step 05).
 *
 * All audit records are append-only, project-scoped, and chronological.
 * Never rewrite, update, or delete historical audit events.
 */
data class VendorCommunicationActivityEvent(
    val eventId: String,
    val projectId: String,
    val communicationId: String,
    val vendorId: String,
    val eventType: VendorCommunicationActivityEventType,
    val actorId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}

/**
 * Enumeration of all auditable vendor communication events (Module 10 Step 05).
 */
enum class VendorCommunicationActivityEventType(val defaultLabel: String) {
    COMMUNICATION_CREATED("Communication Created"),
    COMMUNICATION_UPDATED("Communication Updated"),
    COMMUNICATION_SCHEDULED("Communication Scheduled"),
    COMMUNICATION_QUEUED("Communication Queued"),
    COMMUNICATION_SENT("Communication Sent"),
    COMMUNICATION_DELIVERED("Communication Delivered"),
    COMMUNICATION_READ("Communication Read"),
    COMMUNICATION_ACKNOWLEDGED("Communication Acknowledged"),
    COMMUNICATION_DECLINED("Communication Declined"),
    COMMUNICATION_FAILED("Communication Failed"),
    COMMUNICATION_CANCELLED("Communication Cancelled"),
    COMMUNICATION_RETRY("Communication Retry Requested")
}

/**
 * Summary of communications for a project or vendor scope (Module 10 Step 05).
 */
data class VendorCommunicationSummary(
    val projectId: String,
    val vendorId: String? = null,
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val readCount: Int = 0,
    val acknowledgedCount: Int = 0,
    val declinedCount: Int = 0,
    val scheduledCount: Int = 0,
    val sentCount: Int = 0,
    val deliveredCount: Int = 0,
    val failedCount: Int = 0,
    val pendingAcknowledgementCount: Int = 0,
    val countsByType: Map<VendorCommunicationType, Int> = emptyMap(),
    val countsByPriority: Map<NotificationPriority, Int> = emptyMap()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
