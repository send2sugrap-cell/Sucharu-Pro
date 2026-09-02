package com.sucharu.sucharupro.domain.model.communication.vendor

import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

/**
 * Core aggregate root representing a Vendor & Supplier Communication (Module 10 Step 05).
 *
 * References the canonical [com.sucharu.sucharupro.domain.model.notification.Notification]
 * created by Module 10 Step 01 for delivery.
 *
 * Security: No financial values, passwords, tokens, or sensitive credentials may be stored.
 */
data class VendorCommunication(
    val communicationId: String,
    val communicationNo: String,
    val projectId: String,
    val vendorId: String,
    val supplierReferenceId: String? = null,
    val communicationType: VendorCommunicationType,
    val status: VendorCommunicationStatus = VendorCommunicationStatus.DRAFT,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val subject: String,
    val message: String,
    val notificationId: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdBy: String,
    val requiresAcknowledgement: Boolean = false,
    val scheduledAt: Long? = null,
    val sentAt: Long? = null,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancelledBy: String? = null,
    val cancellationReason: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(communicationNo.isNotBlank()) { "Communication Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(subject.isNotBlank()) { "Subject cannot be blank." }
        require(message.isNotBlank()) { "Message cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By identifier cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }

        // Security: Sanitize metadata keys — never allow credential-like keys
        val forbiddenKeys = listOf(
            "password", "token", "secret", "cvv", "card_number", "pin", "api_key",
            "auth", "credential", "private_key", "bearer"
        )
        for (key in metadata.keys) {
            val lower = key.lowercase()
            require(forbiddenKeys.none { lower.contains(it) }) {
                "Sensitive key '$key' is prohibited in vendor communication metadata."
            }
        }
    }

    // Computed state helpers
    val isRead: Boolean
        get() = status == VendorCommunicationStatus.READ ||
                status == VendorCommunicationStatus.ACKNOWLEDGED ||
                status == VendorCommunicationStatus.DECLINED ||
                readAt != null

    val isAcknowledged: Boolean
        get() = status == VendorCommunicationStatus.ACKNOWLEDGED || acknowledgedAt != null

    val isDeclined: Boolean
        get() = status == VendorCommunicationStatus.DECLINED

    val isDelivered: Boolean
        get() = status == VendorCommunicationStatus.DELIVERED || isRead || deliveredAt != null

    val isCancelled: Boolean
        get() = status == VendorCommunicationStatus.CANCELLED

    val isTerminal: Boolean
        get() = status.isTerminal
}
