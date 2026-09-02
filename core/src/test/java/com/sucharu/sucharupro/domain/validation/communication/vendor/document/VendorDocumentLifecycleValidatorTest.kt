package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentRequestStatus
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorDocumentLifecycleValidatorTest {

    // ─────────────────────────────────────────────
    // validateDocumentTransition — happy paths
    // ─────────────────────────────────────────────

    @Test
    fun documentTransition_sameStatus_alwaysSucceeds() {
        VendorDocumentStatus.entries.forEach { status ->
            val result = VendorDocumentLifecycleValidator.validateDocumentTransition(status, status)
            assertTrue("Same-status no-op should succeed for $status", result is DomainResult.Success)
        }
    }

    @Test
    fun documentTransition_requested_to_submitted_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.REQUESTED, VendorDocumentStatus.SUBMITTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_requested_to_cancelled_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.REQUESTED, VendorDocumentStatus.CANCELLED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_submitted_to_underReview_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.SUBMITTED, VendorDocumentStatus.UNDER_REVIEW
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_submitted_to_approved_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.SUBMITTED, VendorDocumentStatus.APPROVED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_submitted_to_rejected_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.SUBMITTED, VendorDocumentStatus.REJECTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_underReview_to_approved_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.UNDER_REVIEW, VendorDocumentStatus.APPROVED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_underReview_to_rejected_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.UNDER_REVIEW, VendorDocumentStatus.REJECTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_approved_to_expired_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.APPROVED, VendorDocumentStatus.EXPIRED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_approved_to_renewalRequired_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.APPROVED, VendorDocumentStatus.RENEWAL_REQUIRED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_rejected_to_submitted_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.REJECTED, VendorDocumentStatus.SUBMITTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_expired_to_renewalRequired_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.EXPIRED, VendorDocumentStatus.RENEWAL_REQUIRED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_expired_to_submitted_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.EXPIRED, VendorDocumentStatus.SUBMITTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun documentTransition_renewalRequired_to_submitted_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.RENEWAL_REQUIRED, VendorDocumentStatus.SUBMITTED
        )
        assertTrue(result is DomainResult.Success)
    }

    // ─────────────────────────────────────────────
    // validateDocumentTransition — illegal transitions
    // ─────────────────────────────────────────────

    @Test
    fun documentTransition_fromCancelled_toAnyOther_denied() {
        VendorDocumentStatus.entries
            .filter { it != VendorDocumentStatus.CANCELLED }
            .forEach { target ->
                val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
                    VendorDocumentStatus.CANCELLED, target
                )
                assertTrue("Transition from terminal CANCELLED to $target must fail", result is DomainResult.Error)
            }
    }

    @Test
    fun documentTransition_requested_to_approved_isIllegal() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.REQUESTED, VendorDocumentStatus.APPROVED
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun documentTransition_approved_to_submitted_isIllegal() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.APPROVED, VendorDocumentStatus.SUBMITTED
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun documentTransition_approved_to_rejected_isIllegal() {
        val result = VendorDocumentLifecycleValidator.validateDocumentTransition(
            VendorDocumentStatus.APPROVED, VendorDocumentStatus.REJECTED
        )
        assertTrue(result is DomainResult.Error)
    }

    // ─────────────────────────────────────────────
    // validateRequestTransition — happy paths
    // ─────────────────────────────────────────────

    @Test
    fun requestTransition_sameStatus_alwaysSucceeds() {
        VendorDocumentRequestStatus.entries.forEach { status ->
            val result = VendorDocumentLifecycleValidator.validateRequestTransition(status, status)
            assertTrue("Same-status no-op should succeed for $status", result is DomainResult.Success)
        }
    }

    @Test
    fun requestTransition_open_to_submitted_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.OPEN, VendorDocumentRequestStatus.SUBMITTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun requestTransition_open_to_overdue_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.OPEN, VendorDocumentRequestStatus.OVERDUE
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun requestTransition_open_to_cancelled_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.OPEN, VendorDocumentRequestStatus.CANCELLED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun requestTransition_overdue_to_submitted_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.OVERDUE, VendorDocumentRequestStatus.SUBMITTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun requestTransition_submitted_to_underReview_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.SUBMITTED, VendorDocumentRequestStatus.UNDER_REVIEW
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun requestTransition_submitted_to_completed_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.SUBMITTED, VendorDocumentRequestStatus.COMPLETED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun requestTransition_underReview_to_completed_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.UNDER_REVIEW, VendorDocumentRequestStatus.COMPLETED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun requestTransition_underReview_to_open_revisionRequested_succeeds() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.UNDER_REVIEW, VendorDocumentRequestStatus.OPEN
        )
        assertTrue(result is DomainResult.Success)
    }

    // ─────────────────────────────────────────────
    // validateRequestTransition — terminal statuses
    // ─────────────────────────────────────────────

    @Test
    fun requestTransition_fromCompleted_toAnyOther_denied() {
        VendorDocumentRequestStatus.entries
            .filter { it != VendorDocumentRequestStatus.COMPLETED }
            .forEach { target ->
                val result = VendorDocumentLifecycleValidator.validateRequestTransition(
                    VendorDocumentRequestStatus.COMPLETED, target
                )
                assertTrue("Transition from terminal COMPLETED to $target must fail", result is DomainResult.Error)
            }
    }

    @Test
    fun requestTransition_fromCancelled_toAnyOther_denied() {
        VendorDocumentRequestStatus.entries
            .filter { it != VendorDocumentRequestStatus.CANCELLED }
            .forEach { target ->
                val result = VendorDocumentLifecycleValidator.validateRequestTransition(
                    VendorDocumentRequestStatus.CANCELLED, target
                )
                assertTrue("Transition from terminal CANCELLED to $target must fail", result is DomainResult.Error)
            }
    }

    @Test
    fun requestTransition_open_to_underReview_isIllegal() {
        val result = VendorDocumentLifecycleValidator.validateRequestTransition(
            VendorDocumentRequestStatus.OPEN, VendorDocumentRequestStatus.UNDER_REVIEW
        )
        assertTrue(result is DomainResult.Error)
    }
}
