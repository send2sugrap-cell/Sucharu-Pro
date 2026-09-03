package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateVerificationStatus
import java.security.MessageDigest
import java.util.UUID

/**
 * Deterministic Governance Integrity Engine for Affiliate Management (Module 20 Step 06).
 *
 * Responsibilities:
 *  1. Cross-step lifecycle consistency validation (Steps 01–05)
 *  2. Integration readiness state computation for Modules 21–24
 *  3. SHA-256 audit chain tamper-detection
 *  4. Final unified handoff contract synthesis (v20.06)
 *
 * SECURITY: This object is READ-ONLY. It NEVER mutates affiliate state.
 * AI_AGENT actors may invoke read operations; all mutation attempts are
 * blocked upstream by AffiliateServiceImpl.assertMutationAllowed().
 */
object AffiliateGovernanceIntegrityEngine {

    // ----------------------------------------------------------------
    // 1. LIFECYCLE INTEGRITY CHECKER
    // ----------------------------------------------------------------

    /**
     * Performs a full cross-step consistency check on an affiliate's aggregate state.
     *
     * Detects violations such as:
     *  - ACTIVE status with unverified identity
     *  - Agreement accepted flag inconsistent with acceptedAt timestamp
     *  - ACTIVE enrollment with a non-ACTIVE affiliate
     *  - Commission-eligible affiliate with incomplete profile
     *  - Operational profile submitted but with zero completeness score
     *  - No audit records for an affiliate that has undergone status transitions
     */
    fun checkLifecycleIntegrity(
        profile: AffiliateProfile,
        eligibility: AffiliateEligibility?,
        operationalProfile: AffiliateOperationalProfile?,
        verifications: List<AffiliateVerificationRecord>,
        enrollments: List<AffiliateEnrollment>,
        communications: List<AffiliateCommunicationRecord>,
        auditRecords: List<AffiliateAuditRecord>,
        checkedBy: String
    ): AffiliateLifecycleIntegrityResult {
        val checkId = "IC-${UUID.randomUUID().toString().take(12)}"
        val violations = mutableListOf<AffiliateIntegrityViolation>()

        // --- Step 01 Checks ---

        if (profile.status == AffiliateStatus.ACTIVE &&
            profile.verificationState != VerificationState.VERIFIED
        ) {
            violations += AffiliateIntegrityViolation(
                code = "S01_ACTIVE_WITHOUT_VERIFIED_IDENTITY",
                description = "Affiliate is ACTIVE but identity is not VERIFIED (state: ${profile.verificationState}).",
                severity = AffiliateIntegrityViolationSeverity.CRITICAL,
                step = "STEP_01",
                recommendation = "Suspend the affiliate and require identity re-verification before reactivation."
            )
        }

        if (profile.status == AffiliateStatus.ACTIVE && !profile.isAgreementAccepted) {
            violations += AffiliateIntegrityViolation(
                code = "S01_ACTIVE_WITHOUT_AGREEMENT",
                description = "Affiliate is ACTIVE but has not accepted the affiliate agreement.",
                severity = AffiliateIntegrityViolationSeverity.CRITICAL,
                step = "STEP_01",
                recommendation = "Require affiliate to accept the current agreement version before reactivation."
            )
        }

        if (profile.agreementAcceptedAt != null && profile.agreementReference.isNullOrBlank()) {
            violations += AffiliateIntegrityViolation(
                code = "S01_AGREEMENT_TIMESTAMP_WITHOUT_REFERENCE",
                description = "agreementAcceptedAt is populated but agreementReference is blank. This signals a data integrity gap.",
                severity = AffiliateIntegrityViolationSeverity.HIGH,
                step = "STEP_01",
                recommendation = "Repair the agreement reference field in the affiliate record."
            )
        }

        if (profile.activatedAt != null && profile.status == AffiliateStatus.PENDING) {
            violations += AffiliateIntegrityViolation(
                code = "S01_ACTIVATION_TIMESTAMP_ON_PENDING",
                description = "activatedAt is set but affiliate is still in PENDING status.",
                severity = AffiliateIntegrityViolationSeverity.HIGH,
                step = "STEP_01",
                recommendation = "Clear activatedAt or transition affiliate to ACTIVE state."
            )
        }

        val affiliateTypesRequiringTax = setOf(AffiliateType.BUSINESS, AffiliateType.PARTNER)
        if (profile.affiliateType in affiliateTypesRequiringTax && profile.taxIdOrGst.isNullOrBlank() &&
            profile.status == AffiliateStatus.ACTIVE
        ) {
            violations += AffiliateIntegrityViolation(
                code = "S01_BUSINESS_MISSING_TAX_ID",
                description = "ACTIVE ${profile.affiliateType} affiliate is missing a Tax ID / GST number.",
                severity = AffiliateIntegrityViolationSeverity.HIGH,
                step = "STEP_01",
                recommendation = "Collect Tax ID / GST from the affiliate before commission can be issued."
            )
        }

        if (auditRecords.isEmpty() && profile.status != AffiliateStatus.PENDING) {
            violations += AffiliateIntegrityViolation(
                code = "S01_MISSING_AUDIT_TRAIL",
                description = "Affiliate has no audit records but has progressed beyond PENDING state.",
                severity = AffiliateIntegrityViolationSeverity.CRITICAL,
                step = "STEP_01",
                recommendation = "Investigate for audit chain corruption. Do not issue commissions without a verified audit trail."
            )
        }

        // --- Step 02 Checks ---

        val activeEnrollments = enrollments.filter { it.enrollmentStatus == AffiliateEnrollmentStatus.ACTIVE }
        if (activeEnrollments.isNotEmpty() && profile.status != AffiliateStatus.ACTIVE) {
            violations += AffiliateIntegrityViolation(
                code = "S02_ACTIVE_ENROLLMENT_WITH_NON_ACTIVE_AFFILIATE",
                description = "Affiliate has ${activeEnrollments.size} ACTIVE enrollment(s) but affiliate status is ${profile.status}.",
                severity = AffiliateIntegrityViolationSeverity.HIGH,
                step = "STEP_02",
                recommendation = "Suspend or terminate enrollments to match the affiliate's current status."
            )
        }

        // --- Step 03 Checks ---

        if (operationalProfile != null) {
            if (operationalProfile.completenessScore < 50 &&
                profile.status == AffiliateStatus.ACTIVE
            ) {
                violations += AffiliateIntegrityViolation(
                    code = "S03_LOW_PROFILE_COMPLETENESS_ACTIVE",
                    description = "ACTIVE affiliate has operational profile completeness score of ${operationalProfile.completenessScore}% (< 50%).",
                    severity = AffiliateIntegrityViolationSeverity.MEDIUM,
                    step = "STEP_03",
                    recommendation = "Prompt affiliate to complete their operational profile to unlock commission eligibility."
                )
            }
        } else if (profile.status == AffiliateStatus.ACTIVE) {
            violations += AffiliateIntegrityViolation(
                code = "S03_MISSING_OPERATIONAL_PROFILE",
                description = "ACTIVE affiliate has no operational profile.",
                severity = AffiliateIntegrityViolationSeverity.MEDIUM,
                step = "STEP_03",
                recommendation = "Create an operational profile for this affiliate."
            )
        }

        val hasVerifiedDoc = verifications.any { it.status == AffiliateVerificationStatus.VERIFIED }

        if (profile.status == AffiliateStatus.ACTIVE &&
            profile.verificationState == VerificationState.VERIFIED &&
            !hasVerifiedDoc &&
            verifications.isNotEmpty()
        ) {
            violations += AffiliateIntegrityViolation(
                code = "S03_VERIFIED_STATE_WITHOUT_VERIFIED_RECORD",
                description = "Affiliate verificationState is VERIFIED but no individual verification record has status VERIFIED.",
                severity = AffiliateIntegrityViolationSeverity.HIGH,
                step = "STEP_03",
                recommendation = "Re-run verification or inspect verification records for data inconsistency."
            )
        }

        // --- Step 04 Checks ---
        // (Communications governance — informational only; no blocking violations)

        // --- Step 05 Eligibility Cross-Check ---
        if (eligibility != null &&
            eligibility.isEligible &&
            profile.status != AffiliateStatus.ACTIVE
        ) {
            violations += AffiliateIntegrityViolation(
                code = "S05_ELIGIBLE_BUT_NOT_ACTIVE",
                description = "Affiliate is evaluated as eligible but current status is ${profile.status}.",
                severity = AffiliateIntegrityViolationSeverity.MEDIUM,
                step = "STEP_05",
                recommendation = "Re-evaluate eligibility to reflect the current affiliate status."
            )
        }

        val criticalCount = violations.count { it.severity == AffiliateIntegrityViolationSeverity.CRITICAL }
        val highCount = violations.count { it.severity == AffiliateIntegrityViolationSeverity.HIGH }
        val mediumCount = violations.count { it.severity == AffiliateIntegrityViolationSeverity.MEDIUM }
        val lowCount = violations.count { it.severity == AffiliateIntegrityViolationSeverity.LOW }

        val isValid = violations.none {
            it.severity == AffiliateIntegrityViolationSeverity.CRITICAL ||
                it.severity == AffiliateIntegrityViolationSeverity.HIGH
        }

        val summary = buildString {
            if (violations.isEmpty()) {
                append("All lifecycle integrity checks passed. Affiliate ${profile.affiliateId} is fully consistent across Steps 01–05.")
            } else {
                append("${violations.size} violation(s) detected: $criticalCount CRITICAL, $highCount HIGH, $mediumCount MEDIUM, $lowCount LOW.")
                if (!isValid) append(" Downstream module access BLOCKED until CRITICAL/HIGH violations are resolved.")
            }
        }

        val resultHash = sha256(
            "${profile.tenantId}|${profile.affiliateId}|$isValid|$criticalCount|$highCount|${System.currentTimeMillis()}"
        )

        return AffiliateLifecycleIntegrityResult(
            checkId = checkId,
            tenantId = profile.tenantId,
            affiliateId = profile.affiliateId,
            isIntegrityValid = isValid,
            violations = violations,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            summary = summary,
            checkedBy = checkedBy,
            resultHash = resultHash
        )
    }

    // ----------------------------------------------------------------
    // 2. INTEGRATION READINESS BUILDER
    // ----------------------------------------------------------------

    /**
     * Computes a deterministic, step-wise integration readiness state for an affiliate.
     *
     * Readiness gates for downstream modules:
     *  - Module 21 (Attribution): profile ACTIVE + eligibility passed
     *  - Module 22 (Commission):  Module 21 gates + verified identity + agreement + tax compliance
     *  - Module 23 (Payout):      Module 22 gates + operational profile + verified documents
     *  - Module 24 (Analytics):   minimal gate — profile exists and has been active at some point
     */
    fun buildIntegrationReadinessState(
        profile: AffiliateProfile,
        eligibility: AffiliateEligibility?,
        operationalProfile: AffiliateOperationalProfile?,
        verifications: List<AffiliateVerificationRecord>,
        enrollments: List<AffiliateEnrollment>,
        workItems: List<AffiliateGovernanceWorkItem>,
        notificationPreferences: List<AffiliateNotificationPreference>,
        assessedBy: String
    ): AffiliateIntegrationReadinessState {
        val profileExists = true
        val isIdentityVerified = profile.verificationState == VerificationState.VERIFIED
        val isAgreementAccepted = profile.isAgreementAccepted
        val isAccountActive = profile.status == AffiliateStatus.ACTIVE
        val isTaxCompliant = when (profile.affiliateType) {
            AffiliateType.BUSINESS, AffiliateType.PARTNER -> !profile.taxIdOrGst.isNullOrBlank()
            else -> true
        }
        val isBusinessVerified = when (profile.affiliateType) {
            AffiliateType.BUSINESS, AffiliateType.PARTNER -> isIdentityVerified
            else -> true
        }
        val isFullyEligible = eligibility?.isEligible ?: false

        val hasActiveEnrollment = enrollments.any { it.enrollmentStatus == AffiliateEnrollmentStatus.ACTIVE }
        val hasAtLeastOneProgramEnrollment = enrollments.isNotEmpty()

        val hasOperationalProfile = operationalProfile != null
        val profileCompletenessScore = operationalProfile?.completenessScore ?: 0
        val hasVerifiedDocuments = verifications.any { it.status == AffiliateVerificationStatus.VERIFIED }

        val hasAcceptedNotificationPreferences = notificationPreferences.isNotEmpty()

        val urgentOpenItems = workItems.filter {
            it.status == AffiliateGovernanceWorkItemStatus.OPEN &&
                it.priority == AffiliateGovernanceWorkItemPriority.URGENT
        }
        val hasNoOpenUrgentWorkItems = urgentOpenItems.isEmpty()
        val hasClearGovernanceQueue = workItems.none {
            it.status == AffiliateGovernanceWorkItemStatus.OPEN ||
                it.status == AffiliateGovernanceWorkItemStatus.ESCALATED
        }

        // Readiness derivation
        val isReadyForAttribution = isAccountActive && isFullyEligible
        val isReadyForCommission = isReadyForAttribution &&
            isIdentityVerified && isAgreementAccepted && isTaxCompliant && isBusinessVerified
        val isReadyForPayout = isReadyForCommission &&
            hasOperationalProfile && profileCompletenessScore >= 60 && hasVerifiedDocuments
        val isReadyForAnalytics = profileExists && (
            profile.status == AffiliateStatus.ACTIVE ||
                profile.activatedAt != null
            )

        // Composite readiness score (0–100)
        val scoreComponents = listOf(
            profileExists to 5,
            isIdentityVerified to 20,
            isAgreementAccepted to 15,
            isAccountActive to 15,
            isTaxCompliant to 10,
            isBusinessVerified to 5,
            isFullyEligible to 10,
            hasActiveEnrollment to 5,
            hasOperationalProfile to 5,
            (profileCompletenessScore >= 60) to 5,
            hasVerifiedDocuments to 5
        )
        val readinessScore = scoreComponents.sumOf { (flag, weight) -> if (flag) weight else 0 }

        val integrityHash = sha256(
            "${profile.tenantId}|${profile.affiliateId}|$isReadyForAttribution|$isReadyForCommission|$isReadyForPayout|$isReadyForAnalytics|$readinessScore"
        )

        return AffiliateIntegrationReadinessState(
            affiliateId = profile.affiliateId,
            tenantId = profile.tenantId,
            profileExists = profileExists,
            isIdentityVerified = isIdentityVerified,
            isAgreementAccepted = isAgreementAccepted,
            isAccountActive = isAccountActive,
            isTaxCompliant = isTaxCompliant,
            isBusinessVerified = isBusinessVerified,
            isFullyEligible = isFullyEligible,
            hasActiveEnrollment = hasActiveEnrollment,
            hasAtLeastOneProgramEnrollment = hasAtLeastOneProgramEnrollment,
            hasOperationalProfile = hasOperationalProfile,
            profileCompletenessScore = profileCompletenessScore,
            hasVerifiedDocuments = hasVerifiedDocuments,
            hasAcceptedNotificationPreferences = hasAcceptedNotificationPreferences,
            hasNoOpenUrgentWorkItems = hasNoOpenUrgentWorkItems,
            hasClearGovernanceQueue = hasClearGovernanceQueue,
            isReadyForAttribution = isReadyForAttribution,
            isReadyForCommission = isReadyForCommission,
            isReadyForPayout = isReadyForPayout,
            isReadyForAnalytics = isReadyForAnalytics,
            readinessScore = readinessScore,
            assessedBy = assessedBy,
            integrityHash = integrityHash
        )
    }

    // ----------------------------------------------------------------
    // 3. AUDIT CHAIN VERIFIER
    // ----------------------------------------------------------------

    /**
     * Scans the affiliate audit record chain for SHA-256 integrity tampering.
     *
     * For each record (sorted by timestamp ascending), recomputes the expected
     * chain hash and compares it to the stored chain hash. Reports the first
     * detected tamper point.
     */
    fun verifyAuditChainIntegrity(
        tenantId: String,
        affiliateId: String,
        records: List<AffiliateAuditRecord>
    ): AuditChainVerificationResult {
        val verificationId = "ACV-${UUID.randomUUID().toString().take(12)}"
        val sorted = records.sortedBy { it.timestamp }

        if (sorted.isEmpty()) {
            return AuditChainVerificationResult(
                verificationId = verificationId,
                tenantId = tenantId,
                affiliateId = affiliateId,
                totalRecordsChecked = 0,
                isChainIntact = true,
                summary = "No audit records to verify. Chain is vacuously intact."
            )
        }

        var previousHash: String? = null
        for ((index, record) in sorted.withIndex()) {
            val expectedRecordHash = AffiliateValidationEngine.computeRecordHash(
                tenantId = record.tenantId,
                affiliateId = record.affiliateId,
                eventType = record.eventType,
                previousStatus = record.previousStatus,
                newStatus = record.newStatus,
                actorType = record.actorType,
                actorId = record.actorId,
                actorRole = record.actorRole,
                timestamp = record.timestamp,
                correlationId = record.correlationId,
                reason = record.reason
            )
            val expectedChainHash = AffiliateValidationEngine.computeChainHash(previousHash, expectedRecordHash)

            if (record.chainHash != expectedChainHash) {
                return AuditChainVerificationResult(
                    verificationId = verificationId,
                    tenantId = tenantId,
                    affiliateId = affiliateId,
                    totalRecordsChecked = index + 1,
                    isChainIntact = false,
                    firstTamperedAuditId = record.auditId,
                    firstTamperedIndex = index,
                    summary = "TAMPER DETECTED at audit record index $index (auditId=${record.auditId}). " +
                        "Expected chainHash: $expectedChainHash, actual: ${record.chainHash}."
                )
            }
            previousHash = record.chainHash
        }

        return AuditChainVerificationResult(
            verificationId = verificationId,
            tenantId = tenantId,
            affiliateId = affiliateId,
            totalRecordsChecked = sorted.size,
            isChainIntact = true,
            summary = "Audit chain fully verified. ${sorted.size} record(s) are intact and unmodified."
        )
    }

    // ----------------------------------------------------------------
    // 4. FINAL HANDOFF CONTRACT SYNTHESIZER
    // ----------------------------------------------------------------

    /**
     * Synthesizes the master Module20Step06FinalGovernanceHandoffContract.
     *
     * This is the authoritative, cryptographically sealed contract that serves
     * as the single integration handshake between Module 20 and Modules 21–24.
     */
    fun synthesizeFinalHandoffContract(
        profile: AffiliateProfile,
        eligibility: AffiliateEligibility?,
        integrationReadiness: AffiliateIntegrationReadinessState,
        integrityResult: AffiliateLifecycleIntegrityResult
    ): Module20Step06FinalGovernanceHandoffContract {
        val sealHash = sha256(
            "${profile.tenantId}|${profile.affiliateId}|${profile.affiliateCode}" +
                "|${profile.status}|${profile.version}" +
                "|${integrationReadiness.isReadyForAttribution}" +
                "|${integrationReadiness.isReadyForCommission}" +
                "|${integrationReadiness.isReadyForPayout}" +
                "|${integrationReadiness.isReadyForAnalytics}" +
                "|${integrityResult.isIntegrityValid}" +
                "|${System.currentTimeMillis()}"
        )

        return Module20Step06FinalGovernanceHandoffContract(
            tenantId = profile.tenantId,
            affiliateId = profile.affiliateId,
            userId = profile.userId,
            customerId = profile.customerId,
            affiliateCode = profile.affiliateCode,
            displayName = profile.displayName,
            currentStatus = profile.status,
            affiliateType = profile.affiliateType,
            verificationState = profile.verificationState,
            onboardingState = profile.onboardingState,
            isFullyEligible = eligibility?.isEligible ?: false,
            eligibilityRejectionReasons = eligibility?.rejectionReasons ?: emptyList(),
            integrationReadiness = integrationReadiness,
            integrityResult = integrityResult,
            isReadyForAttribution = integrationReadiness.isReadyForAttribution && integrityResult.isIntegrityValid,
            isReadyForCommission = integrationReadiness.isReadyForCommission && integrityResult.isIntegrityValid,
            isReadyForPayout = integrationReadiness.isReadyForPayout && integrityResult.isIntegrityValid,
            isReadyForAnalytics = integrationReadiness.isReadyForAnalytics,
            joinedAt = profile.joinedAt,
            activatedAt = profile.activatedAt,
            integritySealHash = sealHash
        )
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
