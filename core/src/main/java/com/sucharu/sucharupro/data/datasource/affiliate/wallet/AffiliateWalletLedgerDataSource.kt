package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntry

/**
 * Data Source interface for Affiliate Wallet Ledger persistence (Module 23 Step 02).
 */
interface AffiliateWalletLedgerDataSource {
    suspend fun postLedgerEntry(entry: AffiliateWalletLedgerEntry): DomainResult<AffiliateWalletLedgerEntry>
    suspend fun getLedgerEntryById(tenantId: String, entryId: String): DomainResult<AffiliateWalletLedgerEntry?>
    suspend fun getLedgerEntryByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<AffiliateWalletLedgerEntry?>
    suspend fun listLedgerEntriesForWallet(tenantId: String, walletId: String, limit: Int = 100): DomainResult<List<AffiliateWalletLedgerEntry>>
    suspend fun getReversalForEntry(tenantId: String, originalEntryId: String): DomainResult<AffiliateWalletLedgerEntry?>
}
