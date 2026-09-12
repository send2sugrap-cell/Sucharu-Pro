package com.sucharu.sucharupro.domain.repository.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHold

/**
 * Domain Repository interface for Affiliate Wallet Holds & Restrictions (Module 23 Step 04).
 */
interface AffiliateWalletHoldRepository {
    suspend fun saveHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold>
    suspend fun getHoldById(tenantId: String, holdId: String): DomainResult<AffiliateWalletHold?>
    suspend fun listHoldsForWallet(tenantId: String, walletId: String, activeOnly: Boolean = true): DomainResult<List<AffiliateWalletHold>>
    suspend fun updateHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold>
}
