package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Data Source Interface for Affiliate Profiles, Verifications & Document References.
 */
interface AffiliateProfileDataSource {
    // Profiles
    suspend fun findProfileByAffiliateId(tenantId: String, affiliateId: String): AffiliateOperationalProfile?
    suspend fun saveProfile(profile: AffiliateOperationalProfile): AffiliateOperationalProfile
    suspend fun listProfiles(tenantId: String, status: AffiliateProfileStatus? = null, query: String? = null): List<AffiliateOperationalProfile>

    // Verifications
    suspend fun findVerificationById(tenantId: String, verificationId: String): AffiliateVerificationRecord?
    suspend fun listVerificationsByAffiliateId(tenantId: String, affiliateId: String): List<AffiliateVerificationRecord>
    suspend fun saveVerification(record: AffiliateVerificationRecord): AffiliateVerificationRecord

    // Documents
    suspend fun findDocumentById(tenantId: String, documentId: String): AffiliateDocumentReference?
    suspend fun listDocumentsByAffiliateId(tenantId: String, affiliateId: String): List<AffiliateDocumentReference>
    suspend fun saveDocument(doc: AffiliateDocumentReference): AffiliateDocumentReference

    // Audit & Outbox
    suspend fun saveAuditRecord(record: AffiliateProfileAuditRecord): AffiliateProfileAuditRecord
    suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateProfileAuditRecord>
    suspend fun getLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateProfileAuditRecord?
    suspend fun saveOutboxEvent(event: AffiliateProfileOutboxEvent): AffiliateProfileOutboxEvent
    suspend fun listPendingOutboxEvents(tenantId: String): List<AffiliateProfileOutboxEvent>

    // Governance
    suspend fun getGovernanceSummary(tenantId: String): AffiliateProfileGovernanceSummary
}
