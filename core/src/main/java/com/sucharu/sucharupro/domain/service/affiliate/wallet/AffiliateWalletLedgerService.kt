package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletBalance
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerDirection
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntry

/**
 * Domain Service interface for Affiliate Wallet Ledger & Balance Operations (Module 23 Step 02).
 */
interface AffiliateWalletLedgerService {
    suspend fun creditWallet(
        tenantId: String,
        walletId: String,
        amount: Money,
        referenceId: String? = null,
        idempotencyKey: String? = null,
        reason: String? = null,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry>

    suspend fun debitWallet(
        tenantId: String,
        walletId: String,
        amount: Money,
        referenceId: String? = null,
        idempotencyKey: String? = null,
        reason: String? = null,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry>

    suspend fun reverseLedgerEntry(
        tenantId: String,
        walletId: String,
        originalEntryId: String,
        reason: String,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry>

    suspend fun adjustWalletBalance(
        tenantId: String,
        walletId: String,
        amount: Money,
        direction: AffiliateWalletLedgerDirection,
        reason: String,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry>

    suspend fun calculateWalletBalance(
        tenantId: String,
        walletId: String
    ): DomainResult<AffiliateWalletBalance>

    suspend fun listLedgerEntries(
        tenantId: String,
        walletId: String,
        limit: Int = 100
    ): DomainResult<List<AffiliateWalletLedgerEntry>>
}
