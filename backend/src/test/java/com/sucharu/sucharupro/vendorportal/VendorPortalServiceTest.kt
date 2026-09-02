package com.sucharu.sucharupro.vendorportal

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

class VendorPortalServiceTest {

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
                    vendorId = "vnd_001",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-001",
                    vendorName = "Global Colors Ltd",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testCannotCreateAccountForNonExistentOrSuspendedVendor() {
        runBlocking {
            // Non-existent vendor fails
            val neRes = portalService.createOrInviteAccount("vnd_unknown", "CODE-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            assertTrue(neRes is DomainResult.Error)

            // Suspended vendor fails
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_suspended",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-SUSP",
                    vendorName = "Suspended Vendor",
                    status = VendorStatus.SUSPENDED
                )
            )
            val sRes = portalService.createOrInviteAccount("vnd_suspended", "SUSP-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            assertTrue(sRes is DomainResult.Error)
            assertTrue((sRes as DomainResult.Error).exception is IllegalStateException)
        }
    }

    @Test
    fun testFullAccessContextResolution() {
        runBlocking {
            val acc = portalService.createOrInviteAccount("vnd_001", "GLOBAL-01", "contact@global.com", "+8801700000000", "TENANT-001", "PROJ-ALPHA", "admin_001")
            val portalAccountId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(portalAccountId, "TENANT-001", "admin_001")

            val mem = portalService.inviteVendorUser(portalAccountId, "vnd_001", "usr_global_01", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-001", "admin_001", true)

            val ctx = portalService.getAccessContext("usr_global_01", "vnd_001", "TENANT-001")
            assertTrue(ctx is DomainResult.Success)
            val data = (ctx as DomainResult.Success).data
            assertEquals("Global Colors Ltd", data.vendorName)
            assertEquals(VendorPortalRole.VENDOR_ADMIN, data.role)
        }
    }
}
