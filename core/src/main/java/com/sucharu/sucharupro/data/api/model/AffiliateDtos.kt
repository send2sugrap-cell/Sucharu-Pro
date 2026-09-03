package com.sucharu.sucharupro.data.api.model

import java.math.BigDecimal

/**
 * Legacy Affiliate profile presentation DTO (INFRA-02 Step 04).
 */
data class LegacyAffiliateProfileDto(
    val affiliateId: String,
    val affiliateCode: String,
    val name: String,
    val email: String,
    val tier: String,
    val commissionRatePercent: BigDecimal,
    val lifetimeEarnings: BigDecimal,
    val unpaidEarnings: BigDecimal,
    val status: String
)

/**
 * Affiliate referral event DTO.
 */
data class AffiliateReferralDto(
    val referralId: String,
    val referredCustomerId: String,
    val referredCustomerName: String,
    val orderId: String?,
    val orderTotal: BigDecimal,
    val commissionEarned: BigDecimal,
    val status: String,
    val createdAt: Long
)

/**
 * Affiliate commission summary DTO.
 */
data class AffiliateCommissionDto(
    val affiliateId: String,
    val totalReferralsCount: Int,
    val totalSalesVolume: BigDecimal,
    val totalCommissionEarned: BigDecimal,
    val pendingPayoutAmount: BigDecimal,
    val lastPayoutDate: Long?
)

/**
 * Affiliate payout transaction DTO.
 */
data class AffiliatePayoutDto(
    val payoutId: String,
    val amount: BigDecimal,
    val paymentMethod: String,
    val transactionReference: String?,
    val status: String,
    val paidAt: Long
)
