package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Immutable decision record of a vendor document review action (Module 10 Step 06).
 */
data class VendorDocumentReview(
    val reviewId: String,
    val reviewNo: String,
    val projectId: String,
    val documentId: String,
    val vendorId: String,
    val documentVersion: Int,
    val reviewStatus: VendorDocumentVerificationStatus,
    val reviewedBy: String,
    val reviewedAt: Long = System.currentTimeMillis(),
    val remarks: String = "",
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(reviewId.isNotBlank()) { "reviewId cannot be blank" }
        require(reviewNo.isNotBlank()) { "reviewNo cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(documentId.isNotBlank()) { "documentId cannot be blank" }
        require(vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(reviewedBy.isNotBlank()) { "reviewedBy cannot be blank" }
        require(documentVersion >= 1) { "documentVersion must be >= 1" }
    }
}
