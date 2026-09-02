package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalVendorIsolationTest {

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
        }
    }

    @Test
    fun testVendorUserCannotAccessOtherVendorContext() {
        runBlocking {
            // Setup Vendor A
            val accA = portalService.createOrInviteAccount("vnd_a", "VND-A-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val portalAccountA = (accA as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(portalAccountA, "TENANT-001", "admin_001")
            val memA = portalService.inviteVendorUser(portalAccountA, "vnd_a", "usr_alice", tenantId = "TENANT-001", actorId = "admin_001")
            val tokenA = (memA as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(tokenA, "TENANT-001", "admin_001", true)

            // Setup Vendor B
            val accB = portalService.createOrInviteAccount("vnd_b", "VND-B-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val portalAccountB = (accB as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(portalAccountB, "TENANT-001", "admin_001")
            val memB = portalService.inviteVendorUser(portalAccountB, "vnd_b", "usr_bob", tenantId = "TENANT-001", actorId = "admin_001")
            val tokenB = (memB as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(tokenB, "TENANT-001", "admin_001", true)

            // User Alice (from Vendor A) attempts to access Vendor B context -> MUST FAIL
            val contextBForAlice = portalService.getAccessContext("usr_alice", "vnd_b", "TENANT-001")
            assertTrue(contextBForAlice is DomainResult.Error)
            assertTrue((contextBForAlice as DomainResult.Error).exception is NoSuchElementException)

            // User Bob (from Vendor B) attempts to access Vendor A context -> MUST FAIL
            val contextAForBob = portalService.getAccessContext("usr_bob", "vnd_a", "TENANT-001")
            assertTrue(contextAForBob is DomainResult.Error)
            assertTrue((contextAForBob as DomainResult.Error).exception is NoSuchElementException)
        }
    }
}
