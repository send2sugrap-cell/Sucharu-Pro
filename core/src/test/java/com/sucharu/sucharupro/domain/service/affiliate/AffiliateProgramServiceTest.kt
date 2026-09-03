package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProgramDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProgramRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateProgramServiceTest {

    private lateinit var programDataSource: FakeAffiliateProgramDataSource
    private lateinit var affiliateDataSource: FakeAffiliateDataSource
    private lateinit var programRepository: AffiliateProgramRepositoryImpl
    private lateinit var affiliateRepository: AffiliateRepositoryImpl
    private lateinit var programService: AffiliateProgramService
    private lateinit var affiliateService: AffiliateService

    private val tenantId = "TENANT-ALPHA"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        username = "admin_user",
        role = UserRole.ADMIN,
        projectId = tenantId
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-1",
        username = "manager_user",
        role = UserRole.MANAGER,
        projectId = tenantId
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-1",
        username = "staff_user",
        role = UserRole.STAFF,
        projectId = tenantId
    )

    @Before
    fun setup() {
        programDataSource = FakeAffiliateProgramDataSource()
        affiliateDataSource = FakeAffiliateDataSource()
        programRepository = AffiliateProgramRepositoryImpl(programDataSource)
        affiliateRepository = AffiliateRepositoryImpl(affiliateDataSource)
        programService = AffiliateProgramServiceImpl(programRepository, affiliateRepository)
        affiliateService = AffiliateServiceImpl(affiliateRepository)
    }

    @Test
    fun `test create affiliate program and verify initial state, audit and outbox`() {
        runBlocking {
            val program = programService.createProgram(
                tenantId = tenantId,
                programCode = "SUMMER_REWARDS_2026",
                programName = "Summer Rewards 2026",
                description = "Tiered referral bonuses for printing jobs",
                startDate = System.currentTimeMillis() - 10000L,
                endDate = null,
                eligibilityPolicy = "STANDARD",
                termsReference = "TERMS-2026",
                termsVersion = "v1.0",
                maxParticipants = 50,
                actorId = managerPrincipal.userId,
                actorRole = managerPrincipal.role.name
            )
            assertNotNull(program.programId)
            assertEquals(tenantId, program.tenantId)
            assertEquals("SUMMER_REWARDS_2026", program.programCode)
            assertEquals(AffiliateProgramStatus.DRAFT, program.status)

            // Verify Audit
            val audits = programService.listProgramAuditRecords(tenantId, program.programId)
            assertEquals(1, audits.size)
            assertEquals(AffiliateProgramAuditEventType.PROGRAM_CREATED, audits[0].eventType)

            // Verify Outbox
            val outboxEvents = programDataSource.listPendingOutboxEvents(tenantId)
            assertEquals(1, outboxEvents.size)
            assertEquals("AffiliateProgramCreated", outboxEvents[0].eventType)
        }
    }

    @Test
    fun `test program lifecycle transitions DRAFT to ACTIVE to PAUSED to ACTIVE to CLOSED to ARCHIVED`() {
        runBlocking {
            val prog = programService.createProgram(
                tenantId = tenantId,
                programCode = "VIP_PRINT_PARTNERS",
                programName = "VIP Print Partners",
                description = "VIP partners program",
                startDate = System.currentTimeMillis(),
                endDate = null,
                eligibilityPolicy = "STANDARD",
                termsReference = null,
                termsVersion = null,
                maxParticipants = 100,
                actorId = managerPrincipal.userId,
                actorRole = managerPrincipal.role.name
            )
            assertEquals(AffiliateProgramStatus.DRAFT, prog.status)

            // 1. Activate
            val activeProg = programService.activateProgram(tenantId, prog.programId, managerPrincipal.userId, managerPrincipal.role.name, "Launch program")
            assertEquals(AffiliateProgramStatus.ACTIVE, activeProg.status)

            // 2. Pause
            val pausedProg = programService.pauseProgram(tenantId, prog.programId, managerPrincipal.userId, managerPrincipal.role.name, "Temporary pause")
            assertEquals(AffiliateProgramStatus.PAUSED, pausedProg.status)

            // 3. Reactivate
            val reactivatedProg = programService.activateProgram(tenantId, prog.programId, managerPrincipal.userId, managerPrincipal.role.name, "Resume program")
            assertEquals(AffiliateProgramStatus.ACTIVE, reactivatedProg.status)

            // 4. Close
            val closedProg = programService.closeProgram(tenantId, prog.programId, managerPrincipal.userId, managerPrincipal.role.name, "Campaign ended")
            assertEquals(AffiliateProgramStatus.CLOSED, closedProg.status)

            // 5. Archive (Admin only)
            val archivedProg = programService.archiveProgram(tenantId, prog.programId, adminPrincipal.userId, adminPrincipal.role.name, "Archiving record")
            assertEquals(AffiliateProgramStatus.ARCHIVED, archivedProg.status)

            // Verify Audit records
            val audits = programService.listProgramAuditRecords(tenantId, prog.programId)
            assertEquals(6, audits.size) // Created + Activate + Pause + Activate + Close + Archive
        }
    }

    @Test
    fun `test affiliate enrollment lifecycle and handoff contract`() {
        runBlocking {
            // 1. Setup Active Program
            val prog = programService.createProgram(
                tenantId = tenantId,
                programCode = "ENROLL_TEST_PROG",
                programName = "Enrollment Test Program",
                description = null,
                startDate = System.currentTimeMillis() - 5000L,
                endDate = null,
                eligibilityPolicy = "STANDARD",
                termsReference = null,
                termsVersion = null,
                maxParticipants = null,
                actorId = managerPrincipal.userId,
                actorRole = managerPrincipal.role.name
            )
            val activeProg = programService.activateProgram(tenantId, prog.programId, managerPrincipal.userId, managerPrincipal.role.name, "Launch")

            // 2. Setup Active Eligible Affiliate
            val affCmd = CreateAffiliateCommand(
                userId = "usr-aff-10",
                displayName = "Partner 10",
                affiliateType = AffiliateType.INDIVIDUAL,
                agreementReference = "AGR-001",
                agreementVersion = "v1.0"
            )
            val aff = affiliateService.createAffiliate(tenantId, affCmd, staffPrincipal)
            affiliateService.updateAffiliateProfile(
                tenantId = tenantId,
                affiliateId = aff.affiliateId,
                command = UpdateAffiliateProfileCommand(verificationState = VerificationState.VERIFIED),
                actorPrincipal = managerPrincipal
            )
            affiliateService.acceptAgreement(tenantId, aff.affiliateId, "AGR-001", "v1.0", staffPrincipal)
            val activeAff = affiliateService.activateAffiliate(tenantId, aff.affiliateId, "Approved", managerPrincipal)

            // 3. Enroll Affiliate -> PENDING
            val enrollment = programService.enrollAffiliate(
                tenantId = tenantId,
                affiliateId = activeAff.affiliateId,
                programId = activeProg.programId,
                enrollmentReason = "Direct strategic invite",
                effectiveFrom = null,
                effectiveTo = null,
                actorId = staffPrincipal.userId,
                actorRole = staffPrincipal.role.name
            )
            assertEquals(AffiliateEnrollmentStatus.PENDING, enrollment.enrollmentStatus)

            // 4. Approve Enrollment -> APPROVED
            val approved = programService.approveEnrollment(tenantId, enrollment.enrollmentId, managerPrincipal.userId, managerPrincipal.role.name, "Verified credentials")
            assertEquals(AffiliateEnrollmentStatus.APPROVED, approved.enrollmentStatus)

            // 5. Activate Enrollment -> ACTIVE
            val active = programService.activateEnrollment(tenantId, enrollment.enrollmentId, managerPrincipal.userId, managerPrincipal.role.name, "Activated")
            assertEquals(AffiliateEnrollmentStatus.ACTIVE, active.enrollmentStatus)

            // 6. Suspend Enrollment -> SUSPENDED
            val suspended = programService.suspendEnrollment(tenantId, enrollment.enrollmentId, managerPrincipal.userId, managerPrincipal.role.name, "Compliance audit")
            assertEquals(AffiliateEnrollmentStatus.SUSPENDED, suspended.enrollmentStatus)

            // 7. Resume Enrollment -> ACTIVE
            val resumed = programService.resumeEnrollment(tenantId, enrollment.enrollmentId, managerPrincipal.userId, managerPrincipal.role.name, "Audit cleared")
            assertEquals(AffiliateEnrollmentStatus.ACTIVE, resumed.enrollmentStatus)

            // 8. Downstream AI Handoff Contract
            val contract = programService.getHandoffContract(tenantId, enrollment.enrollmentId)
            assertTrue(contract.isReadOnly)
            assertTrue(contract.isEligibleForCommission)
            assertTrue(contract.isEligibleForAttribution)
            assertEquals(activeProg.programCode, contract.programCode)
            assertEquals(activeAff.affiliateCode, contract.affiliateCode)

            // 9. Terminate Enrollment -> TERMINATED
            val terminated = programService.terminateEnrollment(tenantId, enrollment.enrollmentId, managerPrincipal.userId, managerPrincipal.role.name, "Fraud detected")
            assertEquals(AffiliateEnrollmentStatus.TERMINATED, terminated.enrollmentStatus)
        }
    }

    @Test
    fun `test governance summary for programs and enrollments`() {
        runBlocking {
            val p1 = programService.createProgram(
                tenantId = tenantId,
                programCode = "PROG_ALPHA_1",
                programName = "Prog 1",
                description = null,
                startDate = 1000L,
                endDate = null,
                eligibilityPolicy = "STANDARD",
                termsReference = null,
                termsVersion = null,
                maxParticipants = null,
                actorId = managerPrincipal.userId,
                actorRole = managerPrincipal.role.name
            )
            programService.createProgram(
                tenantId = tenantId,
                programCode = "PROG_ALPHA_2",
                programName = "Prog 2",
                description = null,
                startDate = 1000L,
                endDate = null,
                eligibilityPolicy = "STANDARD",
                termsReference = null,
                termsVersion = null,
                maxParticipants = null,
                actorId = managerPrincipal.userId,
                actorRole = managerPrincipal.role.name
            )
            programService.activateProgram(tenantId, p1.programId, managerPrincipal.userId, managerPrincipal.role.name, "Launch")

            val summary = programService.getGovernanceSummary(tenantId)
            assertEquals(2L, summary.totalPrograms)
            assertEquals(1L, summary.activePrograms)
            assertEquals(0L, summary.pausedPrograms)
            assertEquals(0L, summary.closedPrograms)
            assertEquals(0L, summary.archivedPrograms)
        }
    }
}
