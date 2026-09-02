package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentVerificationStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorDocumentReviewValidatorTest {

    private val validProjectId = "proj-001"
    private val validDocumentId = "doc-001"
    private val validVendorId = "ven-001"
    private val validReviewedBy = "user-manager-01"

    // ─────────────────────────────────────────────
    // Happy paths
    // ─────────────────────────────────────────────

    @Test
    fun validateReview_verifiedStatus_noRejectionReason_succeeds() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.VERIFIED,
            reviewedBy = validReviewedBy
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateReview_pendingReviewStatus_succeeds() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.PENDING_REVIEW,
            reviewedBy = validReviewedBy
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateReview_expiredStatus_succeeds() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.EXPIRED,
            reviewedBy = validReviewedBy
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateReview_rejectedStatus_withRejectionReason_succeeds() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.REJECTED,
            reviewedBy = validReviewedBy,
            rejectionReason = "Document is expired and unreadable."
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateReview_withOptionalRemarks_succeeds() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.VERIFIED,
            reviewedBy = validReviewedBy,
            remarks = "All looks good. Stamp verified.",
            rejectionReason = null
        )
        assertTrue(result is DomainResult.Success)
    }

    // ─────────────────────────────────────────────
    // Blank field validation
    // ─────────────────────────────────────────────

    @Test
    fun validateReview_blankProjectId_fails() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = "",
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.VERIFIED,
            reviewedBy = validReviewedBy
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateReview_blankDocumentId_fails() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = "  ",
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.VERIFIED,
            reviewedBy = validReviewedBy
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateReview_blankVendorId_fails() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = "",
            status = VendorDocumentVerificationStatus.VERIFIED,
            reviewedBy = validReviewedBy
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateReview_blankReviewedBy_fails() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.VERIFIED,
            reviewedBy = "   "
        )
        assertTrue(result is DomainResult.Error)
    }

    // ─────────────────────────────────────────────
    // Rejection reason required when status is REJECTED
    // ─────────────────────────────────────────────

    @Test
    fun validateReview_rejectedStatus_nullRejectionReason_fails() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.REJECTED,
            reviewedBy = validReviewedBy,
            rejectionReason = null
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateReview_rejectedStatus_blankRejectionReason_fails() {
        val result = VendorDocumentReviewValidator.validateReview(
            projectId = validProjectId,
            documentId = validDocumentId,
            vendorId = validVendorId,
            status = VendorDocumentVerificationStatus.REJECTED,
            reviewedBy = validReviewedBy,
            rejectionReason = "   "
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateReview_nonRejectedStatus_noRejectionReason_succeeds() {
        val nonRejectedStatuses = VendorDocumentVerificationStatus.entries
            .filter { it != VendorDocumentVerificationStatus.REJECTED }
        nonRejectedStatuses.forEach { status ->
            val result = VendorDocumentReviewValidator.validateReview(
                projectId = validProjectId,
                documentId = validDocumentId,
                vendorId = validVendorId,
                status = status,
                reviewedBy = validReviewedBy,
                rejectionReason = null
            )
            assertTrue("$status without rejectionReason should succeed", result is DomainResult.Success)
        }
    }
}
