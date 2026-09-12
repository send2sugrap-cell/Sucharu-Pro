package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntry
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory Fake Data Source for Affiliate Wallet Ledger testing.
 */
class FakeAffiliateWalletLedgerDataSource : AffiliateWalletLedgerDataSource {

    private val ledgerEntries = ConcurrentHashMap<String, AffiliateWalletLedgerEntry>()

    override suspend fun postLedgerEntry(entry: AffiliateWalletLedgerEntry): DomainResult<AffiliateWalletLedgerEntry> {
        val key = "${entry.tenantId}:${entry.entryId}"
        ledgerEntries[key] = entry
        return DomainResult.Success(entry)
    }

    override suspend fun getLedgerEntryById(tenantId: String, entryId: String): DomainResult<AffiliateWalletLedgerEntry?> {
        val key = "$tenantId:$entryId"
        return DomainResult.Success(ledgerEntries[key])
    }

    override suspend fun getLedgerEntryByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<AffiliateWalletLedgerEntry?> {
        val found = ledgerEntries.values.find { e ->
            e.tenantId == tenantId && e.idempotencyKey == idempotencyKey
        }
        return DomainResult.Success(found)
    }

    override suspend fun listLedgerEntriesForWallet(
        tenantId: String,
        walletId: String,
        limit: Int
    ): DomainResult<List<AffiliateWalletLedgerEntry>> {
        val filtered = ledgerEntries.values.filter { e ->
            e.tenantId == tenantId && e.walletId == walletId
        }.sortedBy { it.createdAt }.take(limit)
        return DomainResult.Success(filtered)
    }

    override suspend fun getReversalForEntry(
        tenantId: String,
        originalEntryId: String
    ): DomainResult<AffiliateWalletLedgerEntry?> {
        val found = ledgerEntries.values.find { e ->
            e.tenantId == tenantId && e.reversalOfEntryId == originalEntryId
        }
        return DomainResult.Success(found)
    }
}
