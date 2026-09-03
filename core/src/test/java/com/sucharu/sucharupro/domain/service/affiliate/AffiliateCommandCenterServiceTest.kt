package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.*
import com.sucharu.sucharupro.data.repository.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateCommandCenterServiceTest {

    private lateinit var commandCenterService: AffiliateCommandCenterService
    private lateinit var affiliateService: AffiliateService
    private lateinit var programService: AffiliateProgramService
    private lateinit var profileService: AffiliateProfileService
    private lateinit var communicationService: AffiliateCommunicationService

    private val tenantId = "TENANT-CC-TEST"
    private val adminUserId = "USER-ADMIN-01"

    @Before
    fun setUp() {
        val ccDs = FakeAffiliateCommandCenterDataSource()
        val ccRepo = AffiliateCommandCenterRepositoryImpl(ccDs)

        val affDs = FakeAffiliateDataSource()
        val affRepo = AffiliateRepositoryImpl(affDs)
        affiliateService = AffiliateServiceImpl(affRepo)

        val progDs = FakeAffiliateProgramDataSource()
        val progRepo = AffiliateProgramRepositoryImpl(progDs)
        programService = AffiliateProgramServiceImpl(progRepo, affRepo)

        val profDs = FakeAffiliateProfileDataSource()
        val profRepo = AffiliateProfileRepositoryImpl(profDs)
        profileService = AffiliateProfileServiceImpl(profRepo, affRepo)

        val commDs = FakeAffiliateCommunicationDataSource()
        val commRepo = AffiliateCommunicationRepositoryImpl(commDs)
        communicationService = AffiliateCommunicationServiceImpl(commRepo, affRepo)

        commandCenterService = AffiliateCommandCenterServiceImpl(
            commandCenterRepository = ccRepo,
            affiliateService = affiliateService,
            programService = programService,
            profileService = profileService,
            communicationService = communicationService
        )
    }

    @Test
    fun testGetCommandCenterOverview_ReturnsCorrectStateCounts() = runBlocking {
        // Create 2 affiliates: 1 pending, 1 active
        val aff1 = affiliateService.createAffiliate(tenantId = tenantId, command = CreateAffiliateCommand(userId = "USER-AFF-1", displayName = "Partner Alpha", contactEmail = "alpha@partner.com"), actorPrincipal = AuthenticatedPrincipal("USER-AFF-1", tenantId, "USER-AFF-1", com.sucharu.sucharupro.data.api.model.UserRole.ADMIN))
        val aff2 = affiliateService.createAffiliate(tenantId = tenantId, command = CreateAffiliateCommand(userId = "USER-AFF-2", displayName = "Partner Beta", contactEmail = "beta@partner.com"), actorPrincipal = AuthenticatedPrincipal("USER-AFF-2", tenantId, "USER-AFF-2", com.sucharu.sucharupro.data.api.model.UserRole.ADMIN))
        affiliateService.activateAffiliate(tenantId = tenantId, affiliateId = aff2.affiliateId, reason = "Approved by admin", actorPrincipal = AuthenticatedPrincipal(adminUserId, tenantId, "Admin", com.sucharu.sucharupro.data.api.model.UserRole.ADMIN))

        val overview = commandCenterService.getCommandCenterOverview(tenantId)
        assertEquals(tenantId, overview.tenantId)
        assertEquals(2L, overview.totalAffiliates)
        assertEquals(1L, overview.pendingReviewCount)
        assertEquals(1L, overview.activeCount)
        assertEquals(0L, overview.suspendedCount)
        assertTrue(overview.openWorkItemsCount >= 1L)
    }

    @Test
    fun testListWorkItems_FiltersByPriorityAndStatus() = runBlocking {
        affiliateService.createAffiliate(tenantId = tenantId, command = CreateAffiliateCommand(userId = "USER-AFF-3", displayName = "Partner Gamma", contactEmail = "gamma@partner.com"), actorPrincipal = AuthenticatedPrincipal("USER-AFF-3", tenantId, "USER-AFF-3", com.sucharu.sucharupro.data.api.model.UserRole.ADMIN))

        val items = commandCenterService.listWorkItems(tenantId)
        assertTrue(items.isNotEmpty())

        val filtered = commandCenterService.listWorkItems(
            tenantId = tenantId,
            priority = AffiliateGovernanceWorkItemPriority.HIGH
        )
        assertTrue(filtered.all { it.priority == AffiliateGovernanceWorkItemPriority.HIGH })
    }

    @Test
    fun testResolveWorkItem_UpdatesStatusAndCreatesAudit() = runBlocking {
        val aff = affiliateService.createAffiliate(tenantId = tenantId, command = CreateAffiliateCommand(userId = "USER-AFF-4", displayName = "Partner Delta", contactEmail = "delta@partner.com"), actorPrincipal = AuthenticatedPrincipal("USER-AFF-4", tenantId, "USER-AFF-4", com.sucharu.sucharupro.data.api.model.UserRole.ADMIN))

        val items = commandCenterService.listWorkItems(tenantId, affiliateId = aff.affiliateId)
        val target = items.first()

        val resolved = commandCenterService.resolveWorkItem(
            tenantId = tenantId,
            workItemId = target.workItemId,
            resolutionNotes = "Reviewed document manually",
            status = AffiliateGovernanceWorkItemStatus.RESOLVED,
            actorUserId = adminUserId,
            actorRole = "ADMIN",
            correlationId = "CORR-TEST-RES"
        )

        assertEquals(AffiliateGovernanceWorkItemStatus.RESOLVED, resolved.status)
        assertEquals("Reviewed document manually", resolved.resolutionNotes)

        val audits = commandCenterService.listAuditRecords(tenantId, aff.affiliateId)
        assertTrue(audits.any { it.action == "WORK_ITEM_RESOLVED" })
    }

    @Test
    fun testExecuteAdminAction_ApproveAndSuspend_UpdatesStatusAndDispatchesNotification() = runBlocking {
        val aff = affiliateService.createAffiliate(tenantId = tenantId, command = CreateAffiliateCommand(userId = "USER-AFF-5", displayName = "Partner Epsilon", contactEmail = "epsilon@partner.com"), actorPrincipal = AuthenticatedPrincipal("USER-AFF-5", tenantId, "USER-AFF-5", com.sucharu.sucharupro.data.api.model.UserRole.ADMIN))

        // Admin action: APPROVE
        val approved = commandCenterService.executeAdminAction(
            tenantId = tenantId,
            affiliateId = aff.affiliateId,
            action = "APPROVE",
            reason = "Fully verified business identity",
            actorUserId = adminUserId,
            actorRole = "ADMIN",
            correlationId = "CORR-APPROVE"
        )
        assertEquals(AffiliateStatus.ACTIVE, approved.status)

        // Admin action: SUSPEND
        val suspended = commandCenterService.executeAdminAction(
            tenantId = tenantId,
            affiliateId = aff.affiliateId,
            action = "SUSPEND",
            reason = "Suspicious traffic flag raised",
            actorUserId = adminUserId,
            actorRole = "ADMIN",
            correlationId = "CORR-SUSPEND"
        )
        assertEquals(AffiliateStatus.SUSPENDED, suspended.status)

        // Check notifications dispatched via Step 04
        val comms = communicationService.listCommunications(tenantId, aff.affiliateId)
        assertTrue(comms.size >= 2)
    }

    @Test
    fun testGetAdministrativeDetailView_ConsolidatesAllStepData() = runBlocking {
        val aff = affiliateService.createAffiliate(tenantId = tenantId, command = CreateAffiliateCommand(userId = "USER-AFF-6", displayName = "Partner Zeta", contactEmail = "zeta@partner.com"), actorPrincipal = AuthenticatedPrincipal("USER-AFF-6", tenantId, "USER-AFF-6", com.sucharu.sucharupro.data.api.model.UserRole.ADMIN))

        val detail = commandCenterService.getAdministrativeDetailView(tenantId, aff.affiliateId, adminUserId)
        assertEquals(aff.affiliateId, detail.affiliateId)
        assertEquals("Partner Zeta", detail.identityProfile.displayName)
        assertNotNull(detail.eligibility)
        assertNotNull(detail.handoffContract)
    }
}

