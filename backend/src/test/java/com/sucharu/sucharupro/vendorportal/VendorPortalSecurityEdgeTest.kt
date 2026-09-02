package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccountStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalMembershipStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalSecurityEdgeTest {

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
                    vendorId = "vnd_edge",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-EDGE",
                    vendorName = "Edge Security Vendor",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testAccessBlockedWhenAccountOrMembershipIsSuspended() {
        runBlocking {
            val acc = portalService.createOrInviteAccount("vnd_edge", "EDGE-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val portalAccountId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(portalAccountId, "TENANT-001", "admin_001")

            val mem = portalService.inviteVendorUser(portalAccountId, "vnd_edge", "usr_edge_01", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-001", "admin_001", true)

            // Active works
            val activeCtx = portalService.getAccessContext("usr_edge_01", "vnd_edge", "TENANT-001")
            assertTrue(activeCtx is DomainResult.Success)

            // 1. Suspend account -> Access context blocked
            portalService.suspendAccount(portalAccountId, "Security audit pending", "TENANT-001", "admin_001")
            val suspendedAccCtx = portalService.getAccessContext("usr_edge_01", "vnd_edge", "TENANT-001")
            assertTrue(suspendedAccCtx is DomainResult.Error)
            assertTrue((suspendedAccCtx as DomainResult.Error).exception is IllegalStateException)

            // Reactivate account
            portalService.activateAccount(portalAccountId, "TENANT-001", "admin_001")

            // 2. Suspend membership -> Access context blocked
            val membershipId = (mem as DomainResult.Success).data.membershipId
            portalService.updateMembershipStatus(membershipId, VendorPortalMembershipStatus.SUSPENDED, "TENANT-001", "admin_001")
            val suspendedMemCtx = portalService.getAccessContext("usr_edge_01", "vnd_edge", "TENANT-001")
            assertTrue(suspendedMemCtx is DomainResult.Error)
            assertTrue((suspendedMemCtx as DomainResult.Error).exception is IllegalStateException)
        }
    }

    @Test
    fun testInvalidOrForgedTokenActivationFails() {
        runBlocking {
            val res = portalService.activateMembership("VPT-FORGED-TOKEN-999", "TENANT-001", "admin_001", false)
            assertTrue(res is DomainResult.Error)
            assertTrue((res as DomainResult.Error).exception is NoSuchElementException)
        }
    }
}
