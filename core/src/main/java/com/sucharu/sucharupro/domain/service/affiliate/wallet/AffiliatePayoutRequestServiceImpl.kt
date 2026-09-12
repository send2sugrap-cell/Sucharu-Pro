package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutRequestRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletRepository
import com.sucharu.sucharupro.domain.validation.affiliate.AffiliatePayoutRequestValidator
import java.util.UUID

/**
 * Authoritative Domain Service implementation for Affiliate Payout Request lifecycle (Module 23 Step 05).
 */
class AffiliatePayoutRequestServiceImpl(
    private val walletRepository: AffiliateWalletRepository,
    private val payoutRequestRepository: AffiliatePayoutRequestRepository,
    private val holdService: AffiliateWalletHoldService
) : AffiliatePayoutRequestService {

    override suspend fun submitPayoutRequest(
        tenantId: String,
        walletId: String,
        requestedAmount: Money,
        payoutMethodType: AffiliatePayoutMethodType,
        accountName: String,
        accountNumber: String,
        provider: String?,
        branchRouting: String?,
        idempotencyKey: String?,
        minimumThreshold: Money?,
        actorId: String
    ): DomainResult<AffiliatePayoutRequest> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        if (!requestedAmount.isPositive()) {
            return DomainResult.Error(message = "Requested payout amount must be positive. Provided: ${requestedAmount.formatted()}")
        }
        if (accountName.isBlank() || accountNumber.isBlank()) {
            return DomainResult.Error(message = "Payout method account name and account number cannot be blank.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "RequestedBy actor ID cannot be blank.")
        }

        val walletRes = walletRepository.getWalletById(tenantId, walletId)
        if (walletRes is DomainResult.Error) return walletRes
        val wallet = (walletRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found.")

        // 1. Evaluate Payout Eligibility via Step 04 Gate
        val eligibilityRes = holdService.evaluatePayoutEligibility(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = requestedAmount,
            minimumThreshold = minimumThreshold
        )
        if (eligibilityRes is DomainResult.Error) return eligibilityRes
        val eligibility = (eligibilityRes as DomainResult.Success).data

        if (!eligibility.isEligible) {
            return DomainResult.Error(
                message = "Payout request rejected: ${eligibility.blockingReasons.joinToString("; ")}"
            )
        }

        // 2. Check Idempotency
        val payoutRef = "PAYOUT-REF-" + UUID.randomUUID().toString().take(8).uppercase()
        val idempKey = idempotencyKey ?: "PAYOUT-REQ:$tenantId:$walletId:${requestedAmount.amount}:$payoutRef"

        val existingRes = payoutRequestRepository.getRequestByIdempotencyKey(tenantId, idempKey)
        if (existingRes is DomainResult.Success && existingRes.data != null) {
            return DomainResult.Success(existingRes.data)
        }

        // 3. Place Payout Reservation Hold via Step 04
        val holdRes = holdService.createHold(
            tenantId = tenantId,
            walletId = walletId,
            amount = requestedAmount,
            holdType = AffiliateWalletHoldType.PAYOUT_RESERVATION,
            reason = "Payout reservation for reference $payoutRef",
            referenceId = payoutRef,
            actorId = actorId
        )
        if (holdRes is DomainResult.Error) return holdRes
        val hold = (holdRes as DomainResult.Success).data

        // 4. Create and Save Payout Request
        val requestId = "PO-REQ-" + UUID.randomUUID().toString().take(8).uppercase()
        val now = System.currentTimeMillis()

        val payoutRequest = AffiliatePayoutRequest(
            requestId = requestId,
            tenantId = tenantId,
            walletId = walletId,
            affiliateId = wallet.affiliateId,
            requestedAmount = requestedAmount,
            currency = wallet.currency,
            payoutMethodType = payoutMethodType,
            payoutMethodAccountName = accountName,
            payoutMethodAccountNumber = accountNumber,
            payoutMethodProvider = provider,
            payoutMethodBranchRouting = branchRouting,
            status = AffiliatePayoutRequestStatus.REQUESTED,
            reservationHoldId = hold.holdId,
            payoutReference = payoutRef,
            idempotencyKey = idempKey,
            requestedBy = actorId,
            requestedAt = now,
            updatedAt = now,
            version = 1L
        )

        return payoutRequestRepository.savePayoutRequest(payoutRequest)
    }

    override suspend fun getPayoutRequestDetails(
        tenantId: String,
        requestId: String
    ): DomainResult<AffiliatePayoutRequest?> {
        return payoutRequestRepository.getRequestById(tenantId, requestId)
    }

    override suspend fun listPayoutRequestsForWallet(
        tenantId: String,
        walletId: String
    ): DomainResult<List<AffiliatePayoutRequest>> {
        return payoutRequestRepository.listRequestsForWallet(tenantId, walletId)
    }

    override suspend fun updatePayoutRequestStatus(
        tenantId: String,
        requestId: String,
        newStatus: AffiliatePayoutRequestStatus,
        reason: String?,
        actorId: String
    ): DomainResult<AffiliatePayoutRequest> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank for status update.")
        }

        val requestRes = payoutRequestRepository.getRequestById(tenantId, requestId)
        if (requestRes is DomainResult.Error) return requestRes
        val existing = (requestRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Payout request '$requestId' not found.")

        // Validate state machine transition
        val valRes = AffiliatePayoutRequestValidator.validateStatusTransition(existing.status, newStatus)
        if (valRes is DomainResult.Error) return valRes

        // If transitioning to CANCELLED or REJECTED, release the payout reservation hold!
        if ((newStatus == AffiliatePayoutRequestStatus.CANCELLED || newStatus == AffiliatePayoutRequestStatus.REJECTED) &&
            !existing.reservationHoldId.isNullOrBlank()
        ) {
            val relReason = "Payout request $requestId ${newStatus.name}${if (!reason.isNullOrBlank()) ": $reason" else ""}"
            holdService.releaseHold(
                tenantId = tenantId,
                walletId = existing.walletId,
                holdId = existing.reservationHoldId,
                releaseReason = relReason,
                actorId = actorId
            )
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = newStatus,
            rejectionReason = if (newStatus == AffiliatePayoutRequestStatus.REJECTED) reason ?: existing.rejectionReason else existing.rejectionReason,
            reviewNotes = if (!reason.isNullOrBlank()) reason else existing.reviewNotes,
            updatedAt = now,
            version = existing.version + 1
        )

        return payoutRequestRepository.updatePayoutRequestStatus(updated)
    }

    override suspend fun reviewPayoutRequest(
        tenantId: String,
        requestId: String,
        notes: String?,
        reviewerId: String
    ): DomainResult<AffiliatePayoutRequest> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        if (reviewerId.isBlank()) {
            return DomainResult.Error(message = "Reviewer ID cannot be blank.")
        }

        val requestRes = payoutRequestRepository.getRequestById(tenantId, requestId)
        if (requestRes is DomainResult.Error) return requestRes
        val existing = (requestRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Payout request '$requestId' not found.")

        // Enforce separation of duties: Requester cannot review their own payout
        val sodRes = AffiliatePayoutRequestValidator.validateSeparationOfDuties(existing.requestedBy, reviewerId)
        if (sodRes is DomainResult.Error) return sodRes

        val valRes = AffiliatePayoutRequestValidator.validateStatusTransition(existing.status, AffiliatePayoutRequestStatus.UNDER_REVIEW)
        if (valRes is DomainResult.Error) return valRes

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = AffiliatePayoutRequestStatus.UNDER_REVIEW,
            reviewNotes = notes ?: existing.reviewNotes,
            updatedAt = now,
            version = existing.version + 1
        )

        return payoutRequestRepository.updatePayoutRequestStatus(updated)
    }

    override suspend fun approvePayoutRequest(
        tenantId: String,
        requestId: String,
        notes: String?,
        approverId: String
    ): DomainResult<AffiliatePayoutRequest> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        if (approverId.isBlank()) {
            return DomainResult.Error(message = "Approver ID cannot be blank.")
        }

        val requestRes = payoutRequestRepository.getRequestById(tenantId, requestId)
        if (requestRes is DomainResult.Error) return requestRes
        val existing = (requestRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Payout request '$requestId' not found.")

        // Enforce separation of duties: Requester cannot approve their own payout
        val sodRes = AffiliatePayoutRequestValidator.validateSeparationOfDuties(existing.requestedBy, approverId)
        if (sodRes is DomainResult.Error) return sodRes

        val valRes = AffiliatePayoutRequestValidator.validateStatusTransition(existing.status, AffiliatePayoutRequestStatus.APPROVED)
        if (valRes is DomainResult.Error) return valRes

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = AffiliatePayoutRequestStatus.APPROVED,
            reviewNotes = notes ?: existing.reviewNotes,
            updatedAt = now,
            version = existing.version + 1
        )

        return payoutRequestRepository.updatePayoutRequestStatus(updated)
    }

    override suspend fun rejectPayoutRequest(
        tenantId: String,
        requestId: String,
        rejectionReason: String,
        reviewerId: String
    ): DomainResult<AffiliatePayoutRequest> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        if (rejectionReason.isBlank()) {
            return DomainResult.Error(message = "Rejection reason cannot be blank.")
        }
        if (reviewerId.isBlank()) {
            return DomainResult.Error(message = "Reviewer ID cannot be blank.")
        }

        val requestRes = payoutRequestRepository.getRequestById(tenantId, requestId)
        if (requestRes is DomainResult.Error) return requestRes
        val existing = (requestRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Payout request '$requestId' not found.")

        // Enforce separation of duties: Requester cannot reject their own payout as reviewer
        val sodRes = AffiliatePayoutRequestValidator.validateSeparationOfDuties(existing.requestedBy, reviewerId)
        if (sodRes is DomainResult.Error) return sodRes

        val valRes = AffiliatePayoutRequestValidator.validateStatusTransition(existing.status, AffiliatePayoutRequestStatus.REJECTED)
        if (valRes is DomainResult.Error) return valRes

        // Release payout reservation hold
        if (!existing.reservationHoldId.isNullOrBlank()) {
            holdService.releaseHold(
                tenantId = tenantId,
                walletId = existing.walletId,
                holdId = existing.reservationHoldId,
                releaseReason = "Payout request $requestId REJECTED: $rejectionReason",
                actorId = reviewerId
            )
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = AffiliatePayoutRequestStatus.REJECTED,
            rejectionReason = rejectionReason,
            reviewNotes = rejectionReason,
            updatedAt = now,
            version = existing.version + 1
        )

        return payoutRequestRepository.updatePayoutRequestStatus(updated)
    }
}
