package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalDashboardRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccessPolicy
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDashboardService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalDashboardSecurityEdgeTest {

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
                    vendorId = "vnd_sec",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-SEC",
                    vendorName = "Security Edge Vendor",
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

            val acc = portalService.createOrInviteAccount("vnd_sec", "SEC-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val pId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(pId, "TENANT-001", "admin_001")

            val mem = portalService.inviteVendorUser(pId, "vnd_sec", "usr_sec", VendorPortalRole.VENDOR_OPERATOR, "*", "TENANT-001", "admin_001")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-001", "admin_001", true)
        }
    }

    @Test
    fun testRestrictedRoleCannotAccessDirectFinancialSummary() {
        runBlocking {
            // Operator is NOT authorized to view financial summaries
            val finRes = dashboardService.getFinancialSummary("usr_sec", "vnd_sec", "TENANT-001")
            assertTrue(finRes is DomainResult.Error)
            assertTrue((finRes as DomainResult.Error).exception is SecurityException)
        }
    }

    @Test
    fun testIpWhitelistViolationBlocksDashboardAccess() {
        runBlocking {
            // Set restrictive IP policy
            portalService.savePolicy(
                VendorPortalAccessPolicy(
                    policyId = "pol_ip",
                    tenantId = "TENANT-001",
                    projectId = "PROJ-ALPHA",
                    vendorId = "vnd_sec",
                    ipWhitelist = "192.168.1.100"
                ),
                tenantId = "TENANT-001",
                actorId = "admin_001"
            )

            // Access from unauthorized IP
            val dRes = dashboardService.getDashboard("usr_sec", "vnd_sec", "TENANT-001", clientIp = "10.0.0.1")
            assertTrue(dRes is DomainResult.Error)
            assertTrue((dRes as DomainResult.Error).exception is SecurityException)
        }
    }
}
