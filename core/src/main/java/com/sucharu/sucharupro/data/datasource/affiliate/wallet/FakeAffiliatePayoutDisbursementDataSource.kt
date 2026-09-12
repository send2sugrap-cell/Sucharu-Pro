package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutDisbursementRecord
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory Fake Data Source for Affiliate Payout Disbursement testing.
 */
class FakeAffiliatePayoutDisbursementDataSource : AffiliatePayoutDisbursementDataSource {

    private val records = ConcurrentHashMap<String, AffiliatePayoutDisbursementRecord>()

    override suspend fun saveDisbursementRecord(record: AffiliatePayoutDisbursementRecord): DomainResult<AffiliatePayoutDisbursementRecord> {
        val key = "${record.tenantId}:${record.disbursementId}"
        records[key] = record
        return DomainResult.Success(record)
    }

    override suspend fun getDisbursementRecordById(
        tenantId: String,
        disbursementId: String
    ): DomainResult<AffiliatePayoutDisbursementRecord?> {
        val key = "$tenantId:$disbursementId"
        return DomainResult.Success(records[key])
    }

    override suspend fun listDisbursementsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutDisbursementRecord>> {
        val filtered = records.values.filter { r ->
            r.tenantId == tenantId && r.requestId == requestId
        }.sortedByDescending { it.processedAt }
        return DomainResult.Success(filtered)
    }
}
