package com.sucharu.sucharupro.domain.model.vendor

/**
 * Append-only audit record capturing significant lifecycle events on a VendorWorkOrder (Module 12 Step 04).
 */
data class VendorWorkOrderAuditEvent(
    val auditId: String,
    val projectId: String,
    val workOrderId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val details: String? = null
)
