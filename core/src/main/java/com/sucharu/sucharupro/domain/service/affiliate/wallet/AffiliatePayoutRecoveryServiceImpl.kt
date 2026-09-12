package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutDisbursementRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutRecoveryRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutRequestRepository
import java.util.UUID

/**
 * Authoritative Domain Service implementation for Affiliate Payout Failure Recovery, Retry, Reversal & Reconciliation (Module 23 Step 08).
 */
class AffiliatePayoutRecoveryServiceImpl(
    private val payoutRequestRepository: AffiliatePayoutRequestRepository,
    private val disbursementRepository: AffiliatePayoutDisbursementRepository,
    private val recoveryRepository: AffiliatePayoutRecoveryRepository,
    private val disbursementService: AffiliatePayoutDisbursementService,
    private val ledgerService: AffiliateWalletLedgerService,
    private val holdService: AffiliateWalletHoldService
) : AffiliatePayoutRecoveryService {

    override suspend fun retryFailedPayoutDisbursement(
        tenantId: String,
        requestId: String,
        actorId: String
    ): DomainResult<AffiliatePayoutDisbursementRecord> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank for retry.")
        }

        val requestRes = payoutRequestRepository.getRequestById(tenantId, requestId)
        if (requestRes is DomainResult.Error) return requestRes
        val request = (requestRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Payout request '$requestId' not found.")

        if (request.status == AffiliatePayoutRequestStatus.COMPLETED) {
            return DomainResult.Error(message = "Cannot retry a COMPLETED payout request '$requestId'.")
        }

        if (request.status != AffiliatePayoutRequestStatus.FAILED && request.status != AffiliatePayoutRequestStatus.APPROVED) {
            return DomainResult.Error(
                message = "Payout request '$requestId' is in state '${request.status.name}' (expected FAILED for retry)."
            )
        }

        // Transition back to APPROVED to re-enter disbursement gate safely
        val approvedRequest = request.copy(
            status = AffiliatePayoutRequestStatus.APPROVED,
            updatedAt = System.currentTimeMillis()
        )
        payoutRequestRepository.updatePayoutRequestStatus(approvedRequest)

        // Re-process disbursement
        return disbursementService.processPayoutDisbursement(tenantId, requestId, actorId)
    }

    override suspend fun reverseCompletedPayout(
        tenantId: String,
        requestId: String,
        reversalReason: String,
        actorId: String
    ): DomainResult<AffiliatePayoutReversalRecord> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        if (reversalReason.isBlank()) {
            return DomainResult.Error(message = "Reversal reason cannot be blank.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank for reversal.")
        }

        val requestRes = payoutRequestRepository.getRequestById(tenantId, requestId)
        if (requestRes is DomainResult.Error) return requestRes
        val request = (requestRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Payout request '$requestId' not found.")

        if (request.status != AffiliatePayoutRequestStatus.COMPLETED) {
            return DomainResult.Error(
                message = "Payout request '$requestId' is in state '${request.status.name}' (expected COMPLETED for reversal)."
            )
        }

        // 1. Fetch disbursements to locate original ledger entry ID
        val disbRes = disbursementRepository.listDisbursementsForRequest(tenantId, requestId)
        val disbursements = (disbRes as? DomainResult.Success)?.data ?: emptyList()
        val originalDisb = disbursements.firstOrNull { it.providerStatus == DisbursementProviderStatus.SUCCESS }

        // 2. Post compensating CREDIT to Step 02 immutable wallet ledger (DO NOT mutate original entry)
        val creditRes = ledgerService.creditWallet(
            tenantId = tenantId,
            walletId = request.walletId,
            amount = request.requestedAmount,
            referenceId = "REV-" + request.payoutReference,
            reason = "Compensating credit for reversed payout $requestId: $reversalReason",
            actorId = actorId
        )
        if (creditRes is DomainResult.Error) return creditRes
        val compensatingEntry = (creditRes as DomainResult.Success).data

        // 3. Update request status to REVERSED
        val reversedRequest = request.copy(
            status = AffiliatePayoutRequestStatus.REVERSED,
            reviewNotes = "Reversed: $reversalReason",
            updatedAt = System.currentTimeMillis()
        )
        payoutRequestRepository.updatePayoutRequestStatus(reversedRequest)

        // 4. Save reversal record
        val reversalRecord = AffiliatePayoutReversalRecord(
            reversalId = "REV-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = tenantId,
            requestId = requestId,
            walletId = request.walletId,
            affiliateId = request.affiliateId,
            reversedAmount = request.requestedAmount,
            currency = request.currency,
            originalLedgerEntryId = originalDisb?.ledgerEntryId,
            compensatingLedgerEntryId = compensatingEntry.entryId,
            reversalReason = reversalReason,
            reversedBy = actorId,
            reversedAt = System.currentTimeMillis()
        )

        return recoveryRepository.saveReversalRecord(reversalRecord)
    }

    override suspend fun reconcilePayoutDisbursement(
        tenantId: String,
        requestId: String,
        notes: String?,
        actorId: String
    ): DomainResult<AffiliatePayoutReconciliationRecord> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }

        val requestRes = payoutRequestRepository.getRequestById(tenantId, requestId)
        if (requestRes is DomainResult.Error) return requestRes
        val request = (requestRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Payout request '$requestId' not found.")

        val disbRes = disbursementRepository.listDisbursementsForRequest(tenantId, requestId)
        val disbursements = (disbRes as? DomainResult.Success)?.data ?: emptyList()
        val latestDisb = disbursements.firstOrNull()

        val (recStatus, recNotes) = when {
            request.status == AffiliatePayoutRequestStatus.COMPLETED && latestDisb?.providerStatus == DisbursementProviderStatus.SUCCESS ->
                Pair(PayoutReconciliationStatus.CONSISTENT, notes ?: "Internal COMPLETED status matches provider SUCCESS status.")

            request.status == AffiliatePayoutRequestStatus.REVERSED ->
                Pair(PayoutReconciliationStatus.CONSISTENT, notes ?: "Payout is REVERSED with compensating ledger credit.")

            request.status == AffiliatePayoutRequestStatus.FAILED && latestDisb?.providerStatus == DisbursementProviderStatus.FAILED ->
                Pair(PayoutReconciliationStatus.CONSISTENT, notes ?: "Internal FAILED status matches provider FAILED status.")

            request.status == AffiliatePayoutRequestStatus.PROCESSING && latestDisb?.providerStatus == DisbursementProviderStatus.SUCCESS -> {
                // Reconcile: complete request
                ledgerService.debitWallet(
                    tenantId = tenantId,
                    walletId = request.walletId,
                    amount = request.requestedAmount,
                    referenceId = request.payoutReference,
                    idempotencyKey = null,
                    reason = "Reconciled debit",
                    actorId = actorId
                )
                if (!request.reservationHoldId.isNullOrBlank()) {
                    holdService.releaseHold(tenantId, request.walletId, request.reservationHoldId, "Reconciled hold release", actorId)
                }
                payoutRequestRepository.updatePayoutRequestStatus(request.copy(status = AffiliatePayoutRequestStatus.COMPLETED))
                Pair(PayoutReconciliationStatus.MISMATCH_RESOLVED, notes ?: "Resolved: Marked COMPLETED following provider SUCCESS.")
            }

            request.status == AffiliatePayoutRequestStatus.PROCESSING && latestDisb?.providerStatus == DisbursementProviderStatus.FAILED -> {
                payoutRequestRepository.updatePayoutRequestStatus(request.copy(status = AffiliatePayoutRequestStatus.FAILED))
                Pair(PayoutReconciliationStatus.MISMATCH_RESOLVED, notes ?: "Resolved: Marked FAILED following provider FAILED.")
            }

            else -> Pair(PayoutReconciliationStatus.INVESTIGATION_REQUIRED, notes ?: "Discrepancy detected between internal state '${request.status.name}' and provider state '${latestDisb?.providerStatus?.name ?: "N/A"}'.")
        }

        val record = AffiliatePayoutReconciliationRecord(
            reconciliationId = "REC-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = tenantId,
            requestId = requestId,
            walletId = request.walletId,
            internalStatus = request.status,
            providerStatus = latestDisb?.providerStatus,
            providerTransactionRef = latestDisb?.providerTransactionRef,
            ledgerEntryId = latestDisb?.ledgerEntryId,
            reconciliationStatus = recStatus,
            reconciliationNotes = recNotes,
            reconciledBy = actorId,
            reconciledAt = System.currentTimeMillis()
        )

        return recoveryRepository.saveReconciliationRecord(record)
    }

    override suspend fun listReversalsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutReversalRecord>> {
        return recoveryRepository.listReversalsForRequest(tenantId, requestId)
    }

    override suspend fun listReconciliationsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutReconciliationRecord>> {
        return recoveryRepository.listReconciliationsForRequest(tenantId, requestId)
    }
}
