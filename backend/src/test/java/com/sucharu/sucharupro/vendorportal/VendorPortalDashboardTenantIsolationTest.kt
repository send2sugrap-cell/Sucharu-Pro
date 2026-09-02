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

class VendorPortalDashboardTenantIsolationTest {

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

            // Tenant Alpha
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_alpha",
                    projectId = "TENANT-ALPHA",
                    vendorCode = "VND-ALPHA",
                    vendorName = "Tenant Alpha Supplies",
                    status = VendorStatus.ACTIVE
                )
            )

            // Tenant Beta
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_beta",
                    projectId = "TENANT-BETA",
                    vendorCode = "VND-BETA",
                    vendorName = "Tenant Beta Supplies",
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

            // Setup portal account in Tenant Alpha
            val acc = portalService.createOrInviteAccount("vnd_alpha", "ALPHA-01", null, null, "TENANT-ALPHA", "TENANT-ALPHA", "admin_alpha")
            val pId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(pId, "TENANT-ALPHA", "admin_alpha")

            val mem = portalService.inviteVendorUser(pId, "vnd_alpha", "usr_alpha", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-ALPHA", "admin_alpha")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-ALPHA", "admin_alpha", true)
        }
    }

    @Test
    fun testTenantIsolationForDashboard() {
        runBlocking {
            // Tenant Alpha accesses own dashboard -> OK
            val aRes = dashboardService.getDashboard("usr_alpha", "vnd_alpha", "TENANT-ALPHA")
            assertTrue(aRes is DomainResult.Success)

            // Tenant Beta tries to access Tenant Alpha vendor -> FAIL
            val bRes = dashboardService.getDashboard("usr_alpha", "vnd_alpha", "TENANT-BETA")
            assertTrue(bRes is DomainResult.Error)
        }
    }
}
