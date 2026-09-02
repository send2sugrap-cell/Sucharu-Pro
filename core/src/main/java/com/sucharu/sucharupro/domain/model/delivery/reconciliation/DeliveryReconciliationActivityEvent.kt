package com.sucharu.sucharupro.domain.model.delivery.reconciliation

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Immutable append-only audit event for Delivery Reconciliation lifecycle (Module 08 Step 09).
 */
data class DeliveryReconciliationActivityEvent(
    val eventId: String,
    val reconciliationId: String,
    val projectId: String,
    val activityType: DeliveryReconciliationActivityType,
    val actorId: String,
    val actorRole: UserRole? = null,
    val previousStatus: DeliveryReconciliationStatus? = null,
    val newStatus: DeliveryReconciliationStatus? = null,
    val notes: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(reconciliationId.isNotBlank()) { "Reconciliation ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
