package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWallet
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus

/**
 * Data Source interface for Affiliate Wallet persistence (Module 23 Step 01).
 */
interface AffiliateWalletDataSource {
    suspend fun saveWallet(wallet: AffiliateWallet): DomainResult<AffiliateWallet>
    suspend fun getWalletById(tenantId: String, walletId: String): DomainResult<AffiliateWallet?>
    suspend fun getWalletByAffiliateId(tenantId: String, affiliateId: String, currency: String = "BDT"): DomainResult<AffiliateWallet?>
    suspend fun updateWalletStatus(tenantId: String, walletId: String, status: AffiliateWalletStatus): DomainResult<AffiliateWallet>
}
