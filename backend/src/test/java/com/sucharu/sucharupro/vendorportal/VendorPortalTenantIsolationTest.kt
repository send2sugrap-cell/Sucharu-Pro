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

class VendorPortalTenantIsolationTest {

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

            // Tenant Alpha Vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_alpha",
                    projectId = "TENANT-ALPHA",
                    vendorCode = "VND-ALPHA",
                    vendorName = "Alpha Vendor",
                    status = VendorStatus.ACTIVE
                )
            )

            // Tenant Beta Vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_beta",
                    projectId = "TENANT-BETA",
                    vendorCode = "VND-BETA",
                    vendorName = "Beta Vendor",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testTenantIsolationForPortalAccountsAndMemberships() {
        runBlocking {
            // Create account in Tenant Alpha
            val acc = portalService.createOrInviteAccount("vnd_alpha", "ALPHA-01", null, null, "TENANT-ALPHA", "TENANT-ALPHA", "admin_alpha")
            val portalAccountId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(portalAccountId, "TENANT-ALPHA", "admin_alpha")

            val mem = portalService.inviteVendorUser(portalAccountId, "vnd_alpha", "usr_alpha", tenantId = "TENANT-ALPHA", actorId = "admin_alpha")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-ALPHA", "admin_alpha", true)

            // Tenant Beta cannot access Tenant Alpha's portal account
            val bAcc = portalRepo.getAccountById(portalAccountId, "TENANT-BETA")
            assertTrue(bAcc is DomainResult.Success)
            assertNull((bAcc as DomainResult.Success).data)

            // Tenant Beta cannot resolve access context for Tenant Alpha vendor
            val bCtx = portalService.getAccessContext("usr_alpha", "vnd_alpha", "TENANT-BETA")
            assertTrue(bCtx is DomainResult.Error)

            // Tenant Alpha can resolve access context
            val aCtx = portalService.getAccessContext("usr_alpha", "vnd_alpha", "TENANT-ALPHA")
            assertTrue(aCtx is DomainResult.Success)
        }
    }
}
