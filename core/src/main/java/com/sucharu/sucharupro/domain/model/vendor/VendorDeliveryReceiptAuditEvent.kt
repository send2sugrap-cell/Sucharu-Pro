package com.sucharu.sucharupro.domain.model.vendor

/**
 * Append-only audit record capturing significant lifecycle events on a VendorDeliveryReceipt (Module 12 Step 06).
 */
data class VendorDeliveryReceiptAuditEvent(
    val auditId: String,
    val projectId: String,
    val deliveryReceiptId: String,
    val purchaseOrderId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val details: String? = null
)
