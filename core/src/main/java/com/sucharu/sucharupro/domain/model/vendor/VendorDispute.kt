package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Master aggregate representing a formal commercial or quality dispute with a Vendor (Module 12 Step 08).
 */
data class VendorDispute(
    val disputeId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val invoiceId: String? = null,
    val inspectionId: String? = null,
    val rejectionId: String? = null,
    val disputeReference: String,
    val disputeType: VendorDisputeType = VendorDisputeType.QUALITY,
    val priority: VendorDisputePriority = VendorDisputePriority.MEDIUM,
    val status: VendorDisputeStatus = VendorDisputeStatus.OPEN,
    val subject: String,
    val description: String,
    val disputedQuantity: BigDecimal = BigDecimal.ZERO,
    val disputedAmount: Money = Money.ZERO,
    val raisedBy: String,
    val assignedTo: String? = null,
    val vendorResponseDueAt: Long? = null,
    val vendorResponse: String? = null,
    val vendorResponseAt: Long? = null,
    val resolutionProposal: String? = null,
    val resolution: String? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val closedAt: Long? = null,
    val closedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)
