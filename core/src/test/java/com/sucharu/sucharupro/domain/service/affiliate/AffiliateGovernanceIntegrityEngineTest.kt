package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests for AffiliateGovernanceIntegrityEngine (Module 20 Step 06).
 *
 * Tests the deterministic logic of:
 *  1. Cross-step lifecycle integrity checks
 *  2. Integration readiness state computation
 *  3. SHA-256 audit chain verification
 *  4. Final handoff contract synthesis
 */
class AffiliateGovernanceIntegrityEngineTest {

    private val tenantId = "TENANT-ALPHA"
    private val affiliateId = "AFF-TEST-01"

    private fun baseProfile(
        status: AffiliateStatus = AffiliateStatus.ACTIVE,
        verificationState: VerificationState = VerificationState.VERIFIED,
        affiliateType: AffiliateType = AffiliateType.INDIVIDUAL,
        agreementReference: String? = "AGR-2026",
        agreementAcceptedAt: Long? = System.currentTimeMillis(),
        taxIdOrGst: String? = null
    ) = AffiliateProfile(
        affiliateId = affiliateId,
        tenantId = tenantId,
        userId = "usr-test-1",
        displayName = "Test Affiliate",
        affiliateCode = "TEST_AFF",
        status = status,
        affiliateType = affiliateType,
        verificationState = verificationState,
        onboardingState = OnboardingState.APPROVED,
        agreementReference = agreementReference,
        agreementAcceptedAt = agreementAcceptedAt,
        taxIdOrGst = taxIdOrGst,
        joinedAt = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    private fun baseEligibility(isEligible: Boolean = true) = AffiliateEligibility(
        eligibilityId = "ELIG-01",
        tenantId = tenantId,
        affiliateId = affiliateId,
        isEligible = isEligible,
        identityVerified = isEligible,
        agreementAccepted = isEligible,
        accountActive = isEligible,
        taxCompliant = isEligible,
        businessVerified = isEligible,
        evaluatedBy = "admin-1"
    )

    // ─────────────────────────────────────────────────────────────────
    // 1. Lifecycle Integrity Checks
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `integrity check passes for a fully valid active affiliate`() {
        val profile = baseProfile()
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = baseEligibility(),
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            communications = emptyList(),
            auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        assertTrue(result.isIntegrityValid)
        assertEquals(0, result.criticalCount)
        assertEquals(0, result.highCount)
    }

    @Test
    fun `CRITICAL violation when ACTIVE affiliate has unverified identity`() {
        val profile = baseProfile(verificationState = VerificationState.UNVERIFIED)
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = null,
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            communications = emptyList(),
            auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        assertFalse(result.isIntegrityValid)
        assertTrue(result.criticalCount >= 1)
        assertTrue(result.violations.any { it.code == "S01_ACTIVE_WITHOUT_VERIFIED_IDENTITY" })
    }

    @Test
    fun `CRITICAL violation when ACTIVE affiliate has no agreement accepted`() {
        val profile = baseProfile(agreementReference = null, agreementAcceptedAt = null)
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = null,
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            communications = emptyList(),
            auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        assertFalse(result.isIntegrityValid)
        assertTrue(result.violations.any { it.code == "S01_ACTIVE_WITHOUT_AGREEMENT" })
    }

    @Test
    fun `HIGH violation when agreement timestamp present but reference is blank`() {
        val profile = baseProfile(agreementReference = "", agreementAcceptedAt = System.currentTimeMillis())
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = null,
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            communications = emptyList(),
            auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        assertFalse(result.isIntegrityValid)
        assertTrue(result.violations.any { it.code == "S01_AGREEMENT_TIMESTAMP_WITHOUT_REFERENCE" })
    }

    @Test
    fun `HIGH violation when ACTIVE BUSINESS affiliate lacks tax ID`() {
        val profile = baseProfile(
            affiliateType = AffiliateType.BUSINESS,
            taxIdOrGst = null
        )
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = null,
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            communications = emptyList(),
            auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        assertFalse(result.isIntegrityValid)
        assertTrue(result.violations.any { it.code == "S01_BUSINESS_MISSING_TAX_ID" })
    }

    @Test
    fun `HIGH violation when ACTIVE enrollment exists with non-ACTIVE affiliate`() {
        val profile = baseProfile(status = AffiliateStatus.SUSPENDED)
        val enrollment = AffiliateEnrollment(
            enrollmentId = "ENR-01",
            tenantId = tenantId,
            affiliateId = affiliateId,
            programId = "PROG-01",
            enrollmentStatus = AffiliateEnrollmentStatus.ACTIVE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = null,
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = listOf(enrollment),
            communications = emptyList(),
            auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        assertFalse(result.isIntegrityValid)
        assertTrue(result.violations.any { it.code == "S02_ACTIVE_ENROLLMENT_WITH_NON_ACTIVE_AFFILIATE" })
    }

    @Test
    fun `CRITICAL violation when no audit records but affiliate is beyond PENDING`() {
        val profile = baseProfile(status = AffiliateStatus.ACTIVE)
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = null,
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            communications = emptyList(),
            auditRecords = emptyList(), // No audits!
            checkedBy = "admin-1"
        )
        assertFalse(result.isIntegrityValid)
        assertTrue(result.violations.any { it.code == "S01_MISSING_AUDIT_TRAIL" })
    }

    @Test
    fun `integrity check correctly counts violation severities`() {
        val profile = baseProfile(
            verificationState = VerificationState.UNVERIFIED, // CRITICAL
            affiliateType = AffiliateType.BUSINESS,
            taxIdOrGst = null                                   // HIGH
        )
        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = null,
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            communications = emptyList(),
            auditRecords = emptyList(), // CRITICAL (no audit trail)
            checkedBy = "admin-1"
        )
        assertTrue(result.criticalCount >= 2) // ACTIVE_WITHOUT_VERIFIED + MISSING_AUDIT_TRAIL
        assertTrue(result.highCount >= 1)     // BUSINESS_MISSING_TAX_ID
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. Integration Readiness State
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `fully eligible active affiliate is ready for attribution and commission`() {
        val profile = baseProfile()
        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile,
            eligibility = baseEligibility(true),
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            workItems = emptyList(),
            notificationPreferences = emptyList(),
            assessedBy = "admin-1"
        )
        assertTrue(readiness.isReadyForAttribution)
        assertTrue(readiness.isReadyForCommission)
        assertFalse(readiness.isReadyForPayout) // No operational profile
        assertTrue(readiness.isReadyForAnalytics)
        assertTrue(readiness.readinessScore > 0)
        assertNotNull(readiness.integrityHash)
    }

    @Test
    fun `PENDING affiliate is not ready for attribution or commission`() {
        val profile = baseProfile(status = AffiliateStatus.PENDING)
        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile,
            eligibility = baseEligibility(false),
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            workItems = emptyList(),
            notificationPreferences = emptyList(),
            assessedBy = "admin-1"
        )
        assertFalse(readiness.isReadyForAttribution)
        assertFalse(readiness.isReadyForCommission)
        assertFalse(readiness.isReadyForPayout)
    }

    @Test
    fun `readiness score increases with each satisfied gate`() {
        val low = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = baseProfile(status = AffiliateStatus.PENDING),
            eligibility = baseEligibility(false),
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            workItems = emptyList(),
            notificationPreferences = emptyList(),
            assessedBy = "sys"
        )
        val high = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = baseProfile(),
            eligibility = baseEligibility(true),
            operationalProfile = null,
            verifications = emptyList(),
            enrollments = emptyList(),
            workItems = emptyList(),
            notificationPreferences = emptyList(),
            assessedBy = "sys"
        )
        assertTrue(high.readinessScore > low.readinessScore)
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. Audit Chain Verification
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `audit chain verification passes on a valid hash chain`() {
        val profile = baseProfile()
        val records = buildAuditChain(profile, 3)
        val result = AffiliateGovernanceIntegrityEngine.verifyAuditChainIntegrity(
            tenantId = tenantId,
            affiliateId = affiliateId,
            records = records
        )
        assertTrue(result.isChainIntact)
        assertEquals(3, result.totalRecordsChecked)
        assertNull(result.firstTamperedAuditId)
    }

    @Test
    fun `audit chain verification detects tampered record hash`() {
        val profile = baseProfile()
        val records = buildAuditChain(profile, 2).toMutableList()

        // Tamper: corrupt the chainHash of the second record
        val tampered = records[1].copy(chainHash = "0000000000000000000000000000000000000000000000000000000000000000")
        records[1] = tampered

        val result = AffiliateGovernanceIntegrityEngine.verifyAuditChainIntegrity(
            tenantId = tenantId,
            affiliateId = affiliateId,
            records = records
        )
        assertFalse(result.isChainIntact)
        assertNotNull(result.firstTamperedAuditId)
        assertEquals(1, result.firstTamperedIndex)
    }

    @Test
    fun `audit chain verification returns intact on empty records`() {
        val result = AffiliateGovernanceIntegrityEngine.verifyAuditChainIntegrity(
            tenantId = tenantId,
            affiliateId = affiliateId,
            records = emptyList()
        )
        assertTrue(result.isChainIntact)
        assertEquals(0, result.totalRecordsChecked)
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. Final Handoff Contract Synthesis
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `final handoff contract is read-only and has correct version`() {
        val profile = baseProfile()
        val eligibility = baseEligibility(true)
        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile, eligibility = eligibility,
            operationalProfile = null, verifications = emptyList(),
            enrollments = emptyList(), workItems = emptyList(),
            notificationPreferences = emptyList(), assessedBy = "admin-1"
        )
        val integrityResult = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile, eligibility = eligibility, operationalProfile = null,
            verifications = emptyList(), enrollments = emptyList(),
            communications = emptyList(), auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        val contract = AffiliateGovernanceIntegrityEngine.synthesizeFinalHandoffContract(
            profile = profile, eligibility = eligibility,
            integrationReadiness = readiness, integrityResult = integrityResult
        )
        assertEquals("v20.06", contract.contractVersion)
        assertTrue(contract.isReadOnly)
        assertFalse(contract.integritySealHash.isBlank())
    }

    @Test
    fun `final handoff contract forbids all mutation AI actions`() {
        val profile = baseProfile()
        val eligibility = baseEligibility(true)
        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile, eligibility = eligibility,
            operationalProfile = null, verifications = emptyList(),
            enrollments = emptyList(), workItems = emptyList(),
            notificationPreferences = emptyList(), assessedBy = "admin-1"
        )
        val integrityResult = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile, eligibility = eligibility, operationalProfile = null,
            verifications = emptyList(), enrollments = emptyList(),
            communications = emptyList(), auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        val contract = AffiliateGovernanceIntegrityEngine.synthesizeFinalHandoffContract(
            profile = profile, eligibility = eligibility,
            integrationReadiness = readiness, integrityResult = integrityResult
        )
        val forbidden = contract.forbiddenAiActions
        assertTrue(forbidden.contains("ACTIVATE_AFFILIATE"))
        assertTrue(forbidden.contains("SUSPEND_AFFILIATE"))
        assertTrue(forbidden.contains("TERMINATE_AFFILIATE"))
        assertTrue(forbidden.contains("ALTER_AUDIT_CHAIN"))
        assertTrue(forbidden.contains("BYPASS_RLS"))
    }

    @Test
    fun `final handoff contract does not expose module 21-24 business logic`() {
        val profile = baseProfile()
        val eligibility = baseEligibility(true)
        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile, eligibility = eligibility,
            operationalProfile = null, verifications = emptyList(),
            enrollments = emptyList(), workItems = emptyList(),
            notificationPreferences = emptyList(), assessedBy = "admin-1"
        )
        val integrityResult = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile, eligibility = eligibility, operationalProfile = null,
            verifications = emptyList(), enrollments = emptyList(),
            communications = emptyList(), auditRecords = buildAuditChain(profile, 1),
            checkedBy = "admin-1"
        )
        val contract = AffiliateGovernanceIntegrityEngine.synthesizeFinalHandoffContract(
            profile = profile, eligibility = eligibility,
            integrationReadiness = readiness, integrityResult = integrityResult
        )
        // Module 21–24 readiness flags exist but no commission/payout amounts or attribution logic
        assertNotNull(contract.isReadyForAttribution)
        assertNotNull(contract.isReadyForCommission)
        assertNotNull(contract.isReadyForPayout)
        assertNotNull(contract.isReadyForAnalytics)
    }

    @Test
    fun `integrity result validity gates downstream readiness in handoff contract`() {
        val profile = baseProfile(verificationState = VerificationState.UNVERIFIED)
        val eligibility = baseEligibility(false)
        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile, eligibility = eligibility,
            operationalProfile = null, verifications = emptyList(),
            enrollments = emptyList(), workItems = emptyList(),
            notificationPreferences = emptyList(), assessedBy = "admin-1"
        )
        val integrityResult = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile, eligibility = eligibility, operationalProfile = null,
            verifications = emptyList(), enrollments = emptyList(),
            communications = emptyList(), auditRecords = emptyList(),
            checkedBy = "admin-1"
        )
        val contract = AffiliateGovernanceIntegrityEngine.synthesizeFinalHandoffContract(
            profile = profile, eligibility = eligibility,
            integrationReadiness = readiness, integrityResult = integrityResult
        )
        // integrity is invalid → attribution and commission must be blocked
        assertFalse(contract.isReadyForAttribution)
        assertFalse(contract.isReadyForCommission)
        assertFalse(contract.isReadyForPayout)
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private fun buildAuditChain(profile: AffiliateProfile, count: Int): List<AffiliateAuditRecord> {
        val records = mutableListOf<AffiliateAuditRecord>()
        var previousHash: String? = null
        repeat(count) { i ->
            val ts = System.currentTimeMillis() + i
            val recordHash = AffiliateValidationEngine.computeRecordHash(
                tenantId = tenantId,
                affiliateId = affiliateId,
                eventType = AffiliateAuditEventType.AFFILIATE_CREATED,
                previousStatus = null,
                newStatus = profile.status,
                actorType = AffiliateActorType.HUMAN,
                actorId = "admin-1",
                actorRole = "ADMIN",
                timestamp = ts,
                correlationId = "CORR-$i",
                reason = "Test reason $i"
            )
            val chainHash = AffiliateValidationEngine.computeChainHash(previousHash, recordHash)
            val record = AffiliateAuditRecord(
                auditId = "AUD-TEST-$i",
                tenantId = tenantId,
                affiliateId = affiliateId,
                eventType = AffiliateAuditEventType.AFFILIATE_CREATED,
                newStatus = profile.status,
                actorType = AffiliateActorType.HUMAN,
                actorId = "admin-1",
                actorRole = "ADMIN",
                reason = "Test reason $i",
                correlationId = "CORR-$i",
                recordHash = recordHash,
                previousAuditHash = previousHash,
                chainHash = chainHash,
                timestamp = ts
            )
            records += record
            previousHash = chainHash
        }
        return records
    }
}
