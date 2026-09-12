package com.sucharu.sucharupro.domain.repository.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutDisbursementRecord

/**
 * Domain Repository interface for Affiliate Payout Disbursements (Module 23 Step 07).
 */
interface AffiliatePayoutDisbursementRepository {
    suspend fun saveDisbursementRecord(record: AffiliatePayoutDisbursementRecord): DomainResult<AffiliatePayoutDisbursementRecord>
    suspend fun getDisbursementRecordById(tenantId: String, disbursementId: String): DomainResult<AffiliatePayoutDisbursementRecord?>
    suspend fun listDisbursementsForRequest(tenantId: String, requestId: String): DomainResult<List<AffiliatePayoutDisbursementRecord>>
}
