package com.sucharu.sucharupro.domain.model.vendor

/**
 * Immutable append-only audit event in the lifecycle of a vendor dispute.
 */
data class VendorDisputeEvent(
    val eventId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val disputeId: String,
    val eventType: VendorDisputeEventType,
    val actorId: String,
    val notes: String? = null,
    val payloadJson: String? = null,
    val occurredAt: Long = System.currentTimeMillis()
)
