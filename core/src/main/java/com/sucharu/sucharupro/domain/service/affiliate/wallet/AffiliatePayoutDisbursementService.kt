package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutDisbursementRecord

/**
 * Domain Service interface for Affiliate Payout Disbursement (Module 23 Step 07).
 */
interface AffiliatePayoutDisbursementService {
    suspend fun processPayoutDisbursement(
        tenantId: String,
        requestId: String,
        actorId: String
    ): DomainResult<AffiliatePayoutDisbursementRecord>

    suspend fun listDisbursementsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutDisbursementRecord>>
}
