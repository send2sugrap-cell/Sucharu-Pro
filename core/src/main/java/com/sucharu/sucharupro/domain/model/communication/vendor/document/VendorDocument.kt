package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Core aggregate root representing a Vendor & Supplier Document / Compliance Record (Module 10 Step 06).
 *
 * Security: Provider-neutral file abstraction. No credentials, tokens, or private secrets stored.
 */
data class VendorDocument(
    val documentId: String,
    val documentNo: String,
    val projectId: String,
    val vendorId: String,
    val documentType: VendorDocumentType,
    val title: String,
    val description: String = "",
    val status: VendorDocumentStatus = VendorDocumentStatus.SUBMITTED,
    val verificationStatus: VendorDocumentVerificationStatus = VendorDocumentVerificationStatus.PENDING_REVIEW,
    val fileReferenceId: String,
    val fileName: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val documentVersion: Int = 1,
    val issueDate: Long? = null,
    val expiryDate: Long? = null,
    val requestedAt: Long? = null,
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val approvedAt: Long? = null,
    val rejectedAt: Long? = null,
    val createdBy: String,
    val submittedBy: String,
    val reviewedBy: String? = null,
    val approvedBy: String? = null,
    val rejectedBy: String? = null,
    val rejectionReason: String? = null,
    val requestId: String? = null,
    val communicationId: String? = null,
    val idempotencyKey: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(documentId.isNotBlank()) { "documentId cannot be blank" }
        require(documentNo.isNotBlank()) { "documentNo cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(title.isNotBlank()) { "title cannot be blank" }
        require(createdBy.isNotBlank()) { "createdBy cannot be blank" }
        require(documentVersion >= 1) { "documentVersion must be >= 1" }
        if (issueDate != null && expiryDate != null) {
            require(expiryDate >= issueDate) { "expiryDate cannot precede issueDate" }
        }
    }

    val isExpired: Boolean
        get() = expiryDate != null && expiryDate < System.currentTimeMillis()

    val isApproved: Boolean
        get() = status == VendorDocumentStatus.APPROVED && verificationStatus == VendorDocumentVerificationStatus.VERIFIED
}
