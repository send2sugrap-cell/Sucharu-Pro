package com.sucharu.sucharupro.domain.model.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Provider status classifications for external financial disbursement attempts (Module 23 Step 07).
 */
enum class DisbursementProviderStatus {
    SUCCESS,
    FAILED,
    PENDING,
    UNKNOWN
}

/**
 * Provider execution response payload from disbursement gateway boundary.
 */
data class DisbursementProviderResult(
    val providerName: String,
    val providerTransactionRef: String?,
    val providerStatus: DisbursementProviderStatus,
    val providerResponseCode: String? = null,
    val failureReason: String? = null,
    val processedAt: Long = System.currentTimeMillis()
)

/**
 * Canonical interface boundary for payment/banking disbursement gateway integration.
 */
interface AffiliatePayoutDisbursementProvider {
    val providerName: String
    suspend fun disbursePayout(
        tenantId: String,
        payoutRequest: AffiliatePayoutRequest
    ): DomainResult<DisbursementProviderResult>
}

/**
 * Standard Mock Disbursement Provider Adapter for software testing without live bank credentials.
 */
class MockAffiliatePayoutDisbursementAdapter(
    override val providerName: String = "MOCK_BANK_DISBURSEMENT_GATEWAY",
    private val shouldFail: Boolean = false,
    private val failureReason: String? = null
) : AffiliatePayoutDisbursementProvider {

    override suspend fun disbursePayout(
        tenantId: String,
        payoutRequest: AffiliatePayoutRequest
    ): DomainResult<DisbursementProviderResult> {
        val now = System.currentTimeMillis()
        if (shouldFail) {
            return DomainResult.Success(
                DisbursementProviderResult(
                    providerName = providerName,
                    providerTransactionRef = "TXN-FAIL-" + payoutRequest.payoutReference,
                    providerStatus = DisbursementProviderStatus.FAILED,
                    providerResponseCode = "ERR_ACCOUNT_RESTRICTED",
                    failureReason = failureReason ?: "Destination bank account is restricted or invalid.",
                    processedAt = now
                )
            )
        }

        return DomainResult.Success(
            DisbursementProviderResult(
                providerName = providerName,
                providerTransactionRef = "TXN-SETTLED-" + payoutRequest.payoutReference,
                providerStatus = DisbursementProviderStatus.SUCCESS,
                providerResponseCode = "200_SETTLED",
                failureReason = null,
                processedAt = now
            )
        )
    }
}

/**
 * Authoritative record of a disbursement execution attempt.
 */
data class AffiliatePayoutDisbursementRecord(
    val disbursementId: String,
    val tenantId: String,
    val requestId: String,
    val walletId: String,
    val affiliateId: String,
    val amount: Money,
    val currency: String = "BDT",
    val providerName: String,
    val providerTransactionRef: String?,
    val providerStatus: DisbursementProviderStatus,
    val providerResponseCode: String? = null,
    val failureReason: String? = null,
    val ledgerEntryId: String? = null,
    val processedAt: Long = System.currentTimeMillis()
)
