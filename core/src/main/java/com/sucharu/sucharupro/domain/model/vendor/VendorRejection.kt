package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Master aggregate representing formally rejected goods/services (Module 12 Step 08).
 */
data class VendorRejection(
    val rejectionId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val deliveryReceiptItemId: String? = null,
    val inspectionId: String? = null,
    val rejectionReference: String,
    val rejectionType: String = "QUALITY_REJECTION",
    val rejectionReason: String,
    val rejectedQuantity: BigDecimal,
    val rejectedValue: Money = Money.ZERO,
    val status: VendorRejectionStatus = VendorRejectionStatus.DRAFT,
    val disposition: VendorRejectionDisposition = VendorRejectionDisposition.RETURN_TO_VENDOR,
    val replacementRequired: Boolean = false,
    val returnRequired: Boolean = true,
    val creditRequired: Boolean = false,
    val notes: String? = null,
    val vendorResponse: String? = null,
    val vendorResponseAt: Long? = null,
    val resolutionNotes: String? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)
