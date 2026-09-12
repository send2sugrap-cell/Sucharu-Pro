package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletHoldRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletRepository
import java.util.UUID

/**
 * Authoritative Domain Service implementation for Available Balance & Hold Governance operations.
 */
class AffiliateWalletHoldServiceImpl(
    private val walletRepository: AffiliateWalletRepository,
    private val holdRepository: AffiliateWalletHoldRepository,
    private val ledgerService: AffiliateWalletLedgerService
) : AffiliateWalletHoldService {

    override suspend fun createHold(
        tenantId: String,
        walletId: String,
        amount: Money,
        holdType: AffiliateWalletHoldType,
        reason: String,
        referenceId: String?,
        actorId: String
    ): DomainResult<AffiliateWalletHold> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        if (!amount.isPositive()) {
            return DomainResult.Error(message = "Hold amount must be positive. Provided: ${amount.formatted()}")
        }
        if (reason.isBlank()) {
            return DomainResult.Error(message = "Hold reason cannot be blank.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "CreatedBy actor ID cannot be blank.")
        }

        val walletRes = walletRepository.getWalletById(tenantId, walletId)
        if (walletRes is DomainResult.Error) return walletRes
        val wallet = (walletRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found.")

        if (wallet.status == AffiliateWalletStatus.CLOSED) {
            return DomainResult.Error(message = "Cannot place hold on closed wallet '$walletId'.")
        }

        // Verify available balance is sufficient for placing the hold
        val balanceRes = calculateAvailableBalance(tenantId, walletId)
        if (balanceRes is DomainResult.Error) return balanceRes
        val balance = (balanceRes as DomainResult.Success).data

        if (amount > balance.availableBalance) {
            return DomainResult.Error(
                message = "Cannot place hold of ${amount.formatted()}: Exceeds current available balance ${balance.availableBalance.formatted()}."
            )
        }

        val holdId = "HLD-" + UUID.randomUUID().toString().take(8).uppercase()
        val now = System.currentTimeMillis()
        val hold = AffiliateWalletHold(
            holdId = holdId,
            tenantId = tenantId,
            walletId = walletId,
            affiliateId = wallet.affiliateId,
            amount = amount,
            currency = wallet.currency,
            holdType = holdType,
            holdReason = reason,
            status = AffiliateWalletHoldStatus.ACTIVE,
            referenceId = referenceId,
            createdBy = actorId,
            createdAt = now
        )

        return holdRepository.saveHold(hold)
    }

    override suspend fun releaseHold(
        tenantId: String,
        walletId: String,
        holdId: String,
        releaseReason: String,
        actorId: String
    ): DomainResult<AffiliateWalletHold> {
        if (tenantId.isBlank() || walletId.isBlank() || holdId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID, Wallet ID, and Hold ID cannot be blank.")
        }
        if (releaseReason.isBlank()) {
            return DomainResult.Error(message = "Release reason cannot be blank.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "ReleasedBy actor ID cannot be blank.")
        }

        val holdRes = holdRepository.getHoldById(tenantId, holdId)
        if (holdRes is DomainResult.Error) return holdRes
        val hold = (holdRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Wallet hold '$holdId' not found.")

        if (hold.walletId != walletId) {
            return DomainResult.Error(message = "Wallet hold '$holdId' does not belong to wallet '$walletId'.")
        }

        if (hold.status != AffiliateWalletHoldStatus.ACTIVE) {
            return DomainResult.Error(message = "Wallet hold '$holdId' is already in state '${hold.status.name}' (cannot be released).")
        }

        val now = System.currentTimeMillis()
        val updatedHold = hold.copy(
            status = AffiliateWalletHoldStatus.RELEASED,
            releasedBy = actorId,
            releasedAt = now,
            releaseReason = releaseReason
        )

        return holdRepository.updateHold(updatedHold)
    }

    override suspend fun listHoldsForWallet(
        tenantId: String,
        walletId: String,
        activeOnly: Boolean
    ): DomainResult<List<AffiliateWalletHold>> {
        return holdRepository.listHoldsForWallet(tenantId, walletId, activeOnly)
    }

    override suspend fun calculateAvailableBalance(
        tenantId: String,
        walletId: String
    ): DomainResult<AffiliateWalletBalance> {
        val baseBalanceRes = ledgerService.calculateWalletBalance(tenantId, walletId)
        if (baseBalanceRes is DomainResult.Error) return baseBalanceRes
        val baseBalance = (baseBalanceRes as DomainResult.Success).data

        val activeHoldsRes = holdRepository.listHoldsForWallet(tenantId, walletId, activeOnly = true)
        if (activeHoldsRes is DomainResult.Error) return activeHoldsRes
        val activeHolds = (activeHoldsRes as? DomainResult.Success)?.data ?: emptyList()

        var totalHeld = Money.ZERO
        for (h in activeHolds) {
            totalHeld += h.amount
        }

        val available = if (baseBalance.currentBalance > totalHeld) baseBalance.currentBalance - totalHeld else Money.ZERO

        return DomainResult.Success(
            baseBalance.copy(
                heldAmount = totalHeld,
                availableBalance = available,
                lastUpdatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun evaluatePayoutEligibility(
        tenantId: String,
        walletId: String,
        requestedAmount: Money?,
        minimumThreshold: Money?
    ): DomainResult<AffiliatePayoutEligibility> {
        val walletRes = walletRepository.getWalletById(tenantId, walletId)
        if (walletRes is DomainResult.Error) return walletRes
        val wallet = (walletRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found.")

        val balanceRes = calculateAvailableBalance(tenantId, walletId)
        if (balanceRes is DomainResult.Error) return balanceRes
        val balance = (balanceRes as DomainResult.Success).data

        val blockingReasons = mutableListOf<String>()

        if (wallet.status != AffiliateWalletStatus.ACTIVE) {
            blockingReasons.add("Wallet is in state '${wallet.status.name}' (must be ACTIVE for payout)")
        }

        if (!balance.availableBalance.isPositive()) {
            blockingReasons.add("Available balance is ${balance.availableBalance.formatted()} (insufficient for payout)")
        }

        if (requestedAmount != null && requestedAmount > balance.availableBalance) {
            blockingReasons.add("Requested payout amount ${requestedAmount.formatted()} exceeds available balance ${balance.availableBalance.formatted()}")
        }

        val isThresholdSatisfied = minimumThreshold == null || balance.availableBalance >= minimumThreshold
        if (minimumThreshold != null && balance.availableBalance < minimumThreshold) {
            blockingReasons.add("Available balance ${balance.availableBalance.formatted()} is below minimum payout threshold ${minimumThreshold.formatted()}")
        }

        val isEligible = blockingReasons.isEmpty()

        return DomainResult.Success(
            AffiliatePayoutEligibility(
                walletId = walletId,
                tenantId = tenantId,
                affiliateId = wallet.affiliateId,
                currency = wallet.currency,
                isEligible = isEligible,
                currentBalance = balance.currentBalance,
                availableBalance = balance.availableBalance,
                pendingBalance = balance.pendingBalance,
                heldAmount = balance.heldAmount,
                minimumThreshold = minimumThreshold,
                isThresholdSatisfied = isThresholdSatisfied,
                blockingReasons = blockingReasons,
                evaluatedAt = System.currentTimeMillis()
            )
        )
    }
}
