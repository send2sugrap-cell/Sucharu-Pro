package com.sucharu.sucharupro.data.repository.affiliate.wallet

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliatePayoutDisbursementDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutDisbursementRecord
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutDisbursementRepository

/**
 * Authoritative Repository implementation for Affiliate Payout Disbursements with validation.
 */
class AffiliatePayoutDisbursementRepositoryImpl(
    private val dataSource: AffiliatePayoutDisbursementDataSource
) : AffiliatePayoutDisbursementRepository {

    override suspend fun saveDisbursementRecord(record: AffiliatePayoutDisbursementRecord): DomainResult<AffiliatePayoutDisbursementRecord> {
        if (record.disbursementId.isBlank()) {
            return DomainResult.Error(message = "Disbursement ID cannot be blank.")
        }
        if (record.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (record.requestId.isBlank()) {
            return DomainResult.Error(message = "Payout Request ID cannot be blank.")
        }
        if (record.providerName.isBlank()) {
            return DomainResult.Error(message = "Provider name cannot be blank.")
        }

        return dataSource.saveDisbursementRecord(record)
    }

    override suspend fun getDisbursementRecordById(
        tenantId: String,
        disbursementId: String
    ): DomainResult<AffiliatePayoutDisbursementRecord?> {
        if (tenantId.isBlank() || disbursementId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Disbursement ID cannot be blank.")
        }
        return dataSource.getDisbursementRecordById(tenantId, disbursementId)
    }

    override suspend fun listDisbursementsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutDisbursementRecord>> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        return dataSource.listDisbursementsForRequest(tenantId, requestId)
    }
}
