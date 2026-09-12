package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletRepository

/**
 * Authoritative Domain Service implementation for Earning -> Wallet Credit Integration (Module 23 Step 03).
 * Consumes pre-calculated approved earning handoffs and credits the canonical Affiliate Wallet Ledger.
 * Module 23 DOES NOT calculate or alter the earning amount.
 */
class AffiliateEarningWalletIntegrationServiceImpl(
    private val affiliateRepository: AffiliateRepository,
    private val walletRepository: AffiliateWalletRepository,
    private val walletService: AffiliateWalletService,
    private val ledgerService: AffiliateWalletLedgerService
) : AffiliateEarningWalletIntegrationService {

    override suspend fun processApprovedEarningCredit(
        principalTenantId: String,
        handoff: ApprovedEarningHandoff,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry> {
        // 1. Tenant boundary validation
        if (principalTenantId.isBlank() || handoff.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (principalTenantId != handoff.tenantId) {
            return DomainResult.Error(
                message = "Cross-tenant earning credit rejected: Principal tenant '$principalTenantId' does not match handoff tenant '${handoff.tenantId}'."
            )
        }

        // 2. Input contract validation
        if (handoff.earningReferenceId.isBlank()) {
            return DomainResult.Error(message = "Earning reference ID cannot be blank.")
        }
        if (handoff.affiliateId.isBlank()) {
            return DomainResult.Error(message = "Affiliate ID cannot be blank.")
        }
        if (!handoff.amount.isPositive()) {
            return DomainResult.Error(message = "Approved earning amount must be positive. Provided: ${handoff.amount.formatted()}")
        }

        // 3. Resolve canonical Module 20 Affiliate identity
        val affiliate = affiliateRepository.findById(handoff.tenantId, handoff.affiliateId)
            ?: return DomainResult.Error(message = "Affiliate '${handoff.affiliateId}' not found in Module 20.")

        // 4. Resolve target Wallet
        val walletRes = walletService.getOrCreateWallet(
            tenantId = handoff.tenantId,
            affiliateId = affiliate.affiliateId,
            currency = handoff.currency,
            actorId = actorId
        )
        if (walletRes is DomainResult.Error) return walletRes
        val wallet = (walletRes as DomainResult.Success).data

        // 5. Currency match validation
        if (!wallet.currency.equals(handoff.currency, ignoreCase = true)) {
            return DomainResult.Error(
                message = "Currency mismatch: Approved earning currency '${handoff.currency}' does not match wallet currency '${wallet.currency}'. Currency conversion is not supported in Step 03."
            )
        }

        // 6. Wallet status validation
        if (wallet.status != AffiliateWalletStatus.ACTIVE) {
            return DomainResult.Error(
                message = "Cannot credit approved earning: Wallet '${wallet.walletId}' is in state '${wallet.status.name}' (must be ACTIVE)."
            )
        }

        // 7. Deterministic idempotency key
        val idempotencyKey = handoff.idempotencyKey
            ?: "EARN-CREDIT:${handoff.tenantId}:${handoff.earningReferenceId}:${wallet.walletId}"

        // 8. Execute wallet credit via Step 02 immutable ledger service
        return ledgerService.creditWallet(
            tenantId = handoff.tenantId,
            walletId = wallet.walletId,
            amount = handoff.amount,
            referenceId = handoff.earningReferenceId,
            idempotencyKey = idempotencyKey,
            reason = "Credit for approved earning '${handoff.earningReferenceId}' (Source: ${handoff.source})",
            actorId = actorId
        )
    }
}
