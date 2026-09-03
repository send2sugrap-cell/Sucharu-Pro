package com.sucharu.sucharupro.data.repository.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.AffiliateDataSource
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository

/**
 * Implementation of AffiliateRepository delegating to AffiliateDataSource.
 */
class AffiliateRepositoryImpl(
    private val dataSource: AffiliateDataSource
) : AffiliateRepository {

    override suspend fun saveAffiliate(profile: AffiliateProfile): AffiliateProfile {
        return dataSource.saveAffiliate(profile)
    }

    override suspend fun findById(tenantId: String, affiliateId: String): AffiliateProfile? {
        return dataSource.findById(tenantId, affiliateId)
    }

    override suspend fun findByUserId(tenantId: String, userId: String): AffiliateProfile? {
        return dataSource.findByUserId(tenantId, userId)
    }

    override suspend fun findByAffiliateCode(tenantId: String, affiliateCode: String): AffiliateProfile? {
        return dataSource.findByAffiliateCode(tenantId, affiliateCode)
    }

    override suspend fun listAffiliates(
        tenantId: String,
        status: AffiliateStatus?,
        affiliateType: AffiliateType?
    ): List<AffiliateProfile> {
        return dataSource.listAffiliates(tenantId, status, affiliateType)
    }

    override suspend fun saveEligibility(eligibility: AffiliateEligibility): AffiliateEligibility {
        return dataSource.saveEligibility(eligibility)
    }

    override suspend fun findLatestEligibility(tenantId: String, affiliateId: String): AffiliateEligibility? {
        return dataSource.findLatestEligibility(tenantId, affiliateId)
    }

    override suspend fun appendAuditRecord(record: AffiliateAuditRecord): AffiliateAuditRecord {
        return dataSource.appendAuditRecord(record)
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateAuditRecord> {
        return dataSource.listAuditRecords(tenantId, affiliateId)
    }

    override suspend fun findLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateAuditRecord? {
        return dataSource.findLatestAuditRecord(tenantId, affiliateId)
    }

    override suspend fun appendOutboxEvent(event: AffiliateOutboxEvent): AffiliateOutboxEvent {
        return dataSource.appendOutboxEvent(event)
    }

    override suspend fun listPendingOutboxEvents(tenantId: String): List<AffiliateOutboxEvent> {
        return dataSource.listPendingOutboxEvents(tenantId)
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateGovernanceSummary {
        return dataSource.getGovernanceSummary(tenantId)
    }
}
