package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Master aggregate representing a formal commercial Purchase Order placed with a Vendor (Module 12 Step 05).
 */
data class VendorPurchaseOrder(
    val purchaseOrderId: String,
    val projectId: String,
    val orderNumber: String,
    val vendorId: String,
    val status: VendorPurchaseOrderStatus = VendorPurchaseOrderStatus.DRAFT,
    val orderDate: Long = System.currentTimeMillis(),
    val requestedBy: String,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val issuedBy: String? = null,
    val issuedAt: Long? = null,
    val expectedDeliveryDate: Long? = null,
    val deliveryLocation: String? = null,
    val currency: String = "BDT",
    val subtotal: Money,
    val taxAmount: Money = Money.ZERO,
    val discountAmount: Money = Money.ZERO,
    val totalAmount: Money,
    val notes: String? = null,
    val sourceReferenceType: String? = null,
    val sourceReferenceId: String? = null,
    val items: List<VendorPurchaseOrderItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)
