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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalDashboardConcurrencyTest {

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
                    vendorId = "vnd_conc",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-CONC",
                    vendorName = "Concurrent Vendor",
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

            val acc = portalService.createOrInviteAccount("vnd_conc", "CONC-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val pId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(pId, "TENANT-001", "admin_001")

            val mem = portalService.inviteVendorUser(pId, "vnd_conc", "usr_conc", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-001", "admin_001", true)
        }
    }

    @Test
    fun testConcurrentDashboardLoadsPerformSafely() {
        runBlocking {
            val deferreds = (1..20).map {
                async {
                    dashboardService.getDashboard("usr_conc", "vnd_conc", "TENANT-001")
                }
            }
            val results = deferreds.awaitAll()
            assertEquals(20, results.size)
            assertTrue(results.all { it is DomainResult.Success })
        }
    }
}
