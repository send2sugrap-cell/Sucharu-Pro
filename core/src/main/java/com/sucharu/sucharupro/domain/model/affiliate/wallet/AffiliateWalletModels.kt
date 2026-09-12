package com.sucharu.sucharupro.domain.model.affiliate.wallet

/**
 * Operational status lifecycle for an Affiliate Wallet (Module 23 Step 01).
 */
enum class AffiliateWalletStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED;

    val isOperational: Boolean get() = this == ACTIVE
}

/**
 * Authoritative Affiliate Wallet Foundation domain entity.
 */
data class AffiliateWallet(
    val walletId: String,
    val tenantId: String,
    val affiliateId: String,
    val currency: String = "BDT",
    val status: AffiliateWalletStatus = AffiliateWalletStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)
