package com.sucharu.sucharupro.domain.model.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Reconciliation result status classifications for Affiliate Payouts (Module 23 Step 08).
 */
enum class PayoutReconciliationStatus {
    CONSISTENT,
    MISMATCH_RESOLVED,
    INVESTIGATION_REQUIRED
}

/**
 * Authoritative entity recording a payout reconciliation audit evaluation.
 */
data class AffiliatePayoutReconciliationRecord(
    val reconciliationId: String,
    val tenantId: String,
    val requestId: String,
    val walletId: String,
    val internalStatus: AffiliatePayoutRequestStatus,
    val providerStatus: DisbursementProviderStatus?,
    val providerTransactionRef: String?,
    val ledgerEntryId: String?,
    val reconciliationStatus: PayoutReconciliationStatus,
    val reconciliationNotes: String,
    val reconciledBy: String,
    val reconciledAt: Long = System.currentTimeMillis()
)

/**
 * Authoritative entity recording a payout reversal and compensating financial credit.
 */
data class AffiliatePayoutReversalRecord(
    val reversalId: String,
    val tenantId: String,
    val requestId: String,
    val walletId: String,
    val affiliateId: String,
    val reversedAmount: Money,
    val currency: String = "BDT",
    val originalLedgerEntryId: String?,
    val compensatingLedgerEntryId: String,
    val reversalReason: String,
    val reversedBy: String,
    val reversedAt: Long = System.currentTimeMillis()
)
