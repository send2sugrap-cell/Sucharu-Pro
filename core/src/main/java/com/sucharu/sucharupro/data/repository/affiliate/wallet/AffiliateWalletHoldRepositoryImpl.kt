package com.sucharu.sucharupro.data.repository.affiliate.wallet

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliateWalletHoldDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHold
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletHoldRepository

/**
 * Authoritative Repository implementation for Affiliate Wallet Holds with validation.
 */
class AffiliateWalletHoldRepositoryImpl(
    private val dataSource: AffiliateWalletHoldDataSource
) : AffiliateWalletHoldRepository {

    override suspend fun saveHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold> {
        if (hold.holdId.isBlank()) {
            return DomainResult.Error(message = "Hold ID cannot be blank.")
        }
        if (hold.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (hold.walletId.isBlank()) {
            return DomainResult.Error(message = "Wallet ID cannot be blank.")
        }
        if (!hold.amount.isPositive()) {
            return DomainResult.Error(message = "Hold amount must be positive. Provided: ${hold.amount.formatted()}")
        }
        if (hold.holdReason.isBlank()) {
            return DomainResult.Error(message = "Hold reason cannot be blank.")
        }
        if (hold.createdBy.isBlank()) {
            return DomainResult.Error(message = "CreatedBy actor ID cannot be blank.")
        }

        return dataSource.saveHold(hold)
    }

    override suspend fun getHoldById(tenantId: String, holdId: String): DomainResult<AffiliateWalletHold?> {
        if (tenantId.isBlank() || holdId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Hold ID cannot be blank.")
        }
        return dataSource.getHoldById(tenantId, holdId)
    }

    override suspend fun listHoldsForWallet(
        tenantId: String,
        walletId: String,
        activeOnly: Boolean
    ): DomainResult<List<AffiliateWalletHold>> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        return dataSource.listHoldsForWallet(tenantId, walletId, activeOnly)
    }

    override suspend fun updateHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold> {
        if (hold.tenantId.isBlank() || hold.holdId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Hold ID cannot be blank.")
        }
        return dataSource.updateHold(hold)
    }
}
