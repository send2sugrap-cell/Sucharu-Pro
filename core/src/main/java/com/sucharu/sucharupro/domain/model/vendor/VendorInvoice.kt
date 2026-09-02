package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Master aggregate representing a commercial Vendor Invoice (Module 12 Step 07).
 */
data class VendorInvoice(
    val invoiceId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val vendorId: String,
    val purchaseOrderId: String,
    val invoiceNumber: String,
    val vendorInvoiceNumber: String,
    val invoiceDate: Long = System.currentTimeMillis(),
    val receivedDate: Long = System.currentTimeMillis(),
    val currency: String = "BDT",
    val subtotal: Money = Money.ZERO,
    val taxAmount: Money = Money.ZERO,
    val discountAmount: Money = Money.ZERO,
    val shippingAmount: Money = Money.ZERO,
    val otherCharges: Money = Money.ZERO,
    val totalAmount: Money = Money.ZERO,
    val notes: String? = null,
    val status: VendorInvoiceStatus = VendorInvoiceStatus.DRAFT,
    val matchStatus: VendorInvoiceMatchStatus = VendorInvoiceMatchStatus.NOT_MATCHED,
    val items: List<VendorInvoiceItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)
