package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHold
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory Fake Data Source for Affiliate Wallet Hold testing.
 */
class FakeAffiliateWalletHoldDataSource : AffiliateWalletHoldDataSource {

    private val holds = ConcurrentHashMap<String, AffiliateWalletHold>()

    override suspend fun saveHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold> {
        val key = "${hold.tenantId}:${hold.holdId}"
        holds[key] = hold
        return DomainResult.Success(hold)
    }

    override suspend fun getHoldById(tenantId: String, holdId: String): DomainResult<AffiliateWalletHold?> {
        val key = "$tenantId:$holdId"
        return DomainResult.Success(holds[key])
    }

    override suspend fun listHoldsForWallet(
        tenantId: String,
        walletId: String,
        activeOnly: Boolean
    ): DomainResult<List<AffiliateWalletHold>> {
        val filtered = holds.values.filter { h ->
            h.tenantId == tenantId && h.walletId == walletId && (!activeOnly || h.status == AffiliateWalletHoldStatus.ACTIVE)
        }.sortedByDescending { it.createdAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun updateHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold> {
        val key = "${hold.tenantId}:${hold.holdId}"
        holds[key] = hold
        return DomainResult.Success(hold)
    }
}
