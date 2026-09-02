package com.sucharu.sucharupro.domain.model.vendor

import java.math.BigDecimal

/**
 * Entity representing an individual or classified quality defect detected during inspection.
 */
data class VendorDefect(
    val defectId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val inspectionId: String,
    val inspectionItemId: String? = null,
    val vendorId: String,
    val defectType: VendorDefectType = VendorDefectType.QUALITY_DEFECT,
    val severity: VendorDefectSeverity = VendorDefectSeverity.MEDIUM,
    val description: String,
    val quantityAffected: BigDecimal = BigDecimal.ZERO,
    val detectedAt: Long = System.currentTimeMillis(),
    val detectedBy: String = "system",
    val evidenceReference: String? = null,
    val status: String = "OPEN",
    val resolutionReference: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)
