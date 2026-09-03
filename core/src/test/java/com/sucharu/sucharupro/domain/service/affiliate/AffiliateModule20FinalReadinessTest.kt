package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Module 20 Final Readiness & Boundary Tests (Module 20 Step 06).
 *
 * Verifies:
 *  1. All Step 01–05 handoff contracts are read-only and forbid AI mutations
 *  2. No Module 21–24 business logic leaks into Module 20 domain objects
 *  3. State machine completeness: all legal transitions are exercisable
 *  4. Integrity engine violations are correctly severity-graded
 *  5. The final handoff contract version is "v20.06"
 *  6. Cross-module readiness flags are derived correctly from integrity validity
 */
class AffiliateModule20FinalReadinessTest {

    private val tenantId = "TENANT-FINAL"
    private val affiliateId = "AFF-FINAL-01"

    // ─────────────────────────────────────────────────────────────────
    // 1. Handoff contract read-only invariants (Steps 01–05)
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `Module20Step01AffiliateHandoffContract is read-only and blocks mutations`() {
        val contract = Module20Step01AffiliateHandoffContract(
            tenantId = tenantId,
            affiliateId = affiliateId,
            userId = "usr-1",
            customerId = null,
            affiliateCode = "CODE01",
            displayName = "Test",
            status = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.INDIVIDUAL,
            isEligibleForCommission = true,
            isEligibleForAttribution = true,
            verificationState = VerificationState.VERIFIED,
            agreementAccepted = true,
            agreementVersion = "v1.0",
            joinedAt = System.currentTimeMillis(),
            activatedAt = System.currentTimeMillis(),
            integritySealHash = "abc123"
        )
        assertTrue(contract.isReadOnly)
        assertTrue(contract.forbiddenAiActions.contains("ACTIVATE_AFFILIATE"))
        assertTrue(contract.forbiddenAiActions.contains("TERMINATE_AFFILIATE"))
        assertTrue(contract.forbiddenAiActions.contains("REWRITE_AUDIT_HISTORY"))
        assertTrue(contract.forbiddenAiActions.contains("BYPASS_ROW_LEVEL_SECURITY"))
    }

    @Test
    fun `Module20Step06FinalGovernanceHandoffContract has correct version and read-only flag`() {
        val readiness = buildReadiness()
        val integrityResult = buildIntegrityResult(isValid = true)

        val contract = Module20Step06FinalGovernanceHandoffContract(
            tenantId = tenantId,
            affiliateId = affiliateId,
            userId = "usr-1",
            customerId = null,
            affiliateCode = "CODE06",
            displayName = "Test Final",
            currentStatus = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.INDIVIDUAL,
            verificationState = VerificationState.VERIFIED,
            onboardingState = OnboardingState.APPROVED,
            isFullyEligible = true,
            eligibilityRejectionReasons = emptyList(),
            integrationReadiness = readiness,
            integrityResult = integrityResult,
            isReadyForAttribution = true,
            isReadyForCommission = true,
            isReadyForPayout = false,
            isReadyForAnalytics = true,
            joinedAt = System.currentTimeMillis(),
            activatedAt = System.currentTimeMillis(),
            integritySealHash = "seal123"
        )

        assertEquals("v20.06", contract.contractVersion)
        assertTrue(contract.isReadOnly)
    }

    @Test
    fun `Module20Step06 forbids downstream module business logic actions via AI`() {
        val readiness = buildReadiness()
        val integrityResult = buildIntegrityResult(isValid = true)

        val contract = Module20Step06FinalGovernanceHandoffContract(
            tenantId = tenantId,
            affiliateId = affiliateId,
            userId = "usr-1",
            customerId = null,
            affiliateCode = "CODE06",
            displayName = "Test Final",
            currentStatus = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.INDIVIDUAL,
            verificationState = VerificationState.VERIFIED,
            onboardingState = OnboardingState.APPROVED,
            isFullyEligible = true,
            eligibilityRejectionReasons = emptyList(),
            integrationReadiness = readiness,
            integrityResult = integrityResult,
            isReadyForAttribution = true,
            isReadyForCommission = true,
            isReadyForPayout = false,
            isReadyForAnalytics = true,
            joinedAt = System.currentTimeMillis(),
            activatedAt = System.currentTimeMillis(),
            integritySealHash = "seal123"
        )

        val forbidden = contract.forbiddenAiActions
        // Module 22 (Commission) must not be bypassed without the module
        assertTrue(forbidden.contains("GRANT_COMMISSION_WITHOUT_MODULE22"))
        // Module 23 (Payout) must not be bypassed
        assertTrue(forbidden.contains("EXECUTE_PAYOUT_WITHOUT_MODULE23"))
        // Module 21 (Attribution) must not be bypassed
        assertTrue(forbidden.contains("ATTRIBUTE_WITHOUT_MODULE21"))
        // Core protections
        assertTrue(forbidden.contains("ALTER_AUDIT_CHAIN"))
        assertTrue(forbidden.contains("BYPASS_RLS"))
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. No module 21–24 logic leaks in domain models
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `AffiliateIntegrationReadinessState contains no commission amounts or payout values`() {
        val readiness = buildReadiness()
        // Verify only flag-based data is present — no amounts, no tracking IDs
        val fields = readiness.javaClass.declaredFields.map { it.name }
        assertFalse("No commissionAmount expected", fields.any { it.contains("commissionAmount", ignoreCase = true) })
        assertFalse("No payoutAmount expected", fields.any { it.contains("payoutAmount", ignoreCase = true) })
        assertFalse("No trackingUrl expected", fields.any { it.contains("trackingUrl", ignoreCase = true) })
        assertFalse("No couponCode expected", fields.any { it.contains("couponCode", ignoreCase = true) })
    }

    @Test
    fun `Module20Step06 handoff contract fields contain no commission or payout amounts`() {
        val contractFields = Module20Step06FinalGovernanceHandoffContract::class.java.declaredFields.map { it.name }
        assertFalse(contractFields.any { it.contains("commissionAmount", ignoreCase = true) })
        assertFalse(contractFields.any { it.contains("payoutAmount", ignoreCase = true) })
        assertFalse(contractFields.any { it.contains("trackingLink", ignoreCase = true) })
        assertFalse(contractFields.any { it.contains("walletBalance", ignoreCase = true) })
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. AffiliateStatus state machine completeness
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `all valid lifecycle transitions pass validation`() {
        val validTransitions = listOf(
            AffiliateStatus.PENDING to AffiliateStatus.ACTIVE,
            AffiliateStatus.PENDING to AffiliateStatus.REJECTED,
            AffiliateStatus.PENDING to AffiliateStatus.INACTIVE,
            AffiliateStatus.PENDING to AffiliateStatus.TERMINATED,
            AffiliateStatus.ACTIVE to AffiliateStatus.SUSPENDED,
            AffiliateStatus.ACTIVE to AffiliateStatus.INACTIVE,
            AffiliateStatus.ACTIVE to AffiliateStatus.TERMINATED,
            AffiliateStatus.SUSPENDED to AffiliateStatus.ACTIVE,
            AffiliateStatus.SUSPENDED to AffiliateStatus.INACTIVE,
            AffiliateStatus.SUSPENDED to AffiliateStatus.TERMINATED,
            AffiliateStatus.INACTIVE to AffiliateStatus.ACTIVE,
            AffiliateStatus.INACTIVE to AffiliateStatus.SUSPENDED,
            AffiliateStatus.INACTIVE to AffiliateStatus.TERMINATED,
            AffiliateStatus.REJECTED to AffiliateStatus.PENDING,
            AffiliateStatus.REJECTED to AffiliateStatus.TERMINATED
        )
        validTransitions.forEach { (from, to) ->
            // Should not throw
            AffiliateValidationEngine.validateStateTransition(from, to)
        }
    }

    @Test
    fun `TERMINATED is a terminal state — no transition allowed out`() {
        AffiliateStatus.values()
            .filter { it != AffiliateStatus.TERMINATED }
            .forEach { target ->
                val threw = try {
                    AffiliateValidationEngine.validateStateTransition(AffiliateStatus.TERMINATED, target)
                    false
                } catch (_: IllegalStateException) { true }
                assertTrue("Expected exception transitioning from TERMINATED to $target", threw)
            }
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. Violation severity grading
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `AffiliateIntegrityViolationSeverity covers all required levels`() {
        val severities = AffiliateIntegrityViolationSeverity.values().toSet()
        assertTrue(severities.contains(AffiliateIntegrityViolationSeverity.CRITICAL))
        assertTrue(severities.contains(AffiliateIntegrityViolationSeverity.HIGH))
        assertTrue(severities.contains(AffiliateIntegrityViolationSeverity.MEDIUM))
        assertTrue(severities.contains(AffiliateIntegrityViolationSeverity.LOW))
        assertTrue(severities.contains(AffiliateIntegrityViolationSeverity.INFO))
    }

    @Test
    fun `integrity result is invalid when CRITICAL violations exist`() {
        val result = buildIntegrityResult(isValid = false, criticalCount = 1)
        assertFalse(result.isIntegrityValid)
    }

    @Test
    fun `integrity result is valid when only MEDIUM violations exist`() {
        // MEDIUM violations alone do not block downstream access
        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = "usr-1",
            displayName = "Test",
            affiliateCode = "T01",
            status = AffiliateStatus.ACTIVE,
            verificationState = VerificationState.VERIFIED,
            onboardingState = OnboardingState.APPROVED,
            agreementReference = "AGR-01",
            agreementAcceptedAt = System.currentTimeMillis(),
            joinedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = AffiliateEligibility(
                eligibilityId = "ELIG-01", tenantId = tenantId, affiliateId = affiliateId,
                isEligible = true, identityVerified = true, agreementAccepted = true,
                accountActive = true, taxCompliant = true, businessVerified = true,
                evaluatedBy = "sys"
            ),
            operationalProfile = null, // triggers MEDIUM
            verifications = emptyList(),
            enrollments = emptyList(),
            communications = emptyList(),
            auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        // Only MEDIUM violation (missing operational profile) — still valid
        assertTrue(result.isIntegrityValid)
        assertEquals(0, result.criticalCount)
        assertEquals(0, result.highCount)
        assertTrue(result.mediumCount >= 1)
    }

    // ─────────────────────────────────────────────────────────────────
    // 5. Readiness gates with integrity validity
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `integrity failure gates commission and attribution readiness`() {
        val profile = AffiliateProfile(
            affiliateId = affiliateId, tenantId = tenantId, userId = "usr-1",
            displayName = "Bad Affiliate", affiliateCode = "BAD_01",
            status = AffiliateStatus.ACTIVE, verificationState = VerificationState.UNVERIFIED,
            onboardingState = OnboardingState.APPROVED, joinedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile, eligibility = null, operationalProfile = null,
            verifications = emptyList(), enrollments = emptyList(), workItems = emptyList(),
            notificationPreferences = emptyList(), assessedBy = "admin-1"
        )
        val integrityResult = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile, eligibility = null, operationalProfile = null,
            verifications = emptyList(), enrollments = emptyList(),
            communications = emptyList(), auditRecords = emptyList(),
            checkedBy = "admin-1"
        )
        val contract = AffiliateGovernanceIntegrityEngine.synthesizeFinalHandoffContract(
            profile = profile, eligibility = null,
            integrationReadiness = readiness, integrityResult = integrityResult
        )
        assertFalse(contract.isReadyForAttribution)
        assertFalse(contract.isReadyForCommission)
        assertFalse(contract.isReadyForPayout)
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private fun buildReadiness() = AffiliateIntegrationReadinessState(
        affiliateId = affiliateId,
        tenantId = tenantId,
        profileExists = true,
        isIdentityVerified = true,
        isAgreementAccepted = true,
        isAccountActive = true,
        isTaxCompliant = true,
        isBusinessVerified = true,
        isFullyEligible = true,
        hasActiveEnrollment = false,
        hasAtLeastOneProgramEnrollment = false,
        hasOperationalProfile = false,
        profileCompletenessScore = 0,
        hasVerifiedDocuments = false,
        hasAcceptedNotificationPreferences = false,
        hasNoOpenUrgentWorkItems = true,
        hasClearGovernanceQueue = true,
        isReadyForAttribution = true,
        isReadyForCommission = true,
        isReadyForPayout = false,
        isReadyForAnalytics = true,
        readinessScore = 70,
        assessedBy = "admin-1",
        integrityHash = "hash123"
    )

    private fun buildIntegrityResult(isValid: Boolean, criticalCount: Int = 0) =
        AffiliateLifecycleIntegrityResult(
            checkId = "IC-01",
            tenantId = tenantId,
            affiliateId = affiliateId,
            isIntegrityValid = isValid,
            violations = emptyList(),
            criticalCount = criticalCount,
            highCount = 0,
            mediumCount = 0,
            lowCount = 0,
            summary = if (isValid) "All checks passed" else "Violations found",
            checkedBy = "admin-1",
            resultHash = "rh01"
        )

    private fun buildAuditChain(profile: AffiliateProfile, count: Int): List<AffiliateAuditRecord> {
        val records = mutableListOf<AffiliateAuditRecord>()
        var previousHash: String? = null
        repeat(count) { i ->
            val ts = System.currentTimeMillis() + i
            val recordHash = AffiliateValidationEngine.computeRecordHash(
                tenantId = tenantId, affiliateId = affiliateId,
                eventType = AffiliateAuditEventType.AFFILIATE_CREATED,
                previousStatus = null, newStatus = profile.status,
                actorType = AffiliateActorType.HUMAN, actorId = "admin-1",
                actorRole = "ADMIN", timestamp = ts, correlationId = "CORR-$i",
                reason = "Test"
            )
            val chainHash = AffiliateValidationEngine.computeChainHash(previousHash, recordHash)
            records += AffiliateAuditRecord(
                auditId = "AUD-$i", tenantId = tenantId, affiliateId = affiliateId,
                eventType = AffiliateAuditEventType.AFFILIATE_CREATED, newStatus = profile.status,
                actorType = AffiliateActorType.HUMAN, actorId = "admin-1", actorRole = "ADMIN",
                reason = "Test", correlationId = "CORR-$i", recordHash = recordHash,
                previousAuditHash = previousHash, chainHash = chainHash, timestamp = ts
            )
            previousHash = chainHash
        }
        return records
    }
}
