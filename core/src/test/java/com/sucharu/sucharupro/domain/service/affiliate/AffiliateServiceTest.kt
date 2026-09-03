package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateServiceTest {

    private lateinit var dataSource: FakeAffiliateDataSource
    private lateinit var repository: AffiliateRepositoryImpl
    private lateinit var service: AffiliateService

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

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "usr-aff-1",
        username = "affiliate_user",
        role = UserRole.AFFILIATE,
        projectId = tenantId
    )

    @Before
    fun setup() {
        dataSource = FakeAffiliateDataSource()
        repository = AffiliateRepositoryImpl(dataSource)
        service = AffiliateServiceImpl(repository)
    }

    @Test
    fun `test create affiliate profile and verify initial state, audit and outbox`() {
        runBlocking {
            val cmd = CreateAffiliateCommand(
                userId = "usr-aff-1",
                displayName = "Prime Graphic Referrals",
                affiliateCode = "PRIME_REF",
                affiliateType = AffiliateType.INDIVIDUAL,
                contactPhone = "+8801700000000",
                contactEmail = "affiliate@sucharu.pro",
                agreementReference = "AGR-2026-TERMS"
            )

            val created = service.createAffiliate(tenantId, cmd, affiliatePrincipal)
            assertNotNull(created.affiliateId)
            assertEquals(tenantId, created.tenantId)
            assertEquals("usr-aff-1", created.userId)
            assertEquals("PRIME_REF", created.affiliateCode)
            assertEquals(AffiliateStatus.PENDING, created.status)
            assertEquals(OnboardingState.SUBMITTED, created.onboardingState)
            assertTrue(created.isAgreementAccepted)

            // Verify Audit record was appended
            val audits = service.listAuditRecords(tenantId, created.affiliateId, staffPrincipal)
            assertEquals(1, audits.size)
            assertEquals(AffiliateAuditEventType.AFFILIATE_CREATED, audits[0].eventType)
            assertEquals(64, audits[0].recordHash.length)
            assertEquals(64, audits[0].chainHash.length)

            // Verify Outbox Event was appended
            val outboxEvents = dataSource.listPendingOutboxEvents(tenantId)
            assertEquals(1, outboxEvents.size)
            assertEquals("AffiliateCreated", outboxEvents[0].eventType)
            assertEquals(created.affiliateId, outboxEvents[0].aggregateId)
        }
    }

    @Test
    fun `test full lifecycle transitions PENDING to ACTIVE to SUSPENDED to REACTIVATED to TERMINATED`() {
        runBlocking {
            val cmd = CreateAffiliateCommand(
                userId = "usr-aff-2",
                displayName = "Creative Hub BD",
                affiliateType = AffiliateType.CREATOR
            )
            val created = service.createAffiliate(tenantId, cmd, staffPrincipal)
            assertEquals(AffiliateStatus.PENDING, created.status)

            // 1. Activate
            val activated = service.activateAffiliate(tenantId, created.affiliateId, "Approved by manager", managerPrincipal)
            assertEquals(AffiliateStatus.ACTIVE, activated.status)
            assertEquals(OnboardingState.APPROVED, activated.onboardingState)
            assertNotNull(activated.activatedAt)

            // Idempotent activation
            val reActivated = service.activateAffiliate(tenantId, created.affiliateId, "Duplicate call", managerPrincipal)
            assertEquals(AffiliateStatus.ACTIVE, reActivated.status)

            // 2. Suspend
            val suspended = service.suspendAffiliate(tenantId, created.affiliateId, "Under review for compliance", managerPrincipal)
            assertEquals(AffiliateStatus.SUSPENDED, suspended.status)
            assertNotNull(suspended.suspendedAt)

            // 3. Reactivate
            val reactivated = service.reactivateAffiliate(tenantId, created.affiliateId, "Compliance cleared", managerPrincipal)
            assertEquals(AffiliateStatus.ACTIVE, reactivated.status)

            // 4. Terminate (Admin only)
            val terminated = service.terminateAffiliate(tenantId, created.affiliateId, "Fraudulent activity detected", adminPrincipal)
            assertEquals(AffiliateStatus.TERMINATED, terminated.status)
            assertNotNull(terminated.terminatedAt)

            // Audit history chain verification
            val audits = service.listAuditRecords(tenantId, created.affiliateId, adminPrincipal)
            assertTrue(audits.size >= 5) // CREATE, ACTIVATE, SUSPEND, REACTIVATE, TERMINATE
        }
    }

    @Test
    fun `test accept agreement updates profile and audits`() {
        runBlocking {
            val cmd = CreateAffiliateCommand(
                userId = "usr-aff-3",
                displayName = "Design Agency",
                affiliateType = AffiliateType.BUSINESS
            )
            val created = service.createAffiliate(tenantId, cmd, staffPrincipal)
            assertNull(created.agreementAcceptedAt)

            val accepted = service.acceptAgreement(tenantId, created.affiliateId, "AGR-BIZ-2026", "v2.0", staffPrincipal)
            assertEquals("AGR-BIZ-2026", accepted.agreementReference)
            assertEquals("v2.0", accepted.agreementVersion)
            assertNotNull(accepted.agreementAcceptedAt)

            val audits = service.listAuditRecords(tenantId, created.affiliateId, staffPrincipal)
            assertTrue(audits.any { it.eventType == AffiliateAuditEventType.AGREEMENT_ACCEPTED })
        }
    }

    @Test
    fun `test governance summary aggregation`() {
        runBlocking {
            service.createAffiliate(tenantId, CreateAffiliateCommand("u1", null, "Aff 1"), staffPrincipal)
            val aff2 = service.createAffiliate(tenantId, CreateAffiliateCommand("u2", null, "Aff 2"), staffPrincipal)
            service.activateAffiliate(tenantId, aff2.affiliateId, "Approved", managerPrincipal)

            val summary = service.getGovernanceSummary(tenantId, managerPrincipal)
            assertEquals(2L, summary.totalAffiliates)
            assertEquals(1L, summary.activeAffiliates)
            assertEquals(1L, summary.pendingAffiliates)
        }
    }
}
