package com.sucharu.sucharupro.domain.model.vendor

/**
 * Supporting evidence reference attached to a quality inspection, defect, rejection, or dispute.
 */
data class VendorQualityEvidence(
    val evidenceId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val sourceType: String, // INSPECTION, DEFECT, REJECTION, DISPUTE
    val sourceId: String,
    val fileReference: String,
    val fileName: String,
    val fileType: String,
    val description: String? = null,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis(),
    val checksum: String? = null
)
