package com.sucharu.sucharupro.domain.model.communication.automation

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel

/**
 * BI-10 Delivery Status Lifecycle.
 */
enum class CommunicationDeliveryStatus {
    QUEUED,
    PROCESSING,
    SENT,
    DELIVERED,
    FAILED,
    RETRYING,
    CANCELLED
}

/**
 * Auditable Communication Dispatch Attempt Model.
 */
data class CommunicationDispatchAttempt(
    val dispatchId: String,
    val eventId: String,
    val ruleId: String? = null,
    val recipientId: String,
    val recipientRole: String = "CUSTOMER",
    val channel: NotificationChannel = NotificationChannel.IN_APP,
    val title: String,
    val renderedMessage: String,
    val deliveryStatus: CommunicationDeliveryStatus = CommunicationDeliveryStatus.QUEUED,
    val attemptCount: Int = 1,
    val maxAttempts: Int = 3,
    val idempotencyKey: String,
    val failureReason: String? = null,
    val dispatchedAt: String,
    val deliveredAt: String? = null
) {
    init {
        require(dispatchId.isNotBlank()) { "Dispatch ID cannot be blank." }
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(recipientId.isNotBlank()) { "Recipient ID cannot be blank." }
        require(idempotencyKey.isNotBlank()) { "Idempotency key cannot be blank." }
    }
}

/**
 * BI-10 Master Communication Automation Summary.
 */
data class CommunicationAutomationDispatchSummary(
    val totalEventsProcessedCount: Int,
    val queuedCount: Int,
    val sentCount: Int,
    val deliveredCount: Int,
    val failedCount: Int,
    val retriedCount: Int,
    val isOutboxDecoupledDeliveryEnforced: Boolean = true, // Critical Invariant: Always true!
    val activeDispatches: List<CommunicationDispatchAttempt> = emptyList(),
    val generatedAt: String
)
