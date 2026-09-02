package com.sucharu.sucharupro.domain.model.vendor

/**
 * Append-only audit record capturing significant lifecycle events on a VendorPurchaseOrder (Module 12 Step 05).
 */
data class VendorPurchaseOrderAuditEvent(
    val auditId: String,
    val projectId: String,
    val purchaseOrderId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val details: String? = null
)
