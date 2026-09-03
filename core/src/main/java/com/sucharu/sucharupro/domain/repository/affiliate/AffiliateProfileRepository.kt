package com.sucharu.sucharupro.domain.repository.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Domain Repository Interface for Affiliate Profiles, Verifications, Documents, and Audits.
 */
interface AffiliateProfileRepository {
    // Profile
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
    suspend fun recordAudit(record: AffiliateProfileAuditRecord): AffiliateProfileAuditRecord
    suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateProfileAuditRecord>
    suspend fun getLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateProfileAuditRecord?
    suspend fun saveOutboxEvent(event: AffiliateProfileOutboxEvent): AffiliateProfileOutboxEvent

    // Governance Summary
    suspend fun getGovernanceSummary(tenantId: String): AffiliateProfileGovernanceSummary
}
