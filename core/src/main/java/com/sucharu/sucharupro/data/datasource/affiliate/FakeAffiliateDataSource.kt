package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Fake Datasource for Unit & Security Testing.
 */
class FakeAffiliateDataSource : AffiliateDataSource {

    private val affiliatesMap = ConcurrentHashMap<String, AffiliateProfile>() // key: "$tenantId|$affiliateId"
    private val eligibilityMap = ConcurrentHashMap<String, MutableList<AffiliateEligibility>>() // key: "$tenantId|$affiliateId"
    private val auditMap = ConcurrentHashMap<String, MutableList<AffiliateAuditRecord>>() // key: "$tenantId|$affiliateId"
    private val outboxList = mutableListOf<AffiliateOutboxEvent>()
    private val lock = Any()

    override suspend fun saveAffiliate(profile: AffiliateProfile): AffiliateProfile {
        synchronized(lock) {
            // Enforce unique constraints within tenant
            affiliatesMap.values.forEach { existing ->
                if (existing.tenantId == profile.tenantId && existing.affiliateId != profile.affiliateId) {
                    if (existing.affiliateCode.equals(profile.affiliateCode, ignoreCase = true)) {
                        throw IllegalStateException("Affiliate code '${profile.affiliateCode}' already exists for tenant '${profile.tenantId}'.")
                    }
                    if (existing.userId == profile.userId) {
                        throw IllegalStateException("User ID '${profile.userId}' is already associated with an affiliate in tenant '${profile.tenantId}'.")
                    }
                }
            }
            val key = "${profile.tenantId}|${profile.affiliateId}"
            affiliatesMap[key] = profile
            return profile
        }
    }

    override suspend fun findById(tenantId: String, affiliateId: String): AffiliateProfile? {
        val key = "$tenantId|$affiliateId"
        return affiliatesMap[key]
    }

    override suspend fun findByUserId(tenantId: String, userId: String): AffiliateProfile? {
        return affiliatesMap.values.firstOrNull { it.tenantId == tenantId && it.userId == userId }
    }

    override suspend fun findByAffiliateCode(tenantId: String, affiliateCode: String): AffiliateProfile? {
        return affiliatesMap.values.firstOrNull {
            it.tenantId == tenantId && it.affiliateCode.equals(affiliateCode, ignoreCase = true)
        }
    }

    override suspend fun listAffiliates(
        tenantId: String,
        status: AffiliateStatus?,
        affiliateType: AffiliateType?
    ): List<AffiliateProfile> {
        return affiliatesMap.values
            .filter { it.tenantId == tenantId }
            .filter { status == null || it.status == status }
            .filter { affiliateType == null || it.affiliateType == affiliateType }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun saveEligibility(eligibility: AffiliateEligibility): AffiliateEligibility {
        synchronized(lock) {
            val key = "${eligibility.tenantId}|${eligibility.affiliateId}"
            val list = eligibilityMap.getOrPut(key) { mutableListOf() }
            list.add(eligibility)
            return eligibility
        }
    }

    override suspend fun findLatestEligibility(tenantId: String, affiliateId: String): AffiliateEligibility? {
        val key = "$tenantId|$affiliateId"
        return eligibilityMap[key]?.lastOrNull()
    }

    override suspend fun appendAuditRecord(record: AffiliateAuditRecord): AffiliateAuditRecord {
        synchronized(lock) {
            val key = "${record.tenantId}|${record.affiliateId}"
            val list = auditMap.getOrPut(key) { mutableListOf() }
            list.add(record)
            return record
        }
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateAuditRecord> {
        val key = "$tenantId|$affiliateId"
        return auditMap[key]?.sortedBy { it.timestamp } ?: emptyList()
    }

    override suspend fun findLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateAuditRecord? {
        val key = "$tenantId|$affiliateId"
        return auditMap[key]?.maxByOrNull { it.timestamp }
    }

    override suspend fun appendOutboxEvent(event: AffiliateOutboxEvent): AffiliateOutboxEvent {
        synchronized(lock) {
            outboxList.add(event)
            return event
        }
    }

    override suspend fun listPendingOutboxEvents(tenantId: String): List<AffiliateOutboxEvent> {
        synchronized(lock) {
            return outboxList.filter { it.tenantId == tenantId && it.status == "PENDING" }
        }
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateGovernanceSummary {
        val tenantAffiliates = affiliatesMap.values.filter { it.tenantId == tenantId }
        val active = tenantAffiliates.count { it.status == AffiliateStatus.ACTIVE }.toLong()
        val pending = tenantAffiliates.count { it.status == AffiliateStatus.PENDING }.toLong()
        val suspended = tenantAffiliates.count { it.status == AffiliateStatus.SUSPENDED }.toLong()
        val terminated = tenantAffiliates.count { it.status == AffiliateStatus.TERMINATED }.toLong()
        val verified = tenantAffiliates.count { it.verificationState == VerificationState.VERIFIED }.toLong()

        val eligible = tenantAffiliates.count { profile ->
            findLatestEligibility(tenantId, profile.affiliateId)?.isEligible == true
        }.toLong()

        return AffiliateGovernanceSummary(
            tenantId = tenantId,
            totalAffiliates = tenantAffiliates.size.toLong(),
            activeAffiliates = active,
            pendingAffiliates = pending,
            suspendedAffiliates = suspended,
            terminatedAffiliates = terminated,
            verifiedCount = verified,
            eligibleCount = eligible,
            individualCount = tenantAffiliates.count { it.affiliateType == AffiliateType.INDIVIDUAL }.toLong(),
            businessCount = tenantAffiliates.count { it.affiliateType == AffiliateType.BUSINESS }.toLong(),
            partnerCount = tenantAffiliates.count { it.affiliateType == AffiliateType.PARTNER }.toLong(),
            creatorCount = tenantAffiliates.count { it.affiliateType == AffiliateType.CREATOR }.toLong(),
            referralPartnerCount = tenantAffiliates.count { it.affiliateType == AffiliateType.REFERRAL_PARTNER }.toLong()
        )
    }

    fun clear() {
        synchronized(lock) {
            affiliatesMap.clear()
            eligibilityMap.clear()
            auditMap.clear()
            outboxList.clear()
        }
    }
}
