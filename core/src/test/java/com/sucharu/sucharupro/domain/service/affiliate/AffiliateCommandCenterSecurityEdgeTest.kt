package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateCommandCenterDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProfileDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProgramDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateCommandCenterRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProfileRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProgramRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Security edge-case test suite for Affiliate Command Center (Module 20 Step 05).
 */
class AffiliateCommandCenterSecurityEdgeTest {

    private lateinit var serviceA: AffiliateCommandCenterServiceImpl
    private lateinit var serviceB: AffiliateCommandCenterServiceImpl

    private lateinit var fakeAffDsA: FakeAffiliateDataSource
    private lateinit var fakeAffDsB: FakeAffiliateDataSource

    private val tenantA = "TENANT-ALPHA"
    private val tenantB = "TENANT-BETA"

    private val adminUserId = "admin-cc-01"
    private val aiAgentUserId = "ai-agent-cc-01"

    private lateinit var affA: AffiliateProfile

    @Before
    fun `setUp`() { runBlocking {
        fakeAffDsA = FakeAffiliateDataSource()
        fakeAffDsB = FakeAffiliateDataSource()

        fun buildService(affDs: FakeAffiliateDataSource): AffiliateCommandCenterServiceImpl {
            val ccDs = FakeAffiliateCommandCenterDataSource()
            val affRepo = AffiliateRepositoryImpl(affDs)
            val progRepo = AffiliateProgramRepositoryImpl(FakeAffiliateProgramDataSource())
            val profRepo = AffiliateProfileRepositoryImpl(FakeAffiliateProfileDataSource())
            val commRepo = AffiliateCommunicationRepositoryImpl(FakeAffiliateCommunicationDataSource())
            val ccRepo = AffiliateCommandCenterRepositoryImpl(ccDs)
            return AffiliateCommandCenterServiceImpl(
                commandCenterRepository = ccRepo,
                affiliateService = AffiliateServiceImpl(affRepo),
                programService = AffiliateProgramServiceImpl(progRepo, affRepo),
                profileService = AffiliateProfileServiceImpl(profRepo, affRepo),
                communicationService = AffiliateCommunicationServiceImpl(commRepo, affRepo)
            )
        }

        serviceA = buildService(fakeAffDsA)
        serviceB = buildService(fakeAffDsB)

        affA = AffiliateProfile(
            tenantId = tenantA,
            affiliateId = "AFF-SEC-A-01",
            userId = "USER-A-01",
            affiliateCode = "SECA01",
            displayName = "Security Test Partner A",
            status = AffiliateStatus.PENDING
        )
        fakeAffDsA.saveAffiliate(affA)
    } }

    @Test
    fun `cross-tenant work items are strictly isolated`() { runBlocking {
        serviceA.getCommandCenterOverview(tenantA)
        val itemsB = serviceB.listWorkItems(tenantB)
        assertTrue("Work items from Tenant A must not bleed into Tenant B", itemsB.isEmpty())
    } }

    @Test
    fun `cross-tenant audit records are strictly isolated`() { runBlocking {
        val items = serviceA.listWorkItems(tenantA)
        if (items.isNotEmpty()) {
            serviceA.resolveWorkItem(
                tenantId = tenantA,
                workItemId = items.first().workItemId,
                resolutionNotes = "Security isolation test",
                actorUserId = adminUserId,
                actorRole = "ADMIN",
                correlationId = "CORR-SEC-ISO-A"
            )
        }
        val auditsB = serviceB.listAuditRecords(tenantB)
        assertTrue("Audit records from Tenant A must not bleed into Tenant B", auditsB.isEmpty())
    } }

    @Test
    fun `cross-tenant handoff contract reflects only own tenant data`() { runBlocking {
        val contractA = serviceA.getHandoffContract(tenantA, adminUserId)
        val contractB = serviceB.getHandoffContract(tenantB, adminUserId)
        assertEquals(tenantA, contractA.tenantId)
        assertEquals(tenantB, contractB.tenantId)
        assertEquals(1L, contractA.totalAffiliates)
        assertEquals(0L, contractB.totalAffiliates)
    } }

    @Test
    fun `executeAdminAction on phantom affiliate throws NoSuchElementException`() { runBlocking {
        try {
            serviceA.executeAdminAction(
                tenantId = tenantA,
                affiliateId = "PHANTOM-9999",
                action = "APPROVE",
                reason = "Should not succeed",
                actorUserId = adminUserId,
                actorRole = "ADMIN",
                correlationId = "CORR-PHANTOM"
            )
            fail("Expected NoSuchElementException for phantom affiliate")
        } catch (e: NoSuchElementException) {
            assertNotNull(e.message)
        }
    } }

    @Test
    fun `getAdministrativeDetailView on phantom affiliate throws NoSuchElementException`() { runBlocking {
        try {
            serviceA.getAdministrativeDetailView(tenantA, "PHANTOM-AFF-8888", adminUserId)
            fail("Expected NoSuchElementException")
        } catch (e: NoSuchElementException) {
            assertNotNull(e.message)
        }
    } }

    @Test
    fun `resolveWorkItem with ghost workItemId throws NoSuchElementException`() { runBlocking {
        try {
            serviceA.resolveWorkItem(
                tenantId = tenantA,
                workItemId = "WI-GHOST-9999",
                resolutionNotes = "Phantom resolve",
                actorUserId = adminUserId,
                actorRole = "ADMIN",
                correlationId = "CORR-GHOST"
            )
            fail("Expected NoSuchElementException for ghost work item")
        } catch (e: NoSuchElementException) {
            assertNotNull(e.message)
        }
    } }

    @Test
    fun `handoff contract is readable and exposes governance forbidden action list`() { runBlocking {
        val contract = serviceA.getHandoffContract(tenantA, aiAgentUserId)
        assertNotNull(contract)
        assertEquals(tenantA, contract.tenantId)
        assertTrue(contract.isReadOnly)
        assertTrue(contract.forbiddenActions.contains("BYPASS_LIFECYCLE_GOVERNANCE"))
        assertTrue(contract.forbiddenActions.contains("BYPASS_TENANT_ISOLATION"))
        assertTrue(contract.forbiddenActions.contains("BYPASS_RBAC"))
        assertTrue(contract.forbiddenActions.contains("CALCULATE_COMMISSION"))
        assertTrue(contract.forbiddenActions.contains("ISSUE_PAYOUT"))
    } }

    @Test
    fun `Audit records faithfully capture ADMIN actor identity after work item resolution`() { runBlocking {
        val items = serviceA.listWorkItems(tenantA)
        if (items.isNotEmpty()) {
            serviceA.resolveWorkItem(
                tenantId = tenantA,
                workItemId = items.first().workItemId,
                resolutionNotes = "Audit fidelity test",
                actorUserId = adminUserId,
                actorRole = "ADMIN",
                correlationId = "CORR-FIDELITY"
            )
            val audits = serviceA.listAuditRecords(tenantA)
            val audit = audits.first { it.actorUserId == adminUserId }
            assertEquals(adminUserId, audit.actorUserId)
            assertEquals("ADMIN", audit.actorRole)
            assertNotNull(audit.recordHash)
            assertNotNull(audit.chainHash)
        }
    } }

    @Test
    fun `Audit records capture AI_AGENT actor type for AI-driven lifecycle mutations`() { runBlocking {
        serviceA.executeAdminAction(
            tenantId = tenantA,
            affiliateId = affA.affiliateId,
            action = "APPROVE",
            reason = "AI-driven approval",
            actorUserId = aiAgentUserId,
            actorRole = "ADMIN",
            actorType = AffiliateActorType.AI_AGENT,
            correlationId = "CORR-AI-AUDIT"
        )
        val audits = serviceA.listAuditRecords(tenantA, affA.affiliateId)
        val aiAudit = audits.firstOrNull { it.actorUserId == aiAgentUserId }
        assertNotNull("AI agent audit record must be present", aiAudit)
        assertEquals(AffiliateActorType.AI_AGENT, aiAudit!!.actorType)
        assertEquals("ADMIN", aiAudit.actorRole)
    } }

    @Test
    fun `sequential resolutions produce incrementally chained and distinct audit hashes`() { runBlocking {
        val items = serviceA.listWorkItems(tenantA)
        if (items.isNotEmpty()) {
            serviceA.resolveWorkItem(
                tenantId = tenantA,
                workItemId = items[0].workItemId,
                resolutionNotes = "First resolution",
                actorUserId = adminUserId,
                actorRole = "ADMIN",
                correlationId = "CORR-CHAIN-1"
            )
            val chainHash1 = serviceA.listAuditRecords(tenantA).last().chainHash

            serviceA.executeAdminAction(
                tenantId = tenantA,
                affiliateId = affA.affiliateId,
                action = "APPROVE",
                reason = "Second chained action",
                actorUserId = adminUserId,
                actorRole = "ADMIN",
                correlationId = "CORR-CHAIN-2"
            )
            val chainHash2 = serviceA.listAuditRecords(tenantA).last().chainHash

            assertNotEquals("Sequential audit entries must produce distinct chain hashes", chainHash1, chainHash2)
        }
    } }
}
