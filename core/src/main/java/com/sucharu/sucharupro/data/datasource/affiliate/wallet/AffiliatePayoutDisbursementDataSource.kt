package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutDisbursementRecord

/**
 * Data Source interface for Affiliate Payout Disbursement persistence (Module 23 Step 07).
 */
interface AffiliatePayoutDisbursementDataSource {
    suspend fun saveDisbursementRecord(record: AffiliatePayoutDisbursementRecord): DomainResult<AffiliatePayoutDisbursementRecord>
    suspend fun getDisbursementRecordById(tenantId: String, disbursementId: String): DomainResult<AffiliatePayoutDisbursementRecord?>
    suspend fun listDisbursementsForRequest(tenantId: String, requestId: String): DomainResult<List<AffiliatePayoutDisbursementRecord>>
}
