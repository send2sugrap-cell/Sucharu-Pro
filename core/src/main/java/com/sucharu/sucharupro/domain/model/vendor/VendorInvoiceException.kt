package com.sucharu.sucharupro.domain.model.vendor

/**
 * Structured 3-way matching exception record (Module 12 Step 07).
 */
data class VendorInvoiceException(
    val exceptionId: String,
    val projectId: String,
    val invoiceId: String,
    val matchId: String,
    val exceptionType: VendorInvoiceExceptionType,
    val description: String,
    val resolved: Boolean = false,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
