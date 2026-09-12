package com.sucharu.sucharupro.domain.model.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Operational status lifecycle for a Wallet Hold (Module 23 Step 04).
 */
enum class AffiliateWalletHoldStatus {
    ACTIVE,
    RELEASED,
    CANCELLED
}

/**
 * Business classifications for wallet holds and payout reservations.
 */
enum class AffiliateWalletHoldType {
    DISPUTE_HOLD,
    FRAUD_RISK_REVIEW,
    COMPLIANCE_REVIEW,
    PAYOUT_RESERVATION,
    BUSINESS_POLICY_HOLD
}

/**
 * Authoritative entity representing an active or historical restriction on wallet funds.
 */
data class AffiliateWalletHold(
    val holdId: String,
    val tenantId: String,
    val walletId: String,
    val affiliateId: String,
    val amount: Money,
    val currency: String = "BDT",
    val holdType: AffiliateWalletHoldType = AffiliateWalletHoldType.BUSINESS_POLICY_HOLD,
    val holdReason: String,
    val status: AffiliateWalletHoldStatus = AffiliateWalletHoldStatus.ACTIVE,
    val referenceId: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val releasedBy: String? = null,
    val releasedAt: Long? = null,
    val releaseReason: String? = null
)

/**
 * Deterministic Payout Eligibility Decision entity.
 */
data class AffiliatePayoutEligibility(
    val walletId: String,
    val tenantId: String,
    val affiliateId: String,
    val currency: String = "BDT",
    val isEligible: Boolean,
    val currentBalance: Money,
    val availableBalance: Money,
    val pendingBalance: Money,
    val heldAmount: Money,
    val minimumThreshold: Money?,
    val isThresholdSatisfied: Boolean,
    val blockingReasons: List<String> = emptyList(),
    val evaluatedAt: Long = System.currentTimeMillis()
)
