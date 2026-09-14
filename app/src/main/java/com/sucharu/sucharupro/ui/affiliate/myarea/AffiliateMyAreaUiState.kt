package com.sucharu.sucharupro.ui.affiliate.myarea

/**
 * Affiliate Profile Info Model for My Area.
 */
data class AffiliateProfileInfo(
    val affiliateId: String,
    val affiliateCode: String,
    val displayName: String,
    val partnerType: String,
    val primaryPhone: String,
    val email: String,
    val status: String = "ACTIVE",
    val referralLink: String = "https://sucharu.com/ref/APEX2026"
)

/**
 * Referral Center Summary Model for My Area.
 */
data class ReferralCenterSummary(
    val referralCode: String,
    val referralLink: String,
    val totalReferralsCount: Int,
    val activeReferredCustomersCount: Int,
    val totalReferredOrdersCount: Int,
    val recentReferrals: List<ReferralItem>
)

data class ReferralItem(
    val referralId: String,
    val customerMaskedName: String,
    val orderReference: String,
    val dateFormatted: String,
    val commissionEarnedFormatted: String,
    val statusLabel: String
)

/**
 * Performance & Commission Summary Model for My Area.
 */
data class PerformanceCommissionSummary(
    val totalEarnedFormatted: String,
    val pendingClearanceFormatted: String,
    val approvedCommissionFormatted: String,
    val paidDisbursedFormatted: String
)

/**
 * Module 23 Wallet & Payout Snapshot Model for My Area.
 */
data class WalletSnapshot(
    val walletId: String,
    val availableToWithdrawFormatted: String,
    val heldReserveFormatted: String,
    val totalDisbursedFormatted: String,
    val recentPayouts: List<PayoutItem>
)

data class PayoutItem(
    val payoutId: String,
    val requestedAmountFormatted: String,
    val payoutMethod: String,
    val dateFormatted: String,
    val statusLabel: String
)

/**
 * Campaign Offer Model for Affiliate My Area.
 */
data class AffiliateCampaignItem(
    val campaignId: String,
    val title: String,
    val description: String,
    val commissionRateLabel: String,
    val validUntilFormatted: String
)

/**
 * Top-Level Aggregate Presentation Container for Affiliate My Area.
 */
data class AffiliateMyAreaSummary(
    val profile: AffiliateProfileInfo,
    val referrals: ReferralCenterSummary,
    val performance: PerformanceCommissionSummary,
    val wallet: WalletSnapshot,
    val campaigns: List<AffiliateCampaignItem>,
    val unreadNotificationsCount: Int = 0
)

/**
 * Reactive Presentation UI State for Affiliate My Area.
 */
sealed interface AffiliateMyAreaUiState {
    object Loading : AffiliateMyAreaUiState
    data class Success(
        val summary: AffiliateMyAreaSummary,
        val isRefreshing: Boolean = false
    ) : AffiliateMyAreaUiState
    data class Error(val errorMessage: String) : AffiliateMyAreaUiState
    data class Empty(val message: String = "No Affiliate Partner Data Available") : AffiliateMyAreaUiState
}
