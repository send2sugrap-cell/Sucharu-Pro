package com.sucharu.sucharupro.data.repository.affiliate.wallet

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliateWalletLedgerDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntry
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletLedgerRepository

/**
 * Authoritative Repository implementation for Affiliate Wallet Ledger with validation.
 */
class AffiliateWalletLedgerRepositoryImpl(
    private val dataSource: AffiliateWalletLedgerDataSource
) : AffiliateWalletLedgerRepository {

    override suspend fun postLedgerEntry(entry: AffiliateWalletLedgerEntry): DomainResult<AffiliateWalletLedgerEntry> {
        if (entry.entryId.isBlank()) {
            return DomainResult.Error(message = "Ledger entry ID cannot be blank.")
        }
        if (entry.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (entry.walletId.isBlank()) {
            return DomainResult.Error(message = "Wallet ID cannot be blank.")
        }
        if (entry.amount.isZero() || entry.amount.isNegative()) {
            return DomainResult.Error(message = "Ledger entry amount must be positive. Provided: ${entry.amount.formatted()}")
        }
        if (entry.actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank.")
        }

        // Check idempotency if idempotencyKey is present
        if (!entry.idempotencyKey.isNullOrBlank()) {
            val existingRes = dataSource.getLedgerEntryByIdempotencyKey(entry.tenantId, entry.idempotencyKey)
            if (existingRes is DomainResult.Success && existingRes.data != null) {
                return DomainResult.Success(existingRes.data)
            }
        }

        return dataSource.postLedgerEntry(entry)
    }

    override suspend fun getLedgerEntryById(
        tenantId: String,
        entryId: String
    ): DomainResult<AffiliateWalletLedgerEntry?> {
        if (tenantId.isBlank() || entryId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Ledger Entry ID cannot be blank.")
        }
        return dataSource.getLedgerEntryById(tenantId, entryId)
    }

    override suspend fun getLedgerEntryByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<AffiliateWalletLedgerEntry?> {
        if (tenantId.isBlank() || idempotencyKey.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Idempotency Key cannot be blank.")
        }
        return dataSource.getLedgerEntryByIdempotencyKey(tenantId, idempotencyKey)
    }

    override suspend fun listLedgerEntriesForWallet(
        tenantId: String,
        walletId: String,
        limit: Int
    ): DomainResult<List<AffiliateWalletLedgerEntry>> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        return dataSource.listLedgerEntriesForWallet(tenantId, walletId, limit)
    }

    override suspend fun getReversalForEntry(
        tenantId: String,
        originalEntryId: String
    ): DomainResult<AffiliateWalletLedgerEntry?> {
        if (tenantId.isBlank() || originalEntryId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Original Entry ID cannot be blank.")
        }
        return dataSource.getReversalForEntry(tenantId, originalEntryId)
    }
}
