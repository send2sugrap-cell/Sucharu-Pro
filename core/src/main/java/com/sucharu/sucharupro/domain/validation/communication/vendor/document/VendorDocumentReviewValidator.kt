package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentVerificationStatus

/**
 * Validates document review actions, approval and rejection inputs (Module 10 Step 06).
 */
object VendorDocumentReviewValidator {

    fun validateReview(
        projectId: String,
        documentId: String,
        vendorId: String,
        status: VendorDocumentVerificationStatus,
        reviewedBy: String,
        remarks: String = "",
        rejectionReason: String? = null
    ): DomainResult<Unit> {
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (documentId.isBlank()) return DomainResult.Error(message = "Document ID cannot be blank.")
        if (vendorId.isBlank()) return DomainResult.Error(message = "Vendor ID cannot be blank.")
        if (reviewedBy.isBlank()) return DomainResult.Error(message = "Reviewer ID cannot be blank.")

        if (status == VendorDocumentVerificationStatus.REJECTED && rejectionReason.isNullOrBlank()) {
            return DomainResult.Error(message = "Rejection reason is required when rejecting a document.")
        }

        return DomainResult.Success(Unit)
    }
}
