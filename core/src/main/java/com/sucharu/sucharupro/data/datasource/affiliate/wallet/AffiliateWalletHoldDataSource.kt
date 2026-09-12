package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHold

/**
 * Data Source interface for Affiliate Wallet Hold persistence (Module 23 Step 04).
 */
interface AffiliateWalletHoldDataSource {
    suspend fun saveHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold>
    suspend fun getHoldById(tenantId: String, holdId: String): DomainResult<AffiliateWalletHold?>
    suspend fun listHoldsForWallet(tenantId: String, walletId: String, activeOnly: Boolean = true): DomainResult<List<AffiliateWalletHold>>
    suspend fun updateHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold>
}
