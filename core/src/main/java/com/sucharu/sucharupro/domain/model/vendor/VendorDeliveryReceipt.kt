package com.sucharu.sucharupro.domain.model.vendor

/**
 * Master aggregate representing the receipt and physical inspection of goods/services delivered by a Vendor (Module 12 Step 06).
 */
data class VendorDeliveryReceipt(
    val deliveryReceiptId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val receiptNumber: String,
    val purchaseOrderId: String,
    val vendorId: String,
    val vendorDeliveryReference: String? = null,
    val receiptDate: Long = System.currentTimeMillis(),
    val receivedAt: Long? = null,
    val receivedBy: String,
    val status: VendorDeliveryReceiptStatus = VendorDeliveryReceiptStatus.DRAFT,
    val warehouseId: String? = null,
    val remarks: String? = null,
    val items: List<VendorDeliveryReceiptItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)
