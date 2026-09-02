package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Immutable historical revision record of a Vendor Document (Module 10 Step 06).
 */
data class VendorDocumentVersion(
    val versionId: String,
    val projectId: String,
    val documentId: String,
    val vendorId: String,
    val versionNumber: Int,
    val fileReferenceId: String,
    val fileName: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val issueDate: Long? = null,
    val expiryDate: Long? = null,
    val status: VendorDocumentStatus,
    val verificationStatus: VendorDocumentVerificationStatus,
    val submittedBy: String,
    val submittedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(versionId.isNotBlank()) { "versionId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(documentId.isNotBlank()) { "documentId cannot be blank" }
        require(vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(versionNumber >= 1) { "versionNumber must be >= 1" }
        require(fileReferenceId.isNotBlank()) { "fileReferenceId cannot be blank" }
    }
}
