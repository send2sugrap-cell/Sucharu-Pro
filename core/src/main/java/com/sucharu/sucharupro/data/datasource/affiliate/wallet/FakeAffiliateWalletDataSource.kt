package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWallet
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory Fake Data Source for Affiliate Wallet testing.
 */
class FakeAffiliateWalletDataSource : AffiliateWalletDataSource {

    private val wallets = ConcurrentHashMap<String, AffiliateWallet>()

    override suspend fun saveWallet(wallet: AffiliateWallet): DomainResult<AffiliateWallet> {
        val key = "${wallet.tenantId}:${wallet.walletId}"
        wallets[key] = wallet
        return DomainResult.Success(wallet)
    }

    override suspend fun getWalletById(tenantId: String, walletId: String): DomainResult<AffiliateWallet?> {
        val key = "$tenantId:$walletId"
        return DomainResult.Success(wallets[key])
    }

    override suspend fun getWalletByAffiliateId(
        tenantId: String,
        affiliateId: String,
        currency: String
    ): DomainResult<AffiliateWallet?> {
        val found = wallets.values.find { w ->
            w.tenantId == tenantId && w.affiliateId == affiliateId && w.currency.equals(currency, ignoreCase = true)
        }
        return DomainResult.Success(found)
    }

    override suspend fun updateWalletStatus(
        tenantId: String,
        walletId: String,
        status: AffiliateWalletStatus
    ): DomainResult<AffiliateWallet> {
        val key = "$tenantId:$walletId"
        val existing = wallets[key]
            ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found.")

        val updated = existing.copy(
            status = status,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        wallets[key] = updated
        return DomainResult.Success(updated)
    }
}
