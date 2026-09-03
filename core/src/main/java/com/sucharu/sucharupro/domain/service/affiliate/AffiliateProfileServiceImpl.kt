package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateProfileRepository
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository
import java.util.UUID

/**
 * Service Implementation for Affiliate Profile, Verification & Governance Management (Module 20 Step 03).
 */
class AffiliateProfileServiceImpl(
    private val profileRepository: AffiliateProfileRepository,
    private val affiliateRepository: AffiliateRepository
) : AffiliateProfileService {

    override suspend fun getProfileByAffiliateId(tenantId: String, affiliateId: String): AffiliateOperationalProfile? {
        return profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
    }

    override suspend fun listProfiles(tenantId: String, status: AffiliateProfileStatus?, query: String?): List<AffiliateOperationalProfile> {
        return profileRepository.listProfiles(tenantId, status, query)
    }

    override suspend fun upsertProfile(
        tenantId: String,
        affiliateId: String,
        request: UpsertAffiliateProfileRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType
    ): AffiliateOperationalProfile {
        val affiliate = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        val current = profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
        val now = System.currentTimeMillis()
        val correlationId = "CORR-PROF-UPSERT-${UUID.randomUUID().toString().take(8)}"

        val parsedBusinessType = runCatching {
            AffiliateBusinessType.valueOf(request.businessType.trim().uppercase())
        }.getOrDefault(AffiliateBusinessType.INDIVIDUAL)

        val docs = profileRepository.listDocumentsByAffiliateId(tenantId, affiliateId)

        val targetStatus = current?.profileStatus ?: AffiliateProfileStatus.INCOMPLETE

        val baseProfile = current?.copy(
            displayName = request.displayName.trim().ifBlank { affiliate.displayName },
            legalName = request.legalName?.trim(),
            businessType = parsedBusinessType,
            businessDescription = request.businessDescription?.trim(),
            contactEmail = request.contactEmail?.trim() ?: affiliate.contactEmail,
            contactPhone = request.contactPhone?.trim() ?: affiliate.contactPhone,
            website = request.website?.trim(),
            addressLine1 = request.addressLine1?.trim(),
            addressLine2 = request.addressLine2?.trim(),
            city = request.city?.trim(),
            region = request.region?.trim(),
            country = request.country?.trim(),
            postalCode = request.postalCode?.trim(),
            taxIdOrGst = request.taxIdOrGst?.trim() ?: affiliate.taxIdOrGst,
            taxInformationReference = request.taxInformationReference?.trim(),
            metadataJson = request.metadataJson ?: current.metadataJson,
            updatedAt = now,
            version = current.version + 1
        ) ?: AffiliateOperationalProfile(
            tenantId = tenantId,
            affiliateId = affiliateId,
            displayName = request.displayName.trim().ifBlank { affiliate.displayName },
            legalName = request.legalName?.trim(),
            businessType = parsedBusinessType,
            businessDescription = request.businessDescription?.trim(),
            contactEmail = request.contactEmail?.trim() ?: affiliate.contactEmail,
            contactPhone = request.contactPhone?.trim() ?: affiliate.contactPhone,
            website = request.website?.trim(),
            addressLine1 = request.addressLine1?.trim(),
            addressLine2 = request.addressLine2?.trim(),
            city = request.city?.trim(),
            region = request.region?.trim(),
            country = request.country?.trim(),
            postalCode = request.postalCode?.trim(),
            taxIdOrGst = request.taxIdOrGst?.trim() ?: affiliate.taxIdOrGst,
            taxInformationReference = request.taxInformationReference?.trim(),
            profileStatus = AffiliateProfileStatus.INCOMPLETE,
            createdAt = now,
            updatedAt = now,
            version = 1L,
            metadataJson = request.metadataJson
        )

        val completeness = AffiliateProfileValidationEngine.evaluateCompleteness(baseProfile, docs)
        val profileWithScore = baseProfile.copy(
            completenessScore = completeness.score,
            completenessDetailsJson = """{"score":${completeness.score},"isComplete":${completeness.isComplete},"missingFields":${completeness.missingFields.map { "\"$it\"" }}}"""
        )

        val saved = profileRepository.saveProfile(profileWithScore)

        val actionName = if (current == null) "PROFILE_CREATED" else "PROFILE_UPDATED"
        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = actionName,
            previousState = current?.profileStatus?.name,
            newState = saved.profileStatus.name,
            reason = "Profile information updated",
            correlationId = correlationId,
            idempotencyKey = request.idempotencyKey
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = affiliateId,
            eventType = "AffiliateProfileUpdated",
            payloadJson = """{"affiliateId":"$affiliateId","status":"${saved.profileStatus}","completenessScore":${saved.completenessScore}}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun submitProfile(
        tenantId: String,
        affiliateId: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        idempotencyKey: String?
    ): AffiliateOperationalProfile {
        val current = profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate profile '$affiliateId' not found in tenant '$tenantId'.")

        if (current.profileStatus == AffiliateProfileStatus.SUBMITTED) {
            return current
        }

        AffiliateProfileValidationEngine.validateProfileStateTransition(current.profileStatus, AffiliateProfileStatus.SUBMITTED)

        val docs = profileRepository.listDocumentsByAffiliateId(tenantId, affiliateId)
        val completeness = AffiliateProfileValidationEngine.evaluateCompleteness(current, docs)
        if (!completeness.isComplete) {
            val issues = (completeness.missingFields + completeness.blockingIssues).joinToString(", ")
            throw IllegalStateException("Cannot submit incomplete profile. Missing/Blocking: $issues")
        }

        val now = System.currentTimeMillis()
        val correlationId = "CORR-PROF-SUBMIT-${UUID.randomUUID().toString().take(8)}"

        val submitted = current.copy(
            profileStatus = AffiliateProfileStatus.SUBMITTED,
            submittedAt = now,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = profileRepository.saveProfile(submitted)

        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "PROFILE_SUBMITTED",
            previousState = current.profileStatus.name,
            newState = AffiliateProfileStatus.SUBMITTED.name,
            reason = "Profile submitted for compliance verification",
            correlationId = correlationId,
            idempotencyKey = idempotencyKey
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = affiliateId,
            eventType = "AffiliateProfileSubmitted",
            payloadJson = """{"affiliateId":"$affiliateId","submittedAt":$now,"completenessScore":${saved.completenessScore}}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun requestVerification(
        tenantId: String,
        affiliateId: String,
        request: RequestVerificationRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType
    ): AffiliateVerificationRecord {
        val affiliate = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        val parsedType = runCatching {
            AffiliateVerificationType.valueOf(request.verificationType.trim().uppercase())
        }.getOrDefault(AffiliateVerificationType.OTHER)

        val verificationId = "VERIF-${UUID.randomUUID().toString().take(8).uppercase()}"
        val now = System.currentTimeMillis()
        val correlationId = "CORR-VERIF-REQ-${UUID.randomUUID().toString().take(8)}"

        val record = AffiliateVerificationRecord(
            tenantId = tenantId,
            verificationId = verificationId,
            affiliateId = affiliateId,
            verificationType = parsedType,
            status = AffiliateVerificationStatus.SUBMITTED,
            submittedAt = now,
            reason = request.reason,
            metadataReference = request.metadataReference,
            previousVerificationId = request.previousVerificationId,
            createdAt = now,
            updatedAt = now,
            version = 1L
        )

        val saved = profileRepository.saveVerification(record)

        // Update profile state to UNDER_REVIEW if currently SUBMITTED
        val currentProfile = profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
        if (currentProfile != null && currentProfile.profileStatus == AffiliateProfileStatus.SUBMITTED) {
            val updatedProfile = currentProfile.copy(
                profileStatus = AffiliateProfileStatus.UNDER_REVIEW,
                updatedAt = now,
                version = currentProfile.version + 1
            )
            profileRepository.saveProfile(updatedProfile)
        }

        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "VERIFICATION_REQUESTED",
            entityReference = verificationId,
            previousState = AffiliateVerificationStatus.NOT_SUBMITTED.name,
            newState = AffiliateVerificationStatus.SUBMITTED.name,
            reason = request.reason ?: "Verification check requested for ${parsedType.name}",
            correlationId = correlationId,
            idempotencyKey = request.idempotencyKey
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = verificationId,
            eventType = "AffiliateVerificationRequested",
            payloadJson = """{"affiliateId":"$affiliateId","verificationId":"$verificationId","verificationType":"${parsedType.name}"}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun approveVerification(
        tenantId: String,
        verificationId: String,
        request: ReviewVerificationRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType
    ): AffiliateVerificationRecord {
        val current = profileRepository.findVerificationById(tenantId, verificationId)
            ?: throw NoSuchElementException("Verification record '$verificationId' not found in tenant '$tenantId'.")

        if (current.status == AffiliateVerificationStatus.VERIFIED) {
            return current
        }

        // SoD: Affiliate self-approval prevention
        val affiliate = affiliateRepository.findById(tenantId, current.affiliateId)
        if (affiliate != null && affiliate.userId == actorUserId) {
            throw IllegalStateException("Affiliate cannot approve own verification record (Separation of Duties violation).")
        }

        AffiliateProfileValidationEngine.validateVerificationStateTransition(current.status, AffiliateVerificationStatus.VERIFIED)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-VERIF-APP-${UUID.randomUUID().toString().take(8)}"

        val approved = current.copy(
            status = AffiliateVerificationStatus.VERIFIED,
            reviewedAt = now,
            reviewerUserId = actorUserId,
            reason = request.reason.trim(),
            expiresAt = request.expiresAt,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = profileRepository.saveVerification(approved)

        // Check if all verifications for this affiliate are VERIFIED
        val allVerifs = profileRepository.listVerificationsByAffiliateId(tenantId, current.affiliateId)
        val profile = profileRepository.findProfileByAffiliateId(tenantId, current.affiliateId)
        if (profile != null) {
            val allApproved = allVerifs.all { it.status == AffiliateVerificationStatus.VERIFIED }
            if (allApproved && profile.profileStatus in setOf(AffiliateProfileStatus.SUBMITTED, AffiliateProfileStatus.UNDER_REVIEW)) {
                val verifiedProfile = profile.copy(
                    profileStatus = AffiliateProfileStatus.VERIFIED,
                    verifiedAt = now,
                    updatedAt = now,
                    version = profile.version + 1
                )
                profileRepository.saveProfile(verifiedProfile)
            }
        }

        recordAudit(
            tenantId = tenantId,
            affiliateId = current.affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "VERIFICATION_APPROVED",
            entityReference = verificationId,
            previousState = current.status.name,
            newState = AffiliateVerificationStatus.VERIFIED.name,
            reason = request.reason.trim(),
            correlationId = correlationId,
            idempotencyKey = request.idempotencyKey
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = verificationId,
            eventType = "AffiliateVerificationApproved",
            payloadJson = """{"affiliateId":"${current.affiliateId}","verificationId":"$verificationId","reviewerUserId":"$actorUserId"}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun rejectVerification(
        tenantId: String,
        verificationId: String,
        request: ReviewVerificationRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType
    ): AffiliateVerificationRecord {
        val current = profileRepository.findVerificationById(tenantId, verificationId)
            ?: throw NoSuchElementException("Verification record '$verificationId' not found in tenant '$tenantId'.")

        if (current.status == AffiliateVerificationStatus.REJECTED) {
            return current
        }

        // SoD: Affiliate self-review prevention
        val affiliate = affiliateRepository.findById(tenantId, current.affiliateId)
        if (affiliate != null && affiliate.userId == actorUserId) {
            throw IllegalStateException("Affiliate cannot reject own verification record (Separation of Duties violation).")
        }

        AffiliateProfileValidationEngine.validateVerificationStateTransition(current.status, AffiliateVerificationStatus.REJECTED)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-VERIF-REJ-${UUID.randomUUID().toString().take(8)}"

        val rejected = current.copy(
            status = AffiliateVerificationStatus.REJECTED,
            reviewedAt = now,
            reviewerUserId = actorUserId,
            reason = request.reason.trim(),
            updatedAt = now,
            version = current.version + 1
        )

        val saved = profileRepository.saveVerification(rejected)

        val profile = profileRepository.findProfileByAffiliateId(tenantId, current.affiliateId)
        if (profile != null && profile.profileStatus != AffiliateProfileStatus.SUSPENDED) {
            val updatedProfile = profile.copy(
                profileStatus = AffiliateProfileStatus.CHANGES_REQUIRED,
                updatedAt = now,
                version = profile.version + 1
            )
            profileRepository.saveProfile(updatedProfile)
        }

        recordAudit(
            tenantId = tenantId,
            affiliateId = current.affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "VERIFICATION_REJECTED",
            entityReference = verificationId,
            previousState = current.status.name,
            newState = AffiliateVerificationStatus.REJECTED.name,
            reason = request.reason.trim(),
            correlationId = correlationId,
            idempotencyKey = request.idempotencyKey
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = verificationId,
            eventType = "AffiliateVerificationRejected",
            payloadJson = """{"affiliateId":"${current.affiliateId}","verificationId":"$verificationId","reason":"${request.reason.trim()}"}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun requestChanges(
        tenantId: String,
        verificationId: String,
        request: ReviewVerificationRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType
    ): AffiliateVerificationRecord {
        val current = profileRepository.findVerificationById(tenantId, verificationId)
            ?: throw NoSuchElementException("Verification record '$verificationId' not found in tenant '$tenantId'.")

        // SoD: Affiliate self-review prevention
        val affiliate = affiliateRepository.findById(tenantId, current.affiliateId)
        if (affiliate != null && affiliate.userId == actorUserId) {
            throw IllegalStateException("Affiliate cannot request changes on own verification record.")
        }

        val now = System.currentTimeMillis()
        val correlationId = "CORR-VERIF-CHG-${UUID.randomUUID().toString().take(8)}"

        val changesRequested = current.copy(
            changeRequestNotes = request.changeRequestNotes ?: request.reason,
            reviewedAt = now,
            reviewerUserId = actorUserId,
            reason = request.reason.trim(),
            updatedAt = now,
            version = current.version + 1
        )

        val saved = profileRepository.saveVerification(changesRequested)

        val profile = profileRepository.findProfileByAffiliateId(tenantId, current.affiliateId)
        if (profile != null) {
            val updatedProfile = profile.copy(
                profileStatus = AffiliateProfileStatus.CHANGES_REQUIRED,
                updatedAt = now,
                version = profile.version + 1
            )
            profileRepository.saveProfile(updatedProfile)
        }

        recordAudit(
            tenantId = tenantId,
            affiliateId = current.affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "VERIFICATION_CHANGES_REQUESTED",
            entityReference = verificationId,
            previousState = current.status.name,
            newState = AffiliateProfileStatus.CHANGES_REQUIRED.name,
            reason = request.reason.trim(),
            correlationId = correlationId,
            idempotencyKey = request.idempotencyKey
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = verificationId,
            eventType = "AffiliateVerificationChangesRequested",
            payloadJson = """{"affiliateId":"${current.affiliateId}","verificationId":"$verificationId","notes":"${request.changeRequestNotes ?: request.reason}"}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun listVerifications(tenantId: String, affiliateId: String): List<AffiliateVerificationRecord> {
        return profileRepository.listVerificationsByAffiliateId(tenantId, affiliateId)
    }

    override suspend fun addDocumentReference(
        tenantId: String,
        affiliateId: String,
        request: AddDocumentReferenceRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType
    ): AffiliateDocumentReference {
        val affiliate = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        val parsedType = runCatching {
            AffiliateDocumentType.valueOf(request.documentType.trim().uppercase())
        }.getOrDefault(AffiliateDocumentType.OTHER)

        val documentId = "DOC-${UUID.randomUUID().toString().take(8).uppercase()}"
        val now = System.currentTimeMillis()
        val correlationId = "CORR-DOC-ADD-${UUID.randomUUID().toString().take(8)}"

        val doc = AffiliateDocumentReference(
            tenantId = tenantId,
            documentId = documentId,
            affiliateId = affiliateId,
            verificationId = request.verificationId,
            documentType = parsedType,
            storageReference = request.storageReference.trim(),
            fileName = request.fileName.trim(),
            fileSizeBytes = request.fileSizeBytes,
            mimeType = request.mimeType?.trim(),
            status = AffiliateDocumentStatus.UPLOADED,
            uploadedAt = now,
            expiresAt = request.expiresAt,
            createdAt = now,
            updatedAt = now,
            version = 1L
        )

        val saved = profileRepository.saveDocument(doc)

        // Re-evaluate profile completeness
        val profile = profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
        if (profile != null) {
            val docs = profileRepository.listDocumentsByAffiliateId(tenantId, affiliateId)
            val completeness = AffiliateProfileValidationEngine.evaluateCompleteness(profile, docs)
            val updatedProfile = profile.copy(
                completenessScore = completeness.score,
                completenessDetailsJson = """{"score":${completeness.score},"isComplete":${completeness.isComplete},"missingFields":${completeness.missingFields.map { "\"$it\"" }}}""",
                updatedAt = now,
                version = profile.version + 1
            )
            profileRepository.saveProfile(updatedProfile)
        }

        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "DOCUMENT_UPLOADED",
            entityReference = documentId,
            previousState = null,
            newState = AffiliateDocumentStatus.UPLOADED.name,
            reason = "Supporting document uploaded: ${doc.fileName}",
            correlationId = correlationId,
            idempotencyKey = request.idempotencyKey
        )

        return saved
    }

    override suspend fun verifyDocument(
        tenantId: String,
        documentId: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        idempotencyKey: String?
    ): AffiliateDocumentReference {
        val current = profileRepository.findDocumentById(tenantId, documentId)
            ?: throw NoSuchElementException("Document '$documentId' not found in tenant '$tenantId'.")

        if (current.status == AffiliateDocumentStatus.VERIFIED) {
            return current
        }

        AffiliateProfileValidationEngine.validateDocumentStateTransition(current.status, AffiliateDocumentStatus.VERIFIED)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-DOC-VERIF-${UUID.randomUUID().toString().take(8)}"

        val verified = current.copy(
            status = AffiliateDocumentStatus.VERIFIED,
            verifiedAt = now,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = profileRepository.saveDocument(verified)

        recordAudit(
            tenantId = tenantId,
            affiliateId = current.affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "DOCUMENT_VERIFIED",
            entityReference = documentId,
            previousState = current.status.name,
            newState = AffiliateDocumentStatus.VERIFIED.name,
            reason = "Document verified by compliance reviewer",
            correlationId = correlationId,
            idempotencyKey = idempotencyKey
        )

        return saved
    }

    override suspend fun rejectDocument(
        tenantId: String,
        documentId: String,
        request: ReviewDocumentRequestDto,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType
    ): AffiliateDocumentReference {
        val current = profileRepository.findDocumentById(tenantId, documentId)
            ?: throw NoSuchElementException("Document '$documentId' not found in tenant '$tenantId'.")

        if (current.status == AffiliateDocumentStatus.REJECTED) {
            return current
        }

        AffiliateProfileValidationEngine.validateDocumentStateTransition(current.status, AffiliateDocumentStatus.REJECTED)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-DOC-REJ-${UUID.randomUUID().toString().take(8)}"

        val rejected = current.copy(
            status = AffiliateDocumentStatus.REJECTED,
            rejectionReason = request.rejectionReason?.trim() ?: "Document failed verification criteria",
            updatedAt = now,
            version = current.version + 1
        )

        val saved = profileRepository.saveDocument(rejected)

        recordAudit(
            tenantId = tenantId,
            affiliateId = current.affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "DOCUMENT_REJECTED",
            entityReference = documentId,
            previousState = current.status.name,
            newState = AffiliateDocumentStatus.REJECTED.name,
            reason = request.rejectionReason ?: "Document rejected",
            correlationId = correlationId,
            idempotencyKey = request.idempotencyKey
        )

        return saved
    }

    override suspend fun listDocuments(tenantId: String, affiliateId: String): List<AffiliateDocumentReference> {
        return profileRepository.listDocumentsByAffiliateId(tenantId, affiliateId)
    }

    override suspend fun suspendProfile(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        idempotencyKey: String?
    ): AffiliateOperationalProfile {
        val current = profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate profile '$affiliateId' not found in tenant '$tenantId'.")

        if (current.profileStatus == AffiliateProfileStatus.SUSPENDED) {
            return current
        }

        AffiliateProfileValidationEngine.validateProfileStateTransition(current.profileStatus, AffiliateProfileStatus.SUSPENDED)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-PROF-SUSP-${UUID.randomUUID().toString().take(8)}"

        val suspended = current.copy(
            profileStatus = AffiliateProfileStatus.SUSPENDED,
            suspendedAt = now,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = profileRepository.saveProfile(suspended)

        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "PROFILE_SUSPENDED",
            previousState = current.profileStatus.name,
            newState = AffiliateProfileStatus.SUSPENDED.name,
            reason = reason.trim(),
            correlationId = correlationId,
            idempotencyKey = idempotencyKey
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = affiliateId,
            eventType = "AffiliateProfileSuspended",
            payloadJson = """{"affiliateId":"$affiliateId","reason":"${reason.trim()}","suspendedAt":$now}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun reactivateProfile(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        idempotencyKey: String?
    ): AffiliateOperationalProfile {
        val current = profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate profile '$affiliateId' not found in tenant '$tenantId'.")

        if (current.profileStatus != AffiliateProfileStatus.SUSPENDED) {
            return current
        }

        val targetStatus = if (current.verifiedAt != null) AffiliateProfileStatus.VERIFIED else AffiliateProfileStatus.UNDER_REVIEW
        AffiliateProfileValidationEngine.validateProfileStateTransition(current.profileStatus, targetStatus)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-PROF-REACT-${UUID.randomUUID().toString().take(8)}"

        val reactivated = current.copy(
            profileStatus = targetStatus,
            suspendedAt = null,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = profileRepository.saveProfile(reactivated)

        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "PROFILE_REACTIVATED",
            previousState = AffiliateProfileStatus.SUSPENDED.name,
            newState = targetStatus.name,
            reason = reason.trim(),
            correlationId = correlationId,
            idempotencyKey = idempotencyKey
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = affiliateId,
            eventType = "AffiliateProfileReactivated",
            payloadJson = """{"affiliateId":"$affiliateId","status":"${targetStatus.name}"}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun getProfileCompleteness(tenantId: String, affiliateId: String): ProfileCompletenessResult {
        val profile = profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate profile '$affiliateId' not found in tenant '$tenantId'.")
        val docs = profileRepository.listDocumentsByAffiliateId(tenantId, affiliateId)
        return AffiliateProfileValidationEngine.evaluateCompleteness(profile, docs)
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateProfileAuditRecord> {
        return profileRepository.listAuditRecords(tenantId, affiliateId)
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateProfileGovernanceSummary {
        return profileRepository.getGovernanceSummary(tenantId)
    }

    override suspend fun getHandoffContract(tenantId: String, affiliateId: String): Module20Step03AffiliateProfileHandoffContract {
        val profile = profileRepository.findProfileByAffiliateId(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate profile '$affiliateId' not found in tenant '$tenantId'.")
        val verifs = profileRepository.listVerificationsByAffiliateId(tenantId, affiliateId)
        val docs = profileRepository.listDocumentsByAffiliateId(tenantId, affiliateId)
        val completeness = AffiliateProfileValidationEngine.evaluateCompleteness(profile, docs)

        return AffiliateProfileValidationEngine.synthesizeHandoffContract(
            profile = profile,
            completenessResult = completeness,
            verifications = verifs,
            documents = docs
        )
    }

    private suspend fun recordAudit(
        tenantId: String,
        affiliateId: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        action: String,
        entityReference: String? = null,
        previousState: String?,
        newState: String,
        reason: String?,
        correlationId: String,
        idempotencyKey: String?
    ) {
        val auditId = "AUD-PROF-${UUID.randomUUID().toString().take(8).uppercase()}"
        val now = System.currentTimeMillis()

        val recordHash = AffiliateProfileValidationEngine.computeAuditRecordHash(
            tenantId = tenantId,
            auditId = auditId,
            affiliateId = affiliateId,
            actorUserId = actorUserId,
            action = action,
            previousState = previousState,
            newState = newState,
            correlationId = correlationId,
            timestamp = now
        )

        val latestAudit = profileRepository.getLatestAuditRecord(tenantId, affiliateId)
        val previousAuditHash = latestAudit?.recordHash
        val chainHash = AffiliateProfileValidationEngine.computeAuditChainHash(latestAudit?.chainHash, recordHash)

        val auditRecord = AffiliateProfileAuditRecord(
            tenantId = tenantId,
            auditId = auditId,
            affiliateId = affiliateId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = action,
            entityReference = entityReference,
            previousState = previousState,
            newState = newState,
            reason = reason,
            correlationId = correlationId,
            idempotencyKey = idempotencyKey,
            recordHash = recordHash,
            previousAuditHash = previousAuditHash,
            chainHash = chainHash,
            timestamp = now
        )

        profileRepository.recordAudit(auditRecord)
    }

    private suspend fun recordOutbox(
        tenantId: String,
        aggregateId: String,
        eventType: String,
        payloadJson: String,
        correlationId: String
    ) {
        val outboxId = "OUT-PROF-${UUID.randomUUID().toString().take(8).uppercase()}"
        val event = AffiliateProfileOutboxEvent(
            tenantId = tenantId,
            outboxId = outboxId,
            aggregateId = aggregateId,
            eventType = eventType,
            payloadJson = payloadJson,
            correlationId = correlationId
        )
        profileRepository.saveOutboxEvent(event)
    }
}
