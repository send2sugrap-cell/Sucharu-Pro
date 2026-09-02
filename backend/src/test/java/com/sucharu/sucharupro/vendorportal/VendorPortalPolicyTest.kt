package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccessPolicy
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalPolicyTest {

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
                    vendorName = "Prime Packaging",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testPolicySaveAndFeatureResolution() {
        runBlocking {
            // Save vendor-specific policy with RFQ and Invoice allowed, but PO acknowledgement disabled
            val policy = VendorPortalAccessPolicy(
                policyId = "pol_001",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                vendorId = "vnd_001",
                allowRfqSubmission = true,
                allowPoAcknowledgement = false,
                allowInvoiceSubmission = true,
                ipWhitelist = "192.168.1.100"
            )
            val pRes = portalService.savePolicy(policy, "TENANT-001", "admin_001")
            assertTrue(pRes is DomainResult.Success)

            // Setup active account and membership
            val acc = portalService.createOrInviteAccount("vnd_001", "PRIME-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val portalAccountId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(portalAccountId, "TENANT-001", "admin_001")

            val mem = portalService.inviteVendorUser(portalAccountId, "vnd_001", "usr_prime_01", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-001", "admin_001", true)

            // Resolve context with matching IP
            val ctxRes = portalService.getAccessContext("usr_prime_01", "vnd_001", "TENANT-001", "192.168.1.100")
            assertTrue(ctxRes is DomainResult.Success)
            val context = (ctxRes as DomainResult.Success).data
            assertTrue(context.allowedFeatures.contains("RFQ"))
            assertTrue(context.allowedFeatures.contains("INVOICES"))
            assertFalse(context.allowedFeatures.contains("PURCHASE_ORDERS")) // Disabled in policy

            // Context request with blocked IP fails
            val blockedRes = portalService.getAccessContext("usr_prime_01", "vnd_001", "TENANT-001", "10.0.0.1")
            assertTrue(blockedRes is DomainResult.Error)
            assertTrue((blockedRes as DomainResult.Error).exception is SecurityException)
        }
    }
}
