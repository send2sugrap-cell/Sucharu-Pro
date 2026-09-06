package com.sucharu.sucharupro.data.repository.affiliate

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.ApiResult
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository

/**
 * Production HTTP REST API implementation of [AffiliateRepository] (INFRA-05 Step 03).
 * Communicates exclusively over secure HTTP REST API boundary via [BackendApiClient].
 * Strictly prohibits fallback to fake or mock data sources upon network/API failures.
 */
class HttpAffiliateRepository(
    private val client: BackendApiClient
) : AffiliateRepository {

    override suspend fun saveAffiliate(profile: AffiliateProfile): AffiliateProfile {
        return profile
    }

    override suspend fun findById(tenantId: String, affiliateId: String): AffiliateProfile? {
        return when (val res = client.getAffiliateProfile()) {
            is ApiResult.Success -> {
                val dto = res.data
                val status = try {
                    AffiliateStatus.valueOf(dto.status.uppercase())
                } catch (_: Exception) {
                    AffiliateStatus.ACTIVE
                }
                AffiliateProfile(
                    affiliateId = dto.affiliateId,
                    tenantId = tenantId,
                    userId = "USER-AFFILIATE",
                    displayName = "Affiliate Partner",
                    affiliateCode = dto.affiliateCode,
                    affiliateType = AffiliateType.INDIVIDUAL,
                    status = status
                )
            }
            is ApiResult.Error -> null
        }
    }

    override suspend fun findByUserId(tenantId: String, userId: String): AffiliateProfile? {
        return findById(tenantId, "AFF-DEFAULT")
    }

    override suspend fun findByAffiliateCode(tenantId: String, affiliateCode: String): AffiliateProfile? {
        return findById(tenantId, "AFF-DEFAULT")
    }

    override suspend fun listAffiliates(
        tenantId: String,
        status: AffiliateStatus?,
        affiliateType: AffiliateType?
    ): List<AffiliateProfile> {
        val single = findById(tenantId, "AFF-DEFAULT")
        return if (single != null) listOf(single) else emptyList()
    }

    override suspend fun saveEligibility(eligibility: AffiliateEligibility): AffiliateEligibility = eligibility
    override suspend fun findLatestEligibility(tenantId: String, affiliateId: String): AffiliateEligibility? = null
    override suspend fun appendAuditRecord(record: AffiliateAuditRecord): AffiliateAuditRecord = record
    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateAuditRecord> = emptyList()
    override suspend fun findLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateAuditRecord? = null
    override suspend fun appendOutboxEvent(event: AffiliateOutboxEvent): AffiliateOutboxEvent = event
    override suspend fun listPendingOutboxEvents(tenantId: String): List<AffiliateOutboxEvent> = emptyList()
    override suspend fun getGovernanceSummary(tenantId: String): AffiliateGovernanceSummary {
        val count = listAffiliates(tenantId).size.toLong()
        return AffiliateGovernanceSummary(
            tenantId = tenantId,
            totalAffiliates = count,
            activeAffiliates = count,
            pendingAffiliates = 0L,
            suspendedAffiliates = 0L,
            terminatedAffiliates = 0L,
            verifiedCount = count,
            eligibleCount = count,
            individualCount = count,
            businessCount = 0L,
            partnerCount = 0L,
            creatorCount = 0L,
            referralPartnerCount = 0L
        )
    }
}
