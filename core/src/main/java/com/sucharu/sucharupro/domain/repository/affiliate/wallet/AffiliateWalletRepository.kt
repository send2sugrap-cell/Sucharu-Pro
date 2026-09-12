package com.sucharu.sucharupro.domain.repository.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWallet
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus

/**
 * Domain Repository interface for Affiliate Wallet Foundation (Module 23 Step 01).
 */
interface AffiliateWalletRepository {
    suspend fun saveWallet(wallet: AffiliateWallet): DomainResult<AffiliateWallet>
    suspend fun getWalletById(tenantId: String, walletId: String): DomainResult<AffiliateWallet?>
    suspend fun getWalletByAffiliateId(tenantId: String, affiliateId: String, currency: String = "BDT"): DomainResult<AffiliateWallet?>
    suspend fun updateWalletStatus(tenantId: String, walletId: String, status: AffiliateWalletStatus): DomainResult<AffiliateWallet>
}
