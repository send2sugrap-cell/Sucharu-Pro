package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import java.security.MessageDigest

/**
 * Domain Validation & Cryptographic Engine for Affiliate Profile, Verification & Governance Management (Module 20 Step 03).
 */
object AffiliateProfileValidationEngine {

    const val GENESIS_AFFILIATE_PROFILE_AUDIT_BLOCK = "0000000000000000000000000000000000000000000000000000000000000000"

    /**
     * Validates operational profile lifecycle state transitions.
     */
    fun validateProfileStateTransition(current: AffiliateProfileStatus, target: AffiliateProfileStatus) {
        if (current == target) return

        val isValid = when (current) {
            AffiliateProfileStatus.INCOMPLETE -> target in setOf(
                AffiliateProfileStatus.SUBMITTED,
                AffiliateProfileStatus.UNDER_REVIEW,
                AffiliateProfileStatus.SUSPENDED
            )
            AffiliateProfileStatus.SUBMITTED -> target in setOf(
                AffiliateProfileStatus.UNDER_REVIEW,
                AffiliateProfileStatus.CHANGES_REQUIRED,
                AffiliateProfileStatus.VERIFIED,
                AffiliateProfileStatus.SUSPENDED
            )
            AffiliateProfileStatus.UNDER_REVIEW -> target in setOf(
                AffiliateProfileStatus.VERIFIED,
                AffiliateProfileStatus.CHANGES_REQUIRED,
                AffiliateProfileStatus.SUBMITTED,
                AffiliateProfileStatus.SUSPENDED
            )
            AffiliateProfileStatus.VERIFIED -> target in setOf(
                AffiliateProfileStatus.CHANGES_REQUIRED,
                AffiliateProfileStatus.UNDER_REVIEW,
                AffiliateProfileStatus.SUSPENDED
            )
            AffiliateProfileStatus.CHANGES_REQUIRED -> target in setOf(
                AffiliateProfileStatus.SUBMITTED,
                AffiliateProfileStatus.UNDER_REVIEW,
                AffiliateProfileStatus.SUSPENDED
            )
            AffiliateProfileStatus.SUSPENDED -> target in setOf(
                AffiliateProfileStatus.UNDER_REVIEW,
                AffiliateProfileStatus.INCOMPLETE,
                AffiliateProfileStatus.VERIFIED
            )
        }

        if (!isValid) {
            throw IllegalStateException("Illegal affiliate profile status transition from $current to $target.")
        }
    }

    /**
     * Validates verification record lifecycle state transitions.
     */
    fun validateVerificationStateTransition(current: AffiliateVerificationStatus, target: AffiliateVerificationStatus) {
        if (current == target) return

        val isValid = when (current) {
            AffiliateVerificationStatus.NOT_SUBMITTED -> target in setOf(
                AffiliateVerificationStatus.SUBMITTED,
                AffiliateVerificationStatus.UNDER_REVIEW
            )
            AffiliateVerificationStatus.SUBMITTED -> target in setOf(
                AffiliateVerificationStatus.UNDER_REVIEW,
                AffiliateVerificationStatus.VERIFIED,
                AffiliateVerificationStatus.REJECTED
            )
            AffiliateVerificationStatus.UNDER_REVIEW -> target in setOf(
                AffiliateVerificationStatus.VERIFIED,
                AffiliateVerificationStatus.REJECTED,
                AffiliateVerificationStatus.SUBMITTED
            )
            AffiliateVerificationStatus.VERIFIED -> target in setOf(
                AffiliateVerificationStatus.EXPIRED,
                AffiliateVerificationStatus.UNDER_REVIEW
            )
            AffiliateVerificationStatus.REJECTED -> target in setOf(
                AffiliateVerificationStatus.SUBMITTED,
                AffiliateVerificationStatus.UNDER_REVIEW
            )
            AffiliateVerificationStatus.EXPIRED -> target in setOf(
                AffiliateVerificationStatus.SUBMITTED,
                AffiliateVerificationStatus.UNDER_REVIEW
            )
        }

        if (!isValid) {
            throw IllegalStateException("Illegal affiliate verification status transition from $current to $target.")
        }
    }

    /**
     * Validates document reference lifecycle state transitions.
     */
    fun validateDocumentStateTransition(current: AffiliateDocumentStatus, target: AffiliateDocumentStatus) {
        if (current == target) return

        val isValid = when (current) {
            AffiliateDocumentStatus.UPLOADED -> target in setOf(
                AffiliateDocumentStatus.UNDER_REVIEW,
                AffiliateDocumentStatus.VERIFIED,
                AffiliateDocumentStatus.REJECTED
            )
            AffiliateDocumentStatus.UNDER_REVIEW -> target in setOf(
                AffiliateDocumentStatus.VERIFIED,
                AffiliateDocumentStatus.REJECTED,
                AffiliateDocumentStatus.UPLOADED
            )
            AffiliateDocumentStatus.VERIFIED -> target in setOf(
                AffiliateDocumentStatus.EXPIRED,
                AffiliateDocumentStatus.UNDER_REVIEW
            )
            AffiliateDocumentStatus.REJECTED -> target in setOf(
                AffiliateDocumentStatus.UPLOADED,
                AffiliateDocumentStatus.UNDER_REVIEW
            )
            AffiliateDocumentStatus.EXPIRED -> target in setOf(
                AffiliateDocumentStatus.UPLOADED
            )
        }

        if (!isValid) {
            throw IllegalStateException("Illegal affiliate document status transition from $current to $target.")
        }
    }

    /**
     * Evaluates profile completeness score and returns structured breakdown.
     */
    fun evaluateCompleteness(
        profile: AffiliateOperationalProfile,
        documents: List<AffiliateDocumentReference> = emptyList()
    ): ProfileCompletenessResult {
        val requiredFields = mutableListOf<String>()
        val completedFields = mutableListOf<String>()
        val missingFields = mutableListOf<String>()
        val blockingIssues = mutableListOf<String>()

        // 1. Basic Identity
        requiredFields.add("displayName")
        if (profile.displayName.isNotBlank()) {
            completedFields.add("displayName")
        } else {
            missingFields.add("displayName")
            blockingIssues.add("Display name is required.")
        }

        // 2. Business Information
        requiredFields.add("businessType")
        completedFields.add("businessType") // Enum defaults to INDIVIDUAL

        if (profile.businessType in setOf(AffiliateBusinessType.BUSINESS, AffiliateBusinessType.AGENCY, AffiliateBusinessType.PARTNER, AffiliateBusinessType.ORGANIZATION)) {
            requiredFields.add("legalName")
            if (!profile.legalName.isNullOrBlank()) {
                completedFields.add("legalName")
            } else {
                missingFields.add("legalName")
                blockingIssues.add("Legal entity name is required for ${profile.businessType}.")
            }

            requiredFields.add("businessDescription")
            if (!profile.businessDescription.isNullOrBlank()) {
                completedFields.add("businessDescription")
            } else {
                missingFields.add("businessDescription")
            }
        }

        // 3. Contact Details
        requiredFields.add("contactEmail")
        if (!profile.contactEmail.isNullOrBlank()) {
            completedFields.add("contactEmail")
        } else {
            missingFields.add("contactEmail")
            blockingIssues.add("Contact email is required for communication.")
        }

        requiredFields.add("contactPhone")
        if (!profile.contactPhone.isNullOrBlank()) {
            completedFields.add("contactPhone")
        } else {
            missingFields.add("contactPhone")
        }

        // 4. Address Details
        requiredFields.add("addressLine1")
        if (!profile.addressLine1.isNullOrBlank()) {
            completedFields.add("addressLine1")
        } else {
            missingFields.add("addressLine1")
        }

        requiredFields.add("city")
        if (!profile.city.isNullOrBlank()) {
            completedFields.add("city")
        } else {
            missingFields.add("city")
        }

        requiredFields.add("country")
        if (!profile.country.isNullOrBlank()) {
            completedFields.add("country")
        } else {
            missingFields.add("country")
            blockingIssues.add("Country of operation is required.")
        }

        // 5. Tax Compliance
        if (profile.businessType != AffiliateBusinessType.INDIVIDUAL) {
            requiredFields.add("taxIdOrGst")
            if (!profile.taxIdOrGst.isNullOrBlank()) {
                completedFields.add("taxIdOrGst")
            } else {
                missingFields.add("taxIdOrGst")
                blockingIssues.add("Tax ID/GST is required for business entities.")
            }
        }

        // 6. Supporting Documentation
        val verifiedOrUploadedDocs = documents.filter { it.status in setOf(AffiliateDocumentStatus.UPLOADED, AffiliateDocumentStatus.UNDER_REVIEW, AffiliateDocumentStatus.VERIFIED) }
        requiredFields.add("identityOrBusinessDocument")
        if (verifiedOrUploadedDocs.isNotEmpty()) {
            completedFields.add("identityOrBusinessDocument")
        } else {
            missingFields.add("identityOrBusinessDocument")
            blockingIssues.add("At least one supporting verification document is required.")
        }

        val total = requiredFields.size
        val completed = completedFields.size
        val score = if (total > 0) ((completed.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100) else 100

        val isComplete = missingFields.isEmpty() && blockingIssues.isEmpty()

        return ProfileCompletenessResult(
            affiliateId = profile.affiliateId,
            score = score,
            requiredFields = requiredFields,
            completedFields = completedFields,
            missingFields = missingFields,
            blockingIssues = blockingIssues,
            isComplete = isComplete
        )
    }

    /**
     * Computes deterministic SHA-256 hash of an audit record.
     */
    fun computeAuditRecordHash(
        tenantId: String,
        auditId: String,
        affiliateId: String,
        actorUserId: String,
        action: String,
        previousState: String?,
        newState: String,
        correlationId: String,
        timestamp: Long
    ): String {
        val payload = "$tenantId|$auditId|$affiliateId|$actorUserId|$action|$previousState|$newState|$correlationId|$timestamp"
        return sha256(payload)
    }

    /**
     * Computes chained SHA-256 hash connecting the previous audit block.
     */
    fun computeAuditChainHash(previousChainHash: String?, recordHash: String): String {
        val prev = previousChainHash ?: GENESIS_AFFILIATE_PROFILE_AUDIT_BLOCK
        return sha256("$prev:$recordHash")
    }

    /**
     * Synthesizes an immutable, signed AI Governance Handoff Contract.
     */
    fun synthesizeHandoffContract(
        profile: AffiliateOperationalProfile,
        completenessResult: ProfileCompletenessResult,
        verifications: List<AffiliateVerificationRecord>,
        documents: List<AffiliateDocumentReference>
    ): Module20Step03AffiliateProfileHandoffContract {
        val verificationSummary = verifications.associate {
            it.verificationType.name to it.status.name
        }

        val sealPayload = "${profile.tenantId}:${profile.affiliateId}:${profile.profileStatus}:${completenessResult.score}:${completenessResult.isComplete}:${profile.updatedAt}"
        val sealHash = sha256(sealPayload)

        return Module20Step03AffiliateProfileHandoffContract(
            tenantId = profile.tenantId,
            affiliateId = profile.affiliateId,
            displayName = profile.displayName,
            legalName = profile.legalName,
            businessType = profile.businessType,
            profileStatus = profile.profileStatus,
            completenessScore = completenessResult.score,
            isProfileComplete = completenessResult.isComplete,
            isVerified = profile.isVerified,
            missingFields = completenessResult.missingFields,
            blockingIssues = completenessResult.blockingIssues,
            verificationSummary = verificationSummary,
            documentCount = documents.size,
            isReadOnly = true,
            integritySealHash = sealHash
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
