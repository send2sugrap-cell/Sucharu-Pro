package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import java.security.MessageDigest
import java.util.UUID

/**
 * Domain Validation & Cryptographic Engine for Affiliate Management (Module 20 Step 01).
 */
object AffiliateValidationEngine {

    private val CODE_REGEX = Regex("^[A-Z0-9_-]{3,32}$")

    /**
     * Normalizes and validates an affiliate code according to tenant-safe naming invariants.
     */
    fun normalizeAndValidateCode(rawCode: String): String {
        val trimmed = rawCode.trim().uppercase()
        require(trimmed.isNotBlank()) { "Affiliate code cannot be blank." }
        require(CODE_REGEX.matches(trimmed)) {
            "Affiliate code '$trimmed' is invalid. Must be 3-32 uppercase alphanumeric characters, hyphens, or underscores."
        }
        return trimmed
    }

    /**
     * Generates a deterministic, unique default affiliate code from display name and user ID if not provided.
     */
    fun generateDefaultAffiliateCode(displayName: String, userId: String): String {
        val sanitizedName = displayName.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]"), "")
            .take(8)
            .ifBlank { "AFF" }
        val suffix = userId.takeLast(4).uppercase().ifBlank { UUID.randomUUID().toString().take(4).uppercase() }
        return "${sanitizedName}_$suffix"
    }

    /**
     * Validates affiliate lifecycle state transitions.
     */
    fun validateStateTransition(currentStatus: AffiliateStatus, targetStatus: AffiliateStatus) {
        if (currentStatus == targetStatus) return

        if (currentStatus == AffiliateStatus.TERMINATED) {
            throw IllegalStateException("Affiliate is TERMINATED. Terminated affiliates cannot transition to $targetStatus.")
        }

        val isValid = when (currentStatus) {
            AffiliateStatus.PENDING -> targetStatus in setOf(AffiliateStatus.ACTIVE, AffiliateStatus.REJECTED, AffiliateStatus.INACTIVE, AffiliateStatus.TERMINATED)
            AffiliateStatus.ACTIVE -> targetStatus in setOf(AffiliateStatus.SUSPENDED, AffiliateStatus.INACTIVE, AffiliateStatus.TERMINATED)
            AffiliateStatus.SUSPENDED -> targetStatus in setOf(AffiliateStatus.ACTIVE, AffiliateStatus.INACTIVE, AffiliateStatus.TERMINATED)
            AffiliateStatus.INACTIVE -> targetStatus in setOf(AffiliateStatus.ACTIVE, AffiliateStatus.SUSPENDED, AffiliateStatus.TERMINATED)
            AffiliateStatus.REJECTED -> targetStatus in setOf(AffiliateStatus.PENDING, AffiliateStatus.TERMINATED)
            AffiliateStatus.TERMINATED -> false
        }

        if (!isValid) {
            throw IllegalStateException("Illegal affiliate status transition from $currentStatus to $targetStatus.")
        }
    }

    /**
     * Evaluates multi-dimensional eligibility of an affiliate profile.
     */
    fun evaluateEligibility(
        profile: AffiliateProfile,
        evaluatorId: String
    ): AffiliateEligibility {
        val rejectionReasons = mutableListOf<String>()

        val identityVerified = profile.verificationState == VerificationState.VERIFIED
        if (!identityVerified) {
            rejectionReasons.add("Identity is not verified (current state: ${profile.verificationState}).")
        }

        val agreementAccepted = profile.isAgreementAccepted
        if (!agreementAccepted) {
            rejectionReasons.add("Affiliate agreement has not been accepted.")
        }

        val accountActive = profile.status == AffiliateStatus.ACTIVE
        if (!accountActive) {
            rejectionReasons.add("Affiliate status is not ACTIVE (current status: ${profile.status}).")
        }

        val taxCompliant = when (profile.affiliateType) {
            AffiliateType.BUSINESS, AffiliateType.PARTNER -> !profile.taxIdOrGst.isNullOrBlank()
            else -> true
        }
        if (!taxCompliant) {
            rejectionReasons.add("Tax ID/GST is required for ${profile.affiliateType} affiliates.")
        }

        val businessVerified = when (profile.affiliateType) {
            AffiliateType.BUSINESS, AffiliateType.PARTNER -> identityVerified
            else -> true
        }

        val isEligible = identityVerified && agreementAccepted && accountActive && taxCompliant && businessVerified

        return AffiliateEligibility(
            eligibilityId = "ELIG-${UUID.randomUUID().toString().take(12)}",
            tenantId = profile.tenantId,
            affiliateId = profile.affiliateId,
            isEligible = isEligible,
            identityVerified = identityVerified,
            agreementAccepted = agreementAccepted,
            accountActive = accountActive,
            taxCompliant = taxCompliant,
            businessVerified = businessVerified,
            rejectionReasons = rejectionReasons,
            evaluatedAt = System.currentTimeMillis(),
            evaluatedBy = evaluatorId
        )
    }

    /**
     * Computes SHA-256 record hash for an append-only audit entry.
     */
    fun computeRecordHash(
        tenantId: String,
        affiliateId: String,
        eventType: AffiliateAuditEventType,
        previousStatus: AffiliateStatus?,
        newStatus: AffiliateStatus,
        actorType: AffiliateActorType,
        actorId: String,
        actorRole: String,
        timestamp: Long,
        correlationId: String,
        reason: String
    ): String {
        val payload = "$tenantId|$affiliateId|${eventType.name}|${previousStatus?.name ?: ""}|${newStatus.name}|${actorType.name}|$actorId|$actorRole|$timestamp|$correlationId|$reason"
        return sha256(payload)
    }

    /**
     * Computes cryptographic chain hash linking previous audit record to current record hash.
     */
    fun computeChainHash(previousAuditHash: String?, recordHash: String): String {
        val prev = previousAuditHash ?: "GENESIS_AFFILIATE_AUDIT_BLOCK"
        return sha256("$prev|$recordHash")
    }

    /**
     * Synthesizes the downstream AI Handoff Contract (v1.0.0).
     */
    fun synthesizeHandoffContract(
        profile: AffiliateProfile,
        eligibility: AffiliateEligibility
    ): Module20Step01AffiliateHandoffContract {
        val isEligible = eligibility.isEligible
        val sealHash = sha256("${profile.tenantId}|${profile.affiliateId}|${profile.affiliateCode}|${profile.status}|${profile.version}|$isEligible")

        return Module20Step01AffiliateHandoffContract(
            contractVersion = "v1.0.0",
            tenantId = profile.tenantId,
            affiliateId = profile.affiliateId,
            userId = profile.userId,
            customerId = profile.customerId,
            affiliateCode = profile.affiliateCode,
            displayName = profile.displayName,
            status = profile.status,
            affiliateType = profile.affiliateType,
            isEligibleForCommission = isEligible,
            isEligibleForAttribution = profile.status in setOf(AffiliateStatus.ACTIVE, AffiliateStatus.PENDING),
            verificationState = profile.verificationState,
            agreementAccepted = profile.isAgreementAccepted,
            agreementVersion = profile.agreementVersion,
            joinedAt = profile.joinedAt,
            activatedAt = profile.activatedAt,
            isReadOnly = true,
            integritySealHash = sealHash,
            generatedAt = System.currentTimeMillis()
        )
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
