package com.sucharu.sucharupro.data.repository.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.AffiliateProfileDataSource
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateProfileRepository

/**
 * Repository Implementation for Affiliate Profile, Verification & Governance Management (Module 20 Step 03).
 */
class AffiliateProfileRepositoryImpl(
    private val dataSource: AffiliateProfileDataSource
) : AffiliateProfileRepository {

    override suspend fun findProfileByAffiliateId(tenantId: String, affiliateId: String): AffiliateOperationalProfile? {
        return dataSource.findProfileByAffiliateId(tenantId, affiliateId)
    }

    override suspend fun saveProfile(profile: AffiliateOperationalProfile): AffiliateOperationalProfile {
        return dataSource.saveProfile(profile)
    }

    override suspend fun listProfiles(tenantId: String, status: AffiliateProfileStatus?, query: String?): List<AffiliateOperationalProfile> {
        return dataSource.listProfiles(tenantId, status, query)
    }

    override suspend fun findVerificationById(tenantId: String, verificationId: String): AffiliateVerificationRecord? {
        return dataSource.findVerificationById(tenantId, verificationId)
    }

    override suspend fun listVerificationsByAffiliateId(tenantId: String, affiliateId: String): List<AffiliateVerificationRecord> {
        return dataSource.listVerificationsByAffiliateId(tenantId, affiliateId)
    }

    override suspend fun saveVerification(record: AffiliateVerificationRecord): AffiliateVerificationRecord {
        return dataSource.saveVerification(record)
    }

    override suspend fun findDocumentById(tenantId: String, documentId: String): AffiliateDocumentReference? {
        return dataSource.findDocumentById(tenantId, documentId)
    }

    override suspend fun listDocumentsByAffiliateId(tenantId: String, affiliateId: String): List<AffiliateDocumentReference> {
        return dataSource.listDocumentsByAffiliateId(tenantId, affiliateId)
    }

    override suspend fun saveDocument(doc: AffiliateDocumentReference): AffiliateDocumentReference {
        return dataSource.saveDocument(doc)
    }

    override suspend fun recordAudit(record: AffiliateProfileAuditRecord): AffiliateProfileAuditRecord {
        return dataSource.saveAuditRecord(record)
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateProfileAuditRecord> {
        return dataSource.listAuditRecords(tenantId, affiliateId)
    }

    override suspend fun getLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateProfileAuditRecord? {
        return dataSource.getLatestAuditRecord(tenantId, affiliateId)
    }

    override suspend fun saveOutboxEvent(event: AffiliateProfileOutboxEvent): AffiliateProfileOutboxEvent {
        return dataSource.saveOutboxEvent(event)
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateProfileGovernanceSummary {
        return dataSource.getGovernanceSummary(tenantId)
    }
}
