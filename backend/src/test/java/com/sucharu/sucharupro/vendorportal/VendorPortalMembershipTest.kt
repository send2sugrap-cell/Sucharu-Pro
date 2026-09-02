package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalMembershipTest {

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
                    vendorName = "Master Supply Ltd",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testInviteAndActivateMembershipWorkflow() {
        runBlocking {
            val acc = portalService.createOrInviteAccount(
                vendorId = "vnd_001",
                portalCode = "SUPPLY-01",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                actorId = "admin_001"
            )
            val portalAccountId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(portalAccountId, "TENANT-001", "admin_001")

            // 1. Invite user
            val iRes = portalService.inviteVendorUser(
                portalAccountId = portalAccountId,
                vendorId = "vnd_001",
                userId = "usr_vendor_01",
                role = VendorPortalRole.VENDOR_FINANCE,
                projectScope = "PROJ-ALPHA",
                tenantId = "TENANT-001",
                actorId = "admin_001"
            )
            assertTrue(iRes is DomainResult.Success)
            val invited = (iRes as DomainResult.Success).data
            assertEquals(VendorPortalMembershipStatus.PENDING_ACTIVATION, invited.status)
            assertNotNull(invited.invitationToken)

            // 2. Activate membership using token (with SoD: actor is admin_001)
            val aRes = portalService.activateMembership(
                invitationToken = invited.invitationToken!!,
                tenantId = "TENANT-001",
                actorId = "admin_001",
                isInternalAdmin = true
            )
            assertTrue(aRes is DomainResult.Success)
            val activated = (aRes as DomainResult.Success).data
            assertEquals(VendorPortalMembershipStatus.ACTIVE, activated.status)
            assertNull(activated.invitationToken) // Cleared on activation
        }
    }

    @Test
    fun testUpdateMembershipStatusAndRevocation() {
        runBlocking {
            val acc = portalService.createOrInviteAccount(
                vendorId = "vnd_001",
                portalCode = "SUPPLY-02",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                actorId = "admin_001"
            )
            val portalAccountId = (acc as DomainResult.Success).data.portalAccountId
            val mem = portalService.inviteVendorUser(
                portalAccountId = portalAccountId,
                vendorId = "vnd_001",
                userId = "usr_002",
                tenantId = "TENANT-001",
                actorId = "admin_001"
            )
            val membershipId = (mem as DomainResult.Success).data.membershipId

            // Suspend
            val sRes = portalService.updateMembershipStatus(membershipId, VendorPortalMembershipStatus.SUSPENDED, "TENANT-001", "admin_001")
            assertTrue(sRes is DomainResult.Success)
            assertEquals(VendorPortalMembershipStatus.SUSPENDED, (sRes as DomainResult.Success).data.status)

            // Revoke
            val rRes = portalService.updateMembershipStatus(membershipId, VendorPortalMembershipStatus.REVOKED, "TENANT-001", "admin_001")
            assertTrue(rRes is DomainResult.Success)
            assertEquals(VendorPortalMembershipStatus.REVOKED, (rRes as DomainResult.Success).data.status)
        }
    }
}
