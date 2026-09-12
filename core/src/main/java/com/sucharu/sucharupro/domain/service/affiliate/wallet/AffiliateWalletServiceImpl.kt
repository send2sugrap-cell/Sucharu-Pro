package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWallet
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletRepository
import java.util.UUID

/**
 * Authoritative Domain Service implementation for Affiliate Wallet Foundation operations.
 */
class AffiliateWalletServiceImpl(
    private val walletRepository: AffiliateWalletRepository,
    private val affiliateRepository: AffiliateRepository
) : AffiliateWalletService {

    override suspend fun getOrCreateWallet(
        tenantId: String,
        affiliateId: String,
        currency: String,
        actorId: String
    ): DomainResult<AffiliateWallet> {
        if (tenantId.isBlank() || affiliateId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Affiliate ID cannot be blank.")
        }

        // 1. Verify canonical Affiliate identity in Module 20
        val affiliate = affiliateRepository.findById(tenantId, affiliateId)
            ?: return DomainResult.Error(message = "Affiliate '$affiliateId' not found in Module 20.")

        // 2. Check existing wallet
        val existingRes = walletRepository.getWalletByAffiliateId(tenantId, affiliateId, currency)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as? DomainResult.Success)?.data
        if (existing != null) {
            return DomainResult.Success(existing)
        }

        // 3. Create new wallet
        val walletId = "WLT-" + UUID.randomUUID().toString().take(8).uppercase()
        val now = System.currentTimeMillis()
        val newWallet = AffiliateWallet(
            walletId = walletId,
            tenantId = tenantId,
            affiliateId = affiliate.affiliateId,
            currency = currency.uppercase(),
            status = AffiliateWalletStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
            version = 1L
        )

        return walletRepository.saveWallet(newWallet)
    }

    override suspend fun getWalletDetails(
        tenantId: String,
        walletId: String
    ): DomainResult<AffiliateWallet?> {
        return walletRepository.getWalletById(tenantId, walletId)
    }

    override suspend fun getWalletByAffiliate(
        tenantId: String,
        affiliateId: String,
        currency: String
    ): DomainResult<AffiliateWallet?> {
        return walletRepository.getWalletByAffiliateId(tenantId, affiliateId, currency)
    }

    override suspend fun updateWalletStatus(
        tenantId: String,
        walletId: String,
        status: AffiliateWalletStatus,
        actorId: String
    ): DomainResult<AffiliateWallet> {
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank for wallet status update.")
        }
        return walletRepository.updateWalletStatus(tenantId, walletId, status)
    }
}
