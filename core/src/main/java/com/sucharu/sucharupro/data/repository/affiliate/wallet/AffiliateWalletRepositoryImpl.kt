package com.sucharu.sucharupro.data.repository.affiliate.wallet

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliateWalletDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWallet
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletRepository

/**
 * Authoritative Repository implementation for Affiliate Wallet Foundation operations with validation.
 */
class AffiliateWalletRepositoryImpl(
    private val dataSource: AffiliateWalletDataSource
) : AffiliateWalletRepository {

    override suspend fun saveWallet(wallet: AffiliateWallet): DomainResult<AffiliateWallet> {
        if (wallet.walletId.isBlank()) {
            return DomainResult.Error(message = "Wallet ID cannot be blank.")
        }
        if (wallet.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (wallet.affiliateId.isBlank()) {
            return DomainResult.Error(message = "Affiliate ID cannot be blank.")
        }
        if (wallet.currency.isBlank()) {
            return DomainResult.Error(message = "Wallet currency cannot be blank.")
        }

        return dataSource.saveWallet(wallet)
    }

    override suspend fun getWalletById(tenantId: String, walletId: String): DomainResult<AffiliateWallet?> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        return dataSource.getWalletById(tenantId, walletId)
    }

    override suspend fun getWalletByAffiliateId(
        tenantId: String,
        affiliateId: String,
        currency: String
    ): DomainResult<AffiliateWallet?> {
        if (tenantId.isBlank() || affiliateId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Affiliate ID cannot be blank.")
        }
        return dataSource.getWalletByAffiliateId(tenantId, affiliateId, currency)
    }

    override suspend fun updateWalletStatus(
        tenantId: String,
        walletId: String,
        status: AffiliateWalletStatus
    ): DomainResult<AffiliateWallet> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        return dataSource.updateWalletStatus(tenantId, walletId, status)
    }
}
