package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*

/**
 * Domain Service interface for Available Balance & Hold Governance (Module 23 Step 04).
 */
interface AffiliateWalletHoldService {
    suspend fun createHold(
        tenantId: String,
        walletId: String,
        amount: Money,
        holdType: AffiliateWalletHoldType,
        reason: String,
        referenceId: String? = null,
        actorId: String
    ): DomainResult<AffiliateWalletHold>

    suspend fun releaseHold(
        tenantId: String,
        walletId: String,
        holdId: String,
        releaseReason: String,
        actorId: String
    ): DomainResult<AffiliateWalletHold>

    suspend fun listHoldsForWallet(
        tenantId: String,
        walletId: String,
        activeOnly: Boolean = true
    ): DomainResult<List<AffiliateWalletHold>>

    suspend fun calculateAvailableBalance(
        tenantId: String,
        walletId: String
    ): DomainResult<AffiliateWalletBalance>

    suspend fun evaluatePayoutEligibility(
        tenantId: String,
        walletId: String,
        requestedAmount: Money? = null,
        minimumThreshold: Money? = null
    ): DomainResult<AffiliatePayoutEligibility>
}
