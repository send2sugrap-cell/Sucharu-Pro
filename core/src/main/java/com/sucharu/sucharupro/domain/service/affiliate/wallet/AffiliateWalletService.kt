package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWallet
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus

/**
 * Domain Service interface for Affiliate Wallet Foundation (Module 23 Step 01).
 */
interface AffiliateWalletService {
    suspend fun getOrCreateWallet(
        tenantId: String,
        affiliateId: String,
        currency: String = "BDT",
        actorId: String
    ): DomainResult<AffiliateWallet>

    suspend fun getWalletDetails(tenantId: String, walletId: String): DomainResult<AffiliateWallet?>
    suspend fun getWalletByAffiliate(tenantId: String, affiliateId: String, currency: String = "BDT"): DomainResult<AffiliateWallet?>
    suspend fun updateWalletStatus(
        tenantId: String,
        walletId: String,
        status: AffiliateWalletStatus,
        actorId: String
    ): DomainResult<AffiliateWallet>
}
