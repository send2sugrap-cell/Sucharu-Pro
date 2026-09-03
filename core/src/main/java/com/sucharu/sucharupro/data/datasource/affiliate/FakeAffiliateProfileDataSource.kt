package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Fake Data Source for Affiliate Profile & Verification Management.
 */
class FakeAffiliateProfileDataSource : AffiliateProfileDataSource {

    private val profiles = ConcurrentHashMap<String, AffiliateOperationalProfile>()
    private val verifications = ConcurrentHashMap<String, AffiliateVerificationRecord>()
    private val documents = ConcurrentHashMap<String, AffiliateDocumentReference>()
    private val auditRecords = ConcurrentHashMap<String, MutableList<AffiliateProfileAuditRecord>>()
    private val outboxEvents = ConcurrentHashMap<String, MutableList<AffiliateProfileOutboxEvent>>()

    private fun profileKey(tenantId: String, affiliateId: String) = "$tenantId:$affiliateId"
    private fun verificationKey(tenantId: String, verificationId: String) = "$tenantId:$verificationId"
    private fun documentKey(tenantId: String, documentId: String) = "$tenantId:$documentId"

    override suspend fun findProfileByAffiliateId(tenantId: String, affiliateId: String): AffiliateOperationalProfile? {
        val p = profiles[profileKey(tenantId, affiliateId)]
        return if (p != null && p.tenantId == tenantId) p else null
    }

    override suspend fun saveProfile(profile: AffiliateOperationalProfile): AffiliateOperationalProfile {
        profiles[profileKey(profile.tenantId, profile.affiliateId)] = profile
        return profile
    }

    override suspend fun listProfiles(tenantId: String, status: AffiliateProfileStatus?, query: String?): List<AffiliateOperationalProfile> {
        return profiles.values
            .filter { it.tenantId == tenantId }
            .filter { status == null || it.profileStatus == status }
            .filter {
                if (query.isNullOrBlank()) true
                else it.displayName.contains(query, ignoreCase = true) ||
                     (it.legalName?.contains(query, ignoreCase = true) == true) ||
                     (it.contactEmail?.contains(query, ignoreCase = true) == true)
            }
            .sortedByDescending { it.updatedAt }
    }

    override suspend fun findVerificationById(tenantId: String, verificationId: String): AffiliateVerificationRecord? {
        val v = verifications[verificationKey(tenantId, verificationId)]
        return if (v != null && v.tenantId == tenantId) v else null
    }

    override suspend fun listVerificationsByAffiliateId(tenantId: String, affiliateId: String): List<AffiliateVerificationRecord> {
        return verifications.values
            .filter { it.tenantId == tenantId && it.affiliateId == affiliateId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun saveVerification(record: AffiliateVerificationRecord): AffiliateVerificationRecord {
        verifications[verificationKey(record.tenantId, record.verificationId)] = record
        return record
    }

    override suspend fun findDocumentById(tenantId: String, documentId: String): AffiliateDocumentReference? {
        val d = documents[documentKey(tenantId, documentId)]
        return if (d != null && d.tenantId == tenantId) d else null
    }

    override suspend fun listDocumentsByAffiliateId(tenantId: String, affiliateId: String): List<AffiliateDocumentReference> {
        return documents.values
            .filter { it.tenantId == tenantId && it.affiliateId == affiliateId }
            .sortedByDescending { it.uploadedAt }
    }

    override suspend fun saveDocument(doc: AffiliateDocumentReference): AffiliateDocumentReference {
        documents[documentKey(doc.tenantId, doc.documentId)] = doc
        return doc
    }

    override suspend fun saveAuditRecord(record: AffiliateProfileAuditRecord): AffiliateProfileAuditRecord {
        val list = auditRecords.computeIfAbsent(profileKey(record.tenantId, record.affiliateId)) { mutableListOf() }
        synchronized(list) {
            list.add(record)
        }
        return record
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateProfileAuditRecord> {
        val list = auditRecords[profileKey(tenantId, affiliateId)] ?: return emptyList()
        return synchronized(list) { list.toList() }
    }

    override suspend fun getLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateProfileAuditRecord? {
        val list = auditRecords[profileKey(tenantId, affiliateId)] ?: return null
        return synchronized(list) { list.lastOrNull() }
    }

    override suspend fun saveOutboxEvent(event: AffiliateProfileOutboxEvent): AffiliateProfileOutboxEvent {
        val list = outboxEvents.computeIfAbsent(event.tenantId) { mutableListOf() }
        synchronized(list) {
            list.add(event)
        }
        return event
    }

    override suspend fun listPendingOutboxEvents(tenantId: String): List<AffiliateProfileOutboxEvent> {
        val list = outboxEvents[tenantId] ?: return emptyList()
        return synchronized(list) { list.filter { it.status == "PENDING" } }
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateProfileGovernanceSummary {
        val tenantProfiles = profiles.values.filter { it.tenantId == tenantId }
        val tenantVerifs = verifications.values.filter { it.tenantId == tenantId }
        val tenantDocs = documents.values.filter { it.tenantId == tenantId }

        return AffiliateProfileGovernanceSummary(
            tenantId = tenantId,
            totalProfiles = tenantProfiles.size.toLong(),
            verifiedProfiles = tenantProfiles.count { it.profileStatus == AffiliateProfileStatus.VERIFIED }.toLong(),
            pendingReviewProfiles = tenantProfiles.count { it.profileStatus in setOf(AffiliateProfileStatus.SUBMITTED, AffiliateProfileStatus.UNDER_REVIEW) }.toLong(),
            incompleteProfiles = tenantProfiles.count { it.profileStatus == AffiliateProfileStatus.INCOMPLETE }.toLong(),
            changesRequiredProfiles = tenantProfiles.count { it.profileStatus == AffiliateProfileStatus.CHANGES_REQUIRED }.toLong(),
            suspendedProfiles = tenantProfiles.count { it.profileStatus == AffiliateProfileStatus.SUSPENDED }.toLong(),
            totalVerifications = tenantVerifs.size.toLong(),
            verifiedVerifications = tenantVerifs.count { it.status == AffiliateVerificationStatus.VERIFIED }.toLong(),
            pendingVerifications = tenantVerifs.count { it.status in setOf(AffiliateVerificationStatus.SUBMITTED, AffiliateVerificationStatus.UNDER_REVIEW) }.toLong(),
            rejectedVerifications = tenantVerifs.count { it.status == AffiliateVerificationStatus.REJECTED }.toLong(),
            totalDocuments = tenantDocs.size.toLong(),
            verifiedDocuments = tenantDocs.count { it.status == AffiliateDocumentStatus.VERIFIED }.toLong()
        )
    }

    fun clear() {
        profiles.clear()
        verifications.clear()
        documents.clear()
        auditRecords.clear()
        outboxEvents.clear()
    }
}
