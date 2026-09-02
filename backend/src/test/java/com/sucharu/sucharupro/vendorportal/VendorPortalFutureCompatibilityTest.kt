package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalFutureCompatibilityTest {

    private lateinit var vendorDs: FakeVendorDataSource
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var portalDs: FakeVendorPortalDataSource
    private lateinit var portalRepo: VendorPortalRepositoryImpl
    private lateinit var portalService: VendorPortalService

    @Before
    fun setUp() {
        runBlocking {
            vendorDs = FakeVendorDataSource()
            vendorRepo = VendorRepositoryImpl(vendorDs)
            portalDs = FakeVendorPortalDataSource()
            portalRepo = VendorPortalRepositoryImpl(portalDs)
            portalService = VendorPortalServiceImpl(portalRepo, vendorRepo)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_future",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-FUTURE",
                    vendorName = "Future Tech Supplies",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testAiAgentReadinessAndAuthorizationContext() {
        // Authenticated AI Agent Principal acting on behalf of vendor
        val principal = AuthenticatedPrincipal(
            userId = "agent_n8n_01",
            projectId = "PROJ-ALPHA",
            username = "vendor-ai-assistant",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT,
            agentId = "agy_vendor_agent_01",
            vendorId = "vnd_future",
            permissions = setOf(
                UserPermission.READ_VENDOR_PORTAL,
                UserPermission.READ_VENDOR_PURCHASE_ORDERS,
                UserPermission.READ_VENDOR_INVOICES
            )
        )

        assertTrue(principal.isAiAgent)
        assertEquals("vnd_future", principal.effectiveVendorId)
        assertTrue(principal.hasPermission(UserPermission.READ_VENDOR_PORTAL))
        assertTrue(principal.hasPermission(UserPermission.READ_VENDOR_PURCHASE_ORDERS))
        assertFalse(principal.hasPermission(UserPermission.MANAGE_VENDOR_PORTAL_ACCESS)) // Scoped boundary
    }

    @Test
    fun testFutureRoadmapExtensionPoints() {
        runBlocking {
            val acc = portalService.createOrInviteAccount("vnd_future", "FUTURE-01", "future@sucharu.com", null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val portalAccountId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(portalAccountId, "TENANT-001", "admin_001")

            val mem = portalService.inviteVendorUser(portalAccountId, "vnd_future", "usr_future_01", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-001", "admin_001", true)

            val ctxRes = portalService.getAccessContext("usr_future_01", "vnd_future", "TENANT-001")
            assertTrue(ctxRes is DomainResult.Success)
            val context = (ctxRes as DomainResult.Success).data

            // Verify extensible features list is populated for Step 02-12
            assertNotNull(context.allowedFeatures)
            assertTrue(context.allowedFeatures.isNotEmpty())
            assertTrue(context.allowedFeatures.contains("PROFILE_VIEW")) // Step 03
            assertTrue(context.allowedFeatures.contains("RFQ")) // Step 05
            assertTrue(context.allowedFeatures.contains("PURCHASE_ORDERS")) // Step 06
            assertTrue(context.allowedFeatures.contains("INVOICES")) // Step 08
            assertTrue(context.allowedFeatures.contains("QUALITY_DISPUTES")) // Step 09
            assertTrue(context.allowedFeatures.contains("SETTLEMENT_VIEW")) // Step 12
        }
    }
}
