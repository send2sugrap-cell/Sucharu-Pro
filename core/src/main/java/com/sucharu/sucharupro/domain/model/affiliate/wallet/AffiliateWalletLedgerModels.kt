package com.sucharu.sucharupro.domain.model.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Categorization of financial transaction types for Affiliate Wallet Ledger (Module 23 Step 02).
 */
enum class AffiliateWalletLedgerEntryType {
    CREDIT,
    DEBIT,
    REVERSAL,
    ADJUSTMENT
}

/**
 * Financial direction of a ledger entry.
 */
enum class AffiliateWalletLedgerDirection {
    CREDIT,
    DEBIT
}

/**
 * Authoritative, immutable ledger entry tracking every financial mutation on an Affiliate Wallet.
 */
data class AffiliateWalletLedgerEntry(
    val entryId: String,
    val tenantId: String,
    val walletId: String,
    val affiliateId: String,
    val currency: String = "BDT",
    val entryType: AffiliateWalletLedgerEntryType,
    val direction: AffiliateWalletLedgerDirection,
    val amount: Money,
    val referenceId: String? = null,
    val idempotencyKey: String? = null,
    val reversalOfEntryId: String? = null,
    val reason: String? = null,
    val actorId: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Controlled/Derived financial balance state derived exclusively from authoritative ledger entries.
 */
data class AffiliateWalletBalance(
    val walletId: String,
    val tenantId: String,
    val affiliateId: String,
    val currency: String = "BDT",
    val currentBalance: Money = Money.ZERO,
    val availableBalance: Money = Money.ZERO,
    val pendingBalance: Money = Money.ZERO,
    val heldAmount: Money = Money.ZERO,
    val totalCredits: Money = Money.ZERO,
    val totalDebits: Money = Money.ZERO,
    val lastLedgerEntryId: String? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
