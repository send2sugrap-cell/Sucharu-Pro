package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import org.junit.Assert.*
import org.junit.Test

class AffiliateProgramValidationEngineTest {

    @Test
    fun `test validateProgramCode sanitizes and validates program codes`() {
        val valid1 = AffiliateProgramValidationEngine.validateProgramCode("summer_vip_2026").getOrThrow()
        assertEquals("SUMMER_VIP_2026", valid1)

        val valid2 = AffiliateProgramValidationEngine.validateProgramCode("  partner-tier1  ").getOrThrow()
        assertEquals("PARTNER-TIER1", valid2)

        assertTrue(AffiliateProgramValidationEngine.validateProgramCode("ab").isFailure) // <3 chars
        assertTrue(AffiliateProgramValidationEngine.validateProgramCode("program@name!").isFailure) // invalid special chars
    }

    @Test
    fun `test program state machine transitions`() {
        // Valid transitions
        assertTrue(AffiliateProgramValidationEngine.validateProgramStatusTransition(AffiliateProgramStatus.DRAFT, AffiliateProgramStatus.ACTIVE).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateProgramStatusTransition(AffiliateProgramStatus.DRAFT, AffiliateProgramStatus.CLOSED).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateProgramStatusTransition(AffiliateProgramStatus.ACTIVE, AffiliateProgramStatus.PAUSED).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateProgramStatusTransition(AffiliateProgramStatus.ACTIVE, AffiliateProgramStatus.CLOSED).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateProgramStatusTransition(AffiliateProgramStatus.PAUSED, AffiliateProgramStatus.ACTIVE).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateProgramStatusTransition(AffiliateProgramStatus.CLOSED, AffiliateProgramStatus.ARCHIVED).isSuccess)

        // Invalid transitions
        assertTrue(AffiliateProgramValidationEngine.validateProgramStatusTransition(AffiliateProgramStatus.CLOSED, AffiliateProgramStatus.ACTIVE).isFailure)
        assertTrue(AffiliateProgramValidationEngine.validateProgramStatusTransition(AffiliateProgramStatus.ARCHIVED, AffiliateProgramStatus.ACTIVE).isFailure)
    }

    @Test
    fun `test enrollment state machine transitions`() {
        // Valid transitions
        assertTrue(AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(AffiliateEnrollmentStatus.PENDING, AffiliateEnrollmentStatus.APPROVED).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(AffiliateEnrollmentStatus.PENDING, AffiliateEnrollmentStatus.REJECTED).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(AffiliateEnrollmentStatus.APPROVED, AffiliateEnrollmentStatus.ACTIVE).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(AffiliateEnrollmentStatus.ACTIVE, AffiliateEnrollmentStatus.SUSPENDED).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(AffiliateEnrollmentStatus.SUSPENDED, AffiliateEnrollmentStatus.ACTIVE).isSuccess)
        assertTrue(AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(AffiliateEnrollmentStatus.ACTIVE, AffiliateEnrollmentStatus.TERMINATED).isSuccess)

        // Invalid transitions
        assertTrue(AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(AffiliateEnrollmentStatus.TERMINATED, AffiliateEnrollmentStatus.ACTIVE).isFailure)
        assertTrue(AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(AffiliateEnrollmentStatus.REJECTED, AffiliateEnrollmentStatus.ACTIVE).isFailure)
    }

    @Test
    fun `test multi-criteria enrollment eligibility validation`() {
        val program = AffiliateProgram(
            programId = "PROG-01",
            tenantId = "TENANT-1",
            programCode = "SUMMER_PROMO",
            programName = "Summer Promo 2026",
            description = "Summer promotional program",
            status = AffiliateProgramStatus.ACTIVE,
            startDate = 1000L,
            endDate = 5000000000000L,
            maxParticipants = 10,
            createdBy = "ADMIN-1"
        )

        val activeEligibleAffiliate = AffiliateProfile(
            affiliateId = "AFF-01",
            tenantId = "TENANT-1",
            userId = "USR-01",
            displayName = "Top Partner",
            affiliateCode = "TOP_PARTNER",
            status = AffiliateStatus.ACTIVE,
            taxIdOrGst = "TAX-12345",
            agreementReference = "AGR-001",
            agreementAcceptedAt = 1000L,
            verificationState = VerificationState.VERIFIED
        )

        val affiliateEligibility = AffiliateValidationEngine.evaluateEligibility(activeEligibleAffiliate, "SYSTEM")

        // 1. Successful validation
        val validRes = AffiliateProgramValidationEngine.validateEnrollmentEligibility(
            program = program,
            affiliate = activeEligibleAffiliate,
            affiliateEligibility = affiliateEligibility,
            existingEnrollments = emptyList()
        )
        assertTrue(validRes.isSuccess)

        // 2. Failure: Tenant Mismatch
        val tenantMismatch = AffiliateProgramValidationEngine.validateEnrollmentEligibility(
            program = program,
            affiliate = activeEligibleAffiliate.copy(tenantId = "TENANT-2"),
            affiliateEligibility = affiliateEligibility,
            existingEnrollments = emptyList()
        )
        assertTrue(tenantMismatch.isFailure)

        // 3. Failure: Program not active
        val progNotActive = AffiliateProgramValidationEngine.validateEnrollmentEligibility(
            program = program.copy(status = AffiliateProgramStatus.PAUSED),
            affiliate = activeEligibleAffiliate,
            affiliateEligibility = affiliateEligibility,
            existingEnrollments = emptyList()
        )
        assertTrue(progNotActive.isFailure)

        // 4. Failure: Duplicate active enrollment
        val existingEnrollment = AffiliateEnrollment(
            enrollmentId = "ENR-01",
            tenantId = "TENANT-1",
            programId = "PROG-01",
            affiliateId = "AFF-01",
            enrollmentStatus = AffiliateEnrollmentStatus.ACTIVE,
            effectiveFrom = 1500L
        )
        val duplicateRes = AffiliateProgramValidationEngine.validateEnrollmentEligibility(
            program = program,
            affiliate = activeEligibleAffiliate,
            affiliateEligibility = affiliateEligibility,
            existingEnrollments = listOf(existingEnrollment)
        )
        assertTrue(duplicateRes.isFailure)
    }

    @Test
    fun `test cryptographic audit hash computation for program and enrollment`() {
        val recordHash = AffiliateProgramValidationEngine.computeRecordHash(
            tenantId = "TENANT-1",
            entityType = AffiliateProgramEntityCategory.PROGRAM,
            entityId = "PROG-01",
            eventType = AffiliateProgramAuditEventType.PROGRAM_CREATED,
            previousStatus = null,
            newStatus = "DRAFT",
            actorType = AffiliateActorType.HUMAN,
            actorId = "USR-01",
            actorRole = "MANAGER",
            timestamp = 1000L,
            correlationId = "CORR-01",
            reason = "New promotion campaign"
        )
        assertNotNull(recordHash)
        assertEquals(64, recordHash.length)

        val chainHash = AffiliateProgramValidationEngine.computeChainHash(null, recordHash)
        assertNotNull(chainHash)
        assertEquals(64, chainHash.length)
    }

    @Test
    fun `test program handoff contract synthesis`() {
        val program = AffiliateProgram(
            programId = "PROG-01",
            tenantId = "TENANT-1",
            programCode = "PARTNER_2026",
            programName = "Strategic Partners",
            description = "Strategic partnership program",
            status = AffiliateProgramStatus.ACTIVE,
            startDate = 1000L,
            createdBy = "ADMIN-1"
        )

        val affiliate = AffiliateProfile(
            affiliateId = "AFF-01",
            tenantId = "TENANT-1",
            userId = "USR-01",
            displayName = "Strategic Partner",
            affiliateCode = "PARTNER_CODE",
            status = AffiliateStatus.ACTIVE
        )

        val enrollment = AffiliateEnrollment(
            enrollmentId = "ENR-01",
            tenantId = "TENANT-1",
            programId = "PROG-01",
            affiliateId = "AFF-01",
            enrollmentStatus = AffiliateEnrollmentStatus.ACTIVE,
            effectiveFrom = 1000L
        )

        val contract = AffiliateProgramValidationEngine.buildHandoffContract(
            tenantId = "TENANT-1",
            program = program,
            affiliate = affiliate,
            enrollment = enrollment
        )

        assertEquals("v1.0.0", contract.contractVersion)
        assertTrue(contract.isReadOnly)
        assertTrue(contract.isEligibleForCommission)
        assertTrue(contract.isEligibleForAttribution)
        assertEquals("PROG-01", contract.programId)
        assertEquals("PARTNER_2026", contract.programCode)
        assertEquals("AFF-01", contract.affiliateId)
        assertEquals("PARTNER_CODE", contract.affiliateCode)
        assertTrue(contract.allowedAiActions.contains("READ_AFFILIATE_PROGRAM_DETAILS"))
        assertTrue(contract.forbiddenAiActions.contains("ACTIVATE_ENROLLMENT"))
        assertEquals(64, contract.integritySealHash.length)
    }
}
