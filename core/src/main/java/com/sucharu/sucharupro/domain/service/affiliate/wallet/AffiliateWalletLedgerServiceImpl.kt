package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletLedgerRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletRepository
import java.util.UUID

/**
 * Authoritative Domain Service implementation for Affiliate Wallet Ledger & Balance operations.
 */
class AffiliateWalletLedgerServiceImpl(
    private val walletRepository: AffiliateWalletRepository,
    private val ledgerRepository: AffiliateWalletLedgerRepository
) : AffiliateWalletLedgerService {

    override suspend fun creditWallet(
        tenantId: String,
        walletId: String,
        amount: Money,
        referenceId: String?,
        idempotencyKey: String?,
        reason: String?,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        if (amount.isZero() || amount.isNegative()) {
            return DomainResult.Error(message = "Credit amount must be positive. Provided: ${amount.formatted()}")
        }

        val walletRes = walletRepository.getWalletById(tenantId, walletId)
        if (walletRes is DomainResult.Error) return walletRes
        val wallet = (walletRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found.")

        if (wallet.status != AffiliateWalletStatus.ACTIVE) {
            return DomainResult.Error(message = "Cannot credit wallet '$walletId' in state '${wallet.status.name}'.")
        }

        val entryId = "LDR-" + UUID.randomUUID().toString().take(8).uppercase()
        val entry = AffiliateWalletLedgerEntry(
            entryId = entryId,
            tenantId = tenantId,
            walletId = walletId,
            affiliateId = wallet.affiliateId,
            currency = wallet.currency,
            entryType = AffiliateWalletLedgerEntryType.CREDIT,
            direction = AffiliateWalletLedgerDirection.CREDIT,
            amount = amount,
            referenceId = referenceId,
            idempotencyKey = idempotencyKey,
            reason = reason,
            actorId = actorId,
            createdAt = System.currentTimeMillis()
        )

        return ledgerRepository.postLedgerEntry(entry)
    }

    override suspend fun debitWallet(
        tenantId: String,
        walletId: String,
        amount: Money,
        referenceId: String?,
        idempotencyKey: String?,
        reason: String?,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        if (amount.isZero() || amount.isNegative()) {
            return DomainResult.Error(message = "Debit amount must be positive. Provided: ${amount.formatted()}")
        }

        val walletRes = walletRepository.getWalletById(tenantId, walletId)
        if (walletRes is DomainResult.Error) return walletRes
        val wallet = (walletRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found.")

        if (wallet.status != AffiliateWalletStatus.ACTIVE) {
            return DomainResult.Error(message = "Cannot debit wallet '$walletId' in state '${wallet.status.name}'.")
        }

        // Check current available balance
        val balanceRes = calculateWalletBalance(tenantId, walletId)
        if (balanceRes is DomainResult.Error) return balanceRes
        val balance = (balanceRes as DomainResult.Success).data

        if (amount > balance.availableBalance) {
            return DomainResult.Error(
                message = "Insufficient available balance for debit. Available: ${balance.availableBalance.formatted()}, Requested: ${amount.formatted()}"
            )
        }

        val entryId = "LDR-" + UUID.randomUUID().toString().take(8).uppercase()
        val entry = AffiliateWalletLedgerEntry(
            entryId = entryId,
            tenantId = tenantId,
            walletId = walletId,
            affiliateId = wallet.affiliateId,
            currency = wallet.currency,
            entryType = AffiliateWalletLedgerEntryType.DEBIT,
            direction = AffiliateWalletLedgerDirection.DEBIT,
            amount = amount,
            referenceId = referenceId,
            idempotencyKey = idempotencyKey,
            reason = reason,
            actorId = actorId,
            createdAt = System.currentTimeMillis()
        )

        return ledgerRepository.postLedgerEntry(entry)
    }

    override suspend fun reverseLedgerEntry(
        tenantId: String,
        walletId: String,
        originalEntryId: String,
        reason: String,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry> {
        if (reason.isBlank()) {
            return DomainResult.Error(message = "Reversal reason cannot be blank.")
        }

        val originalRes = ledgerRepository.getLedgerEntryById(tenantId, originalEntryId)
        if (originalRes is DomainResult.Error) return originalRes
        val original = (originalRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Ledger entry '$originalEntryId' not found.")

        if (original.walletId != walletId) {
            return DomainResult.Error(message = "Ledger entry '$originalEntryId' does not belong to wallet '$walletId'.")
        }

        // Check if already reversed
        val existingReversalRes = ledgerRepository.getReversalForEntry(tenantId, originalEntryId)
        if (existingReversalRes is DomainResult.Error) return existingReversalRes
        val existingReversal = (existingReversalRes as? DomainResult.Success)?.data
        if (existingReversal != null) {
            return DomainResult.Error(message = "Ledger entry '$originalEntryId' has already been reversed by entry '${existingReversal.entryId}'.")
        }

        val reversalDirection = if (original.direction == AffiliateWalletLedgerDirection.CREDIT) {
            AffiliateWalletLedgerDirection.DEBIT
        } else {
            AffiliateWalletLedgerDirection.CREDIT
        }

        // If reversal direction is DEBIT, verify sufficient balance
        if (reversalDirection == AffiliateWalletLedgerDirection.DEBIT) {
            val balanceRes = calculateWalletBalance(tenantId, walletId)
            if (balanceRes is DomainResult.Error) return balanceRes
            val balance = (balanceRes as DomainResult.Success).data
            if (original.amount > balance.availableBalance) {
                return DomainResult.Error(
                    message = "Insufficient available balance to reverse credit entry. Available: ${balance.availableBalance.formatted()}, Required: ${original.amount.formatted()}"
                )
            }
        }

        val entryId = "LDR-" + UUID.randomUUID().toString().take(8).uppercase()
        val reversalEntry = AffiliateWalletLedgerEntry(
            entryId = entryId,
            tenantId = tenantId,
            walletId = walletId,
            affiliateId = original.affiliateId,
            currency = original.currency,
            entryType = AffiliateWalletLedgerEntryType.REVERSAL,
            direction = reversalDirection,
            amount = original.amount,
            reversalOfEntryId = original.entryId,
            reason = reason,
            actorId = actorId,
            createdAt = System.currentTimeMillis()
        )

        return ledgerRepository.postLedgerEntry(reversalEntry)
    }

    override suspend fun adjustWalletBalance(
        tenantId: String,
        walletId: String,
        amount: Money,
        direction: AffiliateWalletLedgerDirection,
        reason: String,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry> {
        if (reason.isBlank()) {
            return DomainResult.Error(message = "Adjustment reason cannot be blank.")
        }
        if (amount.isZero() || amount.isNegative()) {
            return DomainResult.Error(message = "Adjustment amount must be positive. Provided: ${amount.formatted()}")
        }

        val walletRes = walletRepository.getWalletById(tenantId, walletId)
        if (walletRes is DomainResult.Error) return walletRes
        val wallet = (walletRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found.")

        if (wallet.status == AffiliateWalletStatus.CLOSED) {
            return DomainResult.Error(message = "Cannot adjust closed wallet '$walletId'.")
        }

        if (direction == AffiliateWalletLedgerDirection.DEBIT) {
            val balanceRes = calculateWalletBalance(tenantId, walletId)
            if (balanceRes is DomainResult.Error) return balanceRes
            val balance = (balanceRes as DomainResult.Success).data
            if (amount > balance.availableBalance) {
                return DomainResult.Error(
                    message = "Insufficient available balance for debit adjustment. Available: ${balance.availableBalance.formatted()}, Requested: ${amount.formatted()}"
                )
            }
        }

        val entryId = "LDR-" + UUID.randomUUID().toString().take(8).uppercase()
        val entry = AffiliateWalletLedgerEntry(
            entryId = entryId,
            tenantId = tenantId,
            walletId = walletId,
            affiliateId = wallet.affiliateId,
            currency = wallet.currency,
            entryType = AffiliateWalletLedgerEntryType.ADJUSTMENT,
            direction = direction,
            amount = amount,
            reason = reason,
            actorId = actorId,
            createdAt = System.currentTimeMillis()
        )

        return ledgerRepository.postLedgerEntry(entry)
    }

    override suspend fun calculateWalletBalance(
        tenantId: String,
        walletId: String
    ): DomainResult<AffiliateWalletBalance> {
        val walletRes = walletRepository.getWalletById(tenantId, walletId)
        if (walletRes is DomainResult.Error) return walletRes
        val wallet = (walletRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found.")

        val entriesRes = ledgerRepository.listLedgerEntriesForWallet(tenantId, walletId, limit = 10000)
        if (entriesRes is DomainResult.Error) return entriesRes
        val entries = (entriesRes as? DomainResult.Success)?.data ?: emptyList()

        var totalCredits = Money.ZERO
        var totalDebits = Money.ZERO

        for (e in entries) {
            when (e.direction) {
                AffiliateWalletLedgerDirection.CREDIT -> totalCredits += e.amount
                AffiliateWalletLedgerDirection.DEBIT -> totalDebits += e.amount
            }
        }

        val currentBalance = if (totalCredits >= totalDebits) totalCredits - totalDebits else Money.ZERO
        val availableBalance = currentBalance // In Step 02, pending and held amounts are ZERO unless specified

        return DomainResult.Success(
            AffiliateWalletBalance(
                walletId = walletId,
                tenantId = tenantId,
                affiliateId = wallet.affiliateId,
                currency = wallet.currency,
                currentBalance = currentBalance,
                availableBalance = availableBalance,
                pendingBalance = Money.ZERO,
                heldAmount = Money.ZERO,
                totalCredits = totalCredits,
                totalDebits = totalDebits,
                lastLedgerEntryId = entries.lastOrNull()?.entryId,
                lastUpdatedAt = entries.lastOrNull()?.createdAt ?: wallet.updatedAt
            )
        )
    }

    override suspend fun listLedgerEntries(
        tenantId: String,
        walletId: String,
        limit: Int
    ): DomainResult<List<AffiliateWalletLedgerEntry>> {
        return ledgerRepository.listLedgerEntriesForWallet(tenantId, walletId, limit)
    }
}
