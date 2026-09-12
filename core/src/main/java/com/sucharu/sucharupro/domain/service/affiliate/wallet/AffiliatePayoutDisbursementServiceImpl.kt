package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutDisbursementRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutRequestRepository
import java.util.UUID

/**
 * Authoritative Domain Service implementation for Affiliate Payout Disbursement operations (Module 23 Step 07).
 */
class AffiliatePayoutDisbursementServiceImpl(
    private val payoutRequestRepository: AffiliatePayoutRequestRepository,
    private val disbursementRepository: AffiliatePayoutDisbursementRepository,
    private val ledgerService: AffiliateWalletLedgerService,
    private val holdService: AffiliateWalletHoldService,
    private val disbursementProvider: AffiliatePayoutDisbursementProvider = MockAffiliatePayoutDisbursementAdapter()
) : AffiliatePayoutDisbursementService {

    override suspend fun processPayoutDisbursement(
        tenantId: String,
        requestId: String,
        actorId: String
    ): DomainResult<AffiliatePayoutDisbursementRecord> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank for disbursement processing.")
        }

        // 1. Fetch payout request
        val requestRes = payoutRequestRepository.getRequestById(tenantId, requestId)
        if (requestRes is DomainResult.Error) return requestRes
        val request = (requestRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Payout request '$requestId' not found.")

        // 2. Validate request status (must be APPROVED)
        if (request.status != AffiliatePayoutRequestStatus.APPROVED) {
            if (request.status == AffiliatePayoutRequestStatus.COMPLETED) {
                return DomainResult.Error(message = "Payout request '$requestId' is already COMPLETED.")
            }
            return DomainResult.Error(
                message = "Payout request '$requestId' is in state '${request.status.name}' (expected APPROVED for disbursement)."
            )
        }

        // 3. Transition status APPROVED -> PROCESSING
        val processingRequest = request.copy(
            status = AffiliatePayoutRequestStatus.PROCESSING,
            updatedAt = System.currentTimeMillis()
        )
        payoutRequestRepository.updatePayoutRequestStatus(processingRequest)

        // 4. Invoke disbursement gateway provider
        val providerRes = disbursementProvider.disbursePayout(tenantId, processingRequest)
        if (providerRes is DomainResult.Error) {
            val failRecord = AffiliatePayoutDisbursementRecord(
                disbursementId = "DISB-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = tenantId,
                requestId = requestId,
                walletId = request.walletId,
                affiliateId = request.affiliateId,
                amount = request.requestedAmount,
                currency = request.currency,
                providerName = disbursementProvider.providerName,
                providerTransactionRef = null,
                providerStatus = DisbursementProviderStatus.FAILED,
                failureReason = providerRes.message,
                processedAt = System.currentTimeMillis()
            )
            disbursementRepository.saveDisbursementRecord(failRecord)

            payoutRequestRepository.updatePayoutRequestStatus(
                processingRequest.copy(
                    status = AffiliatePayoutRequestStatus.FAILED,
                    rejectionReason = providerRes.message,
                    updatedAt = System.currentTimeMillis()
                )
            )
            return DomainResult.Error(message = "Payout disbursement failed: ${providerRes.message}")
        }

        val providerResult = (providerRes as DomainResult.Success).data

        // 5. Handle provider execution status
        return when (providerResult.providerStatus) {
            DisbursementProviderStatus.SUCCESS -> {
                // Post immutable DEBIT entry to Step 02 wallet ledger
                val debitRes = ledgerService.debitWallet(
                    tenantId = tenantId,
                    walletId = request.walletId,
                    amount = request.requestedAmount,
                    referenceId = request.payoutReference,
                    reason = "Payout settlement for reference ${request.payoutReference} (Txn: ${providerResult.providerTransactionRef ?: "N/A"})",
                    actorId = actorId
                )
                val ledgerEntryId = (debitRes as? DomainResult.Success)?.data?.entryId

                // Release payout reservation hold
                if (!request.reservationHoldId.isNullOrBlank()) {
                    holdService.releaseHold(
                        tenantId = tenantId,
                        walletId = request.walletId,
                        holdId = request.reservationHoldId,
                        releaseReason = "Payout settlement complete for $requestId",
                        actorId = actorId
                    )
                }

                // Update payout request status to COMPLETED
                payoutRequestRepository.updatePayoutRequestStatus(
                    processingRequest.copy(
                        status = AffiliatePayoutRequestStatus.COMPLETED,
                        reviewNotes = "Settled: ${providerResult.providerTransactionRef ?: "Success"}",
                        updatedAt = System.currentTimeMillis()
                    )
                )

                val record = AffiliatePayoutDisbursementRecord(
                    disbursementId = "DISB-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = tenantId,
                    requestId = requestId,
                    walletId = request.walletId,
                    affiliateId = request.affiliateId,
                    amount = request.requestedAmount,
                    currency = request.currency,
                    providerName = providerResult.providerName,
                    providerTransactionRef = providerResult.providerTransactionRef,
                    providerStatus = DisbursementProviderStatus.SUCCESS,
                    providerResponseCode = providerResult.providerResponseCode,
                    ledgerEntryId = ledgerEntryId,
                    processedAt = providerResult.processedAt
                )

                disbursementRepository.saveDisbursementRecord(record)
                DomainResult.Success(record)
            }

            DisbursementProviderStatus.FAILED -> {
                payoutRequestRepository.updatePayoutRequestStatus(
                    processingRequest.copy(
                        status = AffiliatePayoutRequestStatus.FAILED,
                        rejectionReason = providerResult.failureReason,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                val record = AffiliatePayoutDisbursementRecord(
                    disbursementId = "DISB-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = tenantId,
                    requestId = requestId,
                    walletId = request.walletId,
                    affiliateId = request.affiliateId,
                    amount = request.requestedAmount,
                    currency = request.currency,
                    providerName = providerResult.providerName,
                    providerTransactionRef = providerResult.providerTransactionRef,
                    providerStatus = DisbursementProviderStatus.FAILED,
                    providerResponseCode = providerResult.providerResponseCode,
                    failureReason = providerResult.failureReason,
                    processedAt = providerResult.processedAt
                )

                disbursementRepository.saveDisbursementRecord(record)
                DomainResult.Error(message = "Payout disbursement failed: ${providerResult.failureReason ?: "Provider error"}")
            }

            else -> {
                val record = AffiliatePayoutDisbursementRecord(
                    disbursementId = "DISB-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = tenantId,
                    requestId = requestId,
                    walletId = request.walletId,
                    affiliateId = request.affiliateId,
                    amount = request.requestedAmount,
                    currency = request.currency,
                    providerName = providerResult.providerName,
                    providerTransactionRef = providerResult.providerTransactionRef,
                    providerStatus = providerResult.providerStatus,
                    processedAt = providerResult.processedAt
                )

                disbursementRepository.saveDisbursementRecord(record)
                DomainResult.Error(message = "Payout disbursement status unknown / pending provider confirmation.")
            }
        }
    }

    override suspend fun listDisbursementsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutDisbursementRecord>> {
        return disbursementRepository.listDisbursementsForRequest(tenantId, requestId)
    }
}
