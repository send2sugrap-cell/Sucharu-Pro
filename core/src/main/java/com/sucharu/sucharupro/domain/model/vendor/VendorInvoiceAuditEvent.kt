package com.sucharu.sucharupro.domain.model.vendor

/**
 * Append-only audit record capturing significant lifecycle events on a VendorInvoice (Module 12 Step 07).
 */
data class VendorInvoiceAuditEvent(
    val auditId: String,
    val projectId: String,
    val invoiceId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val details: String? = null
)
