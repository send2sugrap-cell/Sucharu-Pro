package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccountStatus
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalLifecycleTest {

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
                    vendorName = "Print Corp",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testAccountLifecycleFromInvitedToActiveToSuspended() {
        runBlocking {
            // 1. Create account
            val cRes = portalService.createOrInviteAccount(
                vendorId = "vnd_001",
                portalCode = "PRINT-01",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                actorId = "admin_001"
            )
            assertTrue(cRes is DomainResult.Success)
            val account = (cRes as DomainResult.Success).data
            assertEquals(VendorPortalAccountStatus.INVITED, account.status)

            // 2. Activate account
            val aRes = portalService.activateAccount(account.portalAccountId, "TENANT-001", "admin_001")
            assertTrue(aRes is DomainResult.Success)
            val active = (aRes as DomainResult.Success).data
            assertEquals(VendorPortalAccountStatus.ACTIVE, active.status)
            assertNotNull(active.activatedAt)

            // 3. Suspend account
            val sRes = portalService.suspendAccount(account.portalAccountId, "Payment breach", "TENANT-001", "admin_001")
            assertTrue(sRes is DomainResult.Success)
            val suspended = (sRes as DomainResult.Success).data
            assertEquals(VendorPortalAccountStatus.SUSPENDED, suspended.status)
            assertEquals("Payment breach", suspended.suspensionReason)

            // 4. Reactivate account
            val rRes = portalService.activateAccount(account.portalAccountId, "TENANT-001", "admin_001")
            assertTrue(rRes is DomainResult.Success)
            assertEquals(VendorPortalAccountStatus.ACTIVE, (rRes as DomainResult.Success).data.status)
        }
    }
}
