package com.sucharu.sucharupro.domain.model.vendor

import java.math.BigDecimal

/**
 * Master aggregate representing a formal Quality Inspection against vendor deliveries (Module 12 Step 08).
 */
data class VendorQualityInspection(
    val inspectionId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val inspectionReference: String,
    val inspectionType: VendorInspectionType = VendorInspectionType.RECEIVING_INSPECTION,
    val inspectionStatus: VendorInspectionStatus = VendorInspectionStatus.DRAFT,
    val inspectedBy: String? = null,
    val inspectionStartedAt: Long? = null,
    val inspectionCompletedAt: Long? = null,
    val receivedQuantity: BigDecimal,
    val acceptedQuantity: BigDecimal = BigDecimal.ZERO,
    val rejectedQuantity: BigDecimal = BigDecimal.ZERO,
    val conditionalQuantity: BigDecimal = BigDecimal.ZERO,
    val overallResult: InspectionResult? = null,
    val notes: String? = null,
    val items: List<VendorQualityInspectionItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)
