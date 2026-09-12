package com.sucharu.sucharupro.domain.model.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Deterministic lifecycle status for an Affiliate Payout Request (Module 23 Step 05).
 */
enum class AffiliatePayoutRequestStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    PROCESSING,
    COMPLETED,
    REJECTED,
    FAILED,
    CANCELLED,
    REVERSED;

    val isTerminal: Boolean get() = this == COMPLETED || this == REJECTED || this == FAILED || this == CANCELLED || this == REVERSED
    val isPendingReview: Boolean get() = this == REQUESTED || this == UNDER_REVIEW
}

/**
 * Payout delivery channel classifications.
 */
enum class AffiliatePayoutMethodType {
    BANK_TRANSFER,
    MFS_BKASH,
    MFS_NAGAD,
    OTHER
}

/**
 * Authoritative entity representing an Affiliate Payout Request.
 */
data class AffiliatePayoutRequest(
    val requestId: String,
    val tenantId: String,
    val walletId: String,
    val affiliateId: String,
    val requestedAmount: Money,
    val currency: String = "BDT",
    val payoutMethodType: AffiliatePayoutMethodType,
    val payoutMethodAccountName: String,
    val payoutMethodAccountNumber: String,
    val payoutMethodProvider: String? = null,
    val payoutMethodBranchRouting: String? = null,
    val status: AffiliatePayoutRequestStatus = AffiliatePayoutRequestStatus.REQUESTED,
    val reservationHoldId: String? = null,
    val payoutReference: String,
    val idempotencyKey: String? = null,
    val rejectionReason: String? = null,
    val reviewNotes: String? = null,
    val requestedBy: String,
    val requestedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)
