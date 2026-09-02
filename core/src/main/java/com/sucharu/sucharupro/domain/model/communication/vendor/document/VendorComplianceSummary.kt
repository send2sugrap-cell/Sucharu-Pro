package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Project-and-vendor-scoped compliance health summary (Module 10 Step 06).
 */
data class VendorComplianceSummary(
    val projectId: String,
    val vendorId: String,
    val totalRequiredDocuments: Int,
    val submittedDocuments: Int,
    val approvedDocuments: Int,
    val pendingDocuments: Int,
    val rejectedDocuments: Int,
    val expiredDocuments: Int,
    val expiringSoonDocuments: Int,
    val missingRequiredDocumentTypes: List<VendorDocumentType>,
    val compliancePercentage: Double,
    val overallStatus: VendorComplianceStatus,
    val evaluatedAt: Long = System.currentTimeMillis()
)
