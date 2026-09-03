package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Datasource Interface for Affiliate Management.
 */
interface AffiliateDataSource {

    suspend fun saveAffiliate(profile: AffiliateProfile): AffiliateProfile

    suspend fun findById(tenantId: String, affiliateId: String): AffiliateProfile?

    suspend fun findByUserId(tenantId: String, userId: String): AffiliateProfile?

    suspend fun findByAffiliateCode(tenantId: String, affiliateCode: String): AffiliateProfile?

    suspend fun listAffiliates(
        tenantId: String,
        status: AffiliateStatus? = null,
        affiliateType: AffiliateType? = null
    ): List<AffiliateProfile>

    suspend fun saveEligibility(eligibility: AffiliateEligibility): AffiliateEligibility

    suspend fun findLatestEligibility(tenantId: String, affiliateId: String): AffiliateEligibility?

    suspend fun appendAuditRecord(record: AffiliateAuditRecord): AffiliateAuditRecord

    suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateAuditRecord>

    suspend fun findLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateAuditRecord?

    suspend fun appendOutboxEvent(event: AffiliateOutboxEvent): AffiliateOutboxEvent

    suspend fun listPendingOutboxEvents(tenantId: String): List<AffiliateOutboxEvent>

    suspend fun getGovernanceSummary(tenantId: String): AffiliateGovernanceSummary
}
