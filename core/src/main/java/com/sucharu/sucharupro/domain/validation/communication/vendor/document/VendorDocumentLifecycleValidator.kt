package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentStatus
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentRequestStatus

/**
 * Validates state transitions for Vendor Document and Vendor Document Request lifecycles (Module 10 Step 06).
 */
object VendorDocumentLifecycleValidator {

    private val allowedDocumentTransitions: Map<VendorDocumentStatus, Set<VendorDocumentStatus>> = mapOf(
        VendorDocumentStatus.REQUESTED to setOf(
            VendorDocumentStatus.SUBMITTED,
            VendorDocumentStatus.CANCELLED
        ),
        VendorDocumentStatus.SUBMITTED to setOf(
            VendorDocumentStatus.UNDER_REVIEW,
            VendorDocumentStatus.APPROVED,
            VendorDocumentStatus.REJECTED,
            VendorDocumentStatus.CANCELLED
        ),
        VendorDocumentStatus.UNDER_REVIEW to setOf(
            VendorDocumentStatus.APPROVED,
            VendorDocumentStatus.REJECTED,
            VendorDocumentStatus.CANCELLED
        ),
        VendorDocumentStatus.APPROVED to setOf(
            VendorDocumentStatus.EXPIRED,
            VendorDocumentStatus.RENEWAL_REQUIRED
        ),
        VendorDocumentStatus.REJECTED to setOf(
            VendorDocumentStatus.SUBMITTED,
            VendorDocumentStatus.CANCELLED
        ),
        VendorDocumentStatus.EXPIRED to setOf(
            VendorDocumentStatus.RENEWAL_REQUIRED,
            VendorDocumentStatus.SUBMITTED
        ),
        VendorDocumentStatus.RENEWAL_REQUIRED to setOf(
            VendorDocumentStatus.SUBMITTED,
            VendorDocumentStatus.CANCELLED
        ),
        VendorDocumentStatus.CANCELLED to emptySet()
    )

    private val allowedRequestTransitions: Map<VendorDocumentRequestStatus, Set<VendorDocumentRequestStatus>> = mapOf(
        VendorDocumentRequestStatus.OPEN to setOf(
            VendorDocumentRequestStatus.SUBMITTED,
            VendorDocumentRequestStatus.OVERDUE,
            VendorDocumentRequestStatus.CANCELLED
        ),
        VendorDocumentRequestStatus.OVERDUE to setOf(
            VendorDocumentRequestStatus.SUBMITTED,
            VendorDocumentRequestStatus.CANCELLED
        ),
        VendorDocumentRequestStatus.SUBMITTED to setOf(
            VendorDocumentRequestStatus.UNDER_REVIEW,
            VendorDocumentRequestStatus.COMPLETED,
            VendorDocumentRequestStatus.OPEN, // if rejected and reopened
            VendorDocumentRequestStatus.CANCELLED
        ),
        VendorDocumentRequestStatus.UNDER_REVIEW to setOf(
            VendorDocumentRequestStatus.COMPLETED,
            VendorDocumentRequestStatus.OPEN, // if revision requested
            VendorDocumentRequestStatus.CANCELLED
        ),
        VendorDocumentRequestStatus.COMPLETED to emptySet(),
        VendorDocumentRequestStatus.CANCELLED to emptySet()
    )

    fun validateDocumentTransition(
        from: VendorDocumentStatus,
        to: VendorDocumentStatus
    ): DomainResult<Unit> {
        if (from == to) return DomainResult.Success(Unit)
        if (from.isTerminal) {
            return DomainResult.Error(message = "Cannot transition from terminal status '$from'.")
        }
        val permitted = allowedDocumentTransitions[from] ?: emptySet()
        return if (to in permitted) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Illegal vendor document transition: cannot transition from '$from' to '$to'.")
        }
    }

    fun validateRequestTransition(
        from: VendorDocumentRequestStatus,
        to: VendorDocumentRequestStatus
    ): DomainResult<Unit> {
        if (from == to) return DomainResult.Success(Unit)
        if (from.isTerminal) {
            return DomainResult.Error(message = "Cannot transition from terminal request status '$from'.")
        }
        val permitted = allowedRequestTransitions[from] ?: emptySet()
        return if (to in permitted) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Illegal document request transition: cannot transition from '$from' to '$to'.")
        }
    }
}
