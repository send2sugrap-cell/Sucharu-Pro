package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalDashboardRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDashboardService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalDashboardServiceTest {

    private lateinit var vendorDs: FakeVendorDataSource
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var portalDs: FakeVendorPortalDataSource
    private lateinit var portalRepo: VendorPortalRepositoryImpl
    private lateinit var portalService: VendorPortalService
    private lateinit var dashboardRepo: VendorPortalDashboardRepositoryImpl
    private lateinit var dashboardService: VendorPortalDashboardService

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
                    vendorId = "vnd_001",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-001",
                    vendorName = "Precision Polymers",
                    status = VendorStatus.ACTIVE
                )
            )

            dashboardRepo = VendorPortalDashboardRepositoryImpl(
                vendorRepository = vendorRepo,
                portalRepository = portalRepo
            )

            dashboardService = VendorPortalDashboardServiceImpl(
                portalService = portalService,
                dashboardRepository = dashboardRepo
            )

            // Setup portal account & membership
            val acc = portalService.createOrInviteAccount("vnd_001", "PRECISION-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val pId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(pId, "TENANT-001", "admin_001")

            val mem = portalService.inviteVendorUser(pId, "vnd_001", "usr_dash_01", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-001", "admin_001", true)
        }
    }

    @Test
    fun testGetDashboardReturnsFullAggregateStructure() {
        runBlocking {
            val dRes = dashboardService.getDashboard("usr_dash_01", "vnd_001", "TENANT-001")
            assertTrue(dRes is DomainResult.Success)
            val dashboard = (dRes as DomainResult.Success).data

            assertEquals("vnd_001", dashboard.vendorId)
            assertEquals("VND-001", dashboard.vendorCode)
            assertEquals("Precision Polymers", dashboard.vendorName)
            assertEquals(VendorPortalRole.VENDOR_ADMIN, dashboard.portalRole)
            assertNotNull(dashboard.profile)
            assertNotNull(dashboard.featureVisibility)
            assertTrue(dashboard.navigationItems.isNotEmpty())
        }
    }

    @Test
    fun testGetProfileReturnsAccurateProfileSummary() {
        runBlocking {
            val pRes = dashboardService.getProfile("usr_dash_01", "vnd_001", "TENANT-001")
            assertTrue(pRes is DomainResult.Success)
            val profile = (pRes as DomainResult.Success).data
            assertEquals("vnd_001", profile.vendorId)
            assertEquals("Precision Polymers", profile.vendorName)
            assertEquals("VENDOR_ADMIN", profile.portalRole)
        }
    }

    @Test
    fun testGetWorkspaceReturnsNavigationAndProfile() {
        runBlocking {
            val wRes = dashboardService.getWorkspace("usr_dash_01", "vnd_001", "TENANT-001")
            assertTrue(wRes is DomainResult.Success)
            val workspace = (wRes as DomainResult.Success).data
            assertEquals("vnd_001", workspace.vendorId)
            assertEquals("usr_dash_01", workspace.userId)
            assertTrue(workspace.navigationItems.any { it.id == "nav_dashboard" })
            assertTrue(workspace.navigationItems.any { it.id == "nav_profile" })
        }
    }
}
