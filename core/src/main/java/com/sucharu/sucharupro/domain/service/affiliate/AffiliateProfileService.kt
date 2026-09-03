package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Service Contract for Affiliate Profile, Verification & Governance Management (Module 20 Step 03).
 */
interface AffiliateProfileService {

    suspend fun getProfileByAffiliateId(tenantId: String, affiliateId: String): AffiliateOperationalProfile?

    suspend fun listProfiles(tenantId: String, status: AffiliateProfileStatus? = null, query: String? = null): List<AffiliateOperationalProfile>

    suspend fun upsertProfile(
        tenantId: String,
        affiliateId: String,
        request: UpsertAffiliateProfileRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN
    ): AffiliateOperationalProfile

    suspend fun submitProfile(
        tenantId: String,
        affiliateId: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN,
        idempotencyKey: String? = null
    ): AffiliateOperationalProfile

    suspend fun requestVerification(
        tenantId: String,
        affiliateId: String,
        request: RequestVerificationRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN
    ): AffiliateVerificationRecord

    suspend fun approveVerification(
        tenantId: String,
        verificationId: String,
        request: ReviewVerificationRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN
    ): AffiliateVerificationRecord

    suspend fun rejectVerification(
        tenantId: String,
        verificationId: String,
        request: ReviewVerificationRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN
    ): AffiliateVerificationRecord

    suspend fun requestChanges(
        tenantId: String,
        verificationId: String,
        request: ReviewVerificationRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN
    ): AffiliateVerificationRecord

    suspend fun listVerifications(tenantId: String, affiliateId: String): List<AffiliateVerificationRecord>

    suspend fun addDocumentReference(
        tenantId: String,
        affiliateId: String,
        request: AddDocumentReferenceRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN
    ): AffiliateDocumentReference

    suspend fun verifyDocument(
        tenantId: String,
        documentId: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN,
        idempotencyKey: String? = null
    ): AffiliateDocumentReference

    suspend fun rejectDocument(
        tenantId: String,
        documentId: String,
        request: ReviewDocumentRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN
    ): AffiliateDocumentReference

    suspend fun listDocuments(tenantId: String, affiliateId: String): List<AffiliateDocumentReference>

    suspend fun suspendProfile(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN,
        idempotencyKey: String? = null
    ): AffiliateOperationalProfile

    suspend fun reactivateProfile(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN,
        idempotencyKey: String? = null
    ): AffiliateOperationalProfile

    suspend fun getProfileCompleteness(tenantId: String, affiliateId: String): ProfileCompletenessResult

    suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateProfileAuditRecord>

    suspend fun getGovernanceSummary(tenantId: String): AffiliateProfileGovernanceSummary

    suspend fun getHandoffContract(tenantId: String, affiliateId: String): Module20Step03AffiliateProfileHandoffContract
}
