package com.sucharu.sucharupro.domain.validation.affiliate

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequestStatus

/**
 * Validator for Affiliate Payout Request state transitions (Module 23 Step 05).
 */
object AffiliatePayoutRequestValidator {

    /**
     * Validates status transition against canonical state machine.
     */
    fun validateStatusTransition(
        currentStatus: AffiliatePayoutRequestStatus,
        newStatus: AffiliatePayoutRequestStatus
    ): DomainResult<Unit> {
        if (currentStatus == newStatus) {
            return DomainResult.Success(Unit)
        }

        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Terminal payout request status '${currentStatus.name}' cannot be transitioned to '${newStatus.name}'."
            )
        }

        val isValid = when (currentStatus) {
            AffiliatePayoutRequestStatus.REQUESTED -> newStatus in setOf(
                AffiliatePayoutRequestStatus.UNDER_REVIEW,
                AffiliatePayoutRequestStatus.APPROVED,
                AffiliatePayoutRequestStatus.REJECTED,
                AffiliatePayoutRequestStatus.CANCELLED
            )
            AffiliatePayoutRequestStatus.UNDER_REVIEW -> newStatus in setOf(
                AffiliatePayoutRequestStatus.APPROVED,
                AffiliatePayoutRequestStatus.REJECTED,
                AffiliatePayoutRequestStatus.CANCELLED
            )
            AffiliatePayoutRequestStatus.APPROVED -> newStatus in setOf(
                AffiliatePayoutRequestStatus.PROCESSING,
                AffiliatePayoutRequestStatus.CANCELLED,
                AffiliatePayoutRequestStatus.REJECTED
            )
            AffiliatePayoutRequestStatus.PROCESSING -> newStatus in setOf(
                AffiliatePayoutRequestStatus.COMPLETED,
                AffiliatePayoutRequestStatus.FAILED
            )
            else -> false
        }

        if (!isValid) {
            return DomainResult.Error(
                message = "Invalid payout request status transition from '${currentStatus.name}' to '${newStatus.name}'."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Enforces strict separation of duties: Requester cannot review, approve, or reject their own payout.
     */
    fun validateSeparationOfDuties(
        requestedBy: String,
        reviewerId: String
    ): DomainResult<Unit> {
        if (requestedBy.equals(reviewerId, ignoreCase = true)) {
            return DomainResult.Error(
                message = "Separation of duties violation: Payout requester '$requestedBy' cannot approve, review, or reject their own payout request."
            )
        }
        return DomainResult.Success(Unit)
    }
}
