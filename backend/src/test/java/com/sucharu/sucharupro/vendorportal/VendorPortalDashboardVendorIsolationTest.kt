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

class VendorPortalDashboardVendorIsolationTest {

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
                    vendorId = "vnd_a",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-A",
                    vendorName = "Vendor Alpha",
                    status = VendorStatus.ACTIVE
                )
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_b",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-B",
                    vendorName = "Vendor Beta",
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

            // Setup Vendor A portal
            val accA = portalService.createOrInviteAccount("vnd_a", "VND-A-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val pIdA = (accA as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(pIdA, "TENANT-001", "admin_001")
            val memA = portalService.inviteVendorUser(pIdA, "vnd_a", "usr_alice", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val tokenA = (memA as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(tokenA, "TENANT-001", "admin_001", true)

            // Setup Vendor B portal
            val accB = portalService.createOrInviteAccount("vnd_b", "VND-B-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val pIdB = (accB as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(pIdB, "TENANT-001", "admin_001")
            val memB = portalService.inviteVendorUser(pIdB, "vnd_b", "usr_bob", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val tokenB = (memB as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(tokenB, "TENANT-001", "admin_001", true)
        }
    }

    @Test
    fun testUserCannotAccessOtherVendorDashboard() {
        runBlocking {
            // Alice accessing Vendor A -> OK
            val aRes = dashboardService.getDashboard("usr_alice", "vnd_a", "TENANT-001")
            assertTrue(aRes is DomainResult.Success)

            // Alice attempting to access Vendor B dashboard -> MUST FAIL
            val bRes = dashboardService.getDashboard("usr_alice", "vnd_b", "TENANT-001")
            assertTrue(bRes is DomainResult.Error)

            // Bob attempting to access Vendor A dashboard -> MUST FAIL
            val aForBob = dashboardService.getDashboard("usr_bob", "vnd_a", "TENANT-001")
            assertTrue(aForBob is DomainResult.Error)
        }
    }
}
