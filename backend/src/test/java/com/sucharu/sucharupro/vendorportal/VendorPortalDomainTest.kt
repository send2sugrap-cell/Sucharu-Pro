package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalDomainTest {

    @Test
    fun testVendorPortalEnumsIntegrity() {
        assertEquals(6, VendorPortalAccountStatus.entries.size)
        assertTrue(VendorPortalAccountStatus.entries.contains(VendorPortalAccountStatus.INVITED))
        assertTrue(VendorPortalAccountStatus.entries.contains(VendorPortalAccountStatus.ACTIVE))
        assertTrue(VendorPortalAccountStatus.entries.contains(VendorPortalAccountStatus.SUSPENDED))
        assertTrue(VendorPortalAccountStatus.entries.contains(VendorPortalAccountStatus.REVOKED))

        assertEquals(6, VendorPortalMembershipStatus.entries.size)
        assertTrue(VendorPortalMembershipStatus.entries.contains(VendorPortalMembershipStatus.PENDING_ACTIVATION))

        assertEquals(6, VendorPortalRole.entries.size)
        assertEquals("Vendor Administrator", VendorPortalRole.VENDOR_ADMIN.displayName)
        assertEquals("Vendor Operations Lead", VendorPortalRole.VENDOR_OPERATOR.displayName)
        assertEquals("Vendor Billing & Finance", VendorPortalRole.VENDOR_FINANCE.displayName)
        assertEquals("Vendor Quality Control", VendorPortalRole.VENDOR_QC.displayName)
        assertEquals("Vendor Shipping & Logistics", VendorPortalRole.VENDOR_LOGISTICS.displayName)
        assertEquals("Vendor Read-Only Observer", VendorPortalRole.VENDOR_VIEWER.displayName)
    }

    @Test
    fun testVendorPortalAccountModelCreation() {
        val account = VendorPortalAccount(
            portalAccountId = "pa_001",
            vendorId = "vnd_001",
            tenantId = "TENANT-001",
            projectId = "PROJ-ALPHA",
            status = VendorPortalAccountStatus.ACTIVE,
            portalCode = "VND-PRINT-01",
            primaryContactEmail = "partner@sucharu.com",
            primaryContactPhone = "+8801700000000"
        )
        assertEquals("pa_001", account.portalAccountId)
        assertEquals("VND-PRINT-01", account.portalCode)
        assertEquals(VendorPortalAccountStatus.ACTIVE, account.status)
        assertEquals(1L, account.version)
    }

    @Test
    fun testVendorPortalMembershipModelCreation() {
        val membership = VendorPortalMembership(
            membershipId = "mem_001",
            portalAccountId = "pa_001",
            vendorId = "vnd_001",
            userId = "usr_001",
            tenantId = "TENANT-001",
            projectScope = "PROJ-ALPHA",
            role = VendorPortalRole.VENDOR_ADMIN,
            status = VendorPortalMembershipStatus.ACTIVE
        )
        assertEquals("mem_001", membership.membershipId)
        assertEquals("usr_001", membership.userId)
        assertEquals(VendorPortalRole.VENDOR_ADMIN, membership.role)
        assertEquals(VendorPortalMembershipStatus.ACTIVE, membership.status)
    }

    @Test
    fun testVendorPortalAccessContextModelCreation() {
        val policy = VendorPortalAccessPolicy(
            policyId = "pol_001",
            tenantId = "TENANT-001",
            projectId = "PROJ-ALPHA",
            vendorId = "vnd_001"
        )
        val context = VendorPortalAccessContext(
            userId = "usr_001",
            vendorId = "vnd_001",
            vendorCode = "VND-001",
            vendorName = "Sucharu Master Vendor",
            membershipId = "mem_001",
            role = VendorPortalRole.VENDOR_ADMIN,
            tenantId = "TENANT-001",
            projectScope = "*",
            accountStatus = VendorPortalAccountStatus.ACTIVE,
            membershipStatus = VendorPortalMembershipStatus.ACTIVE,
            policy = policy,
            allowedFeatures = listOf("RFQ", "PURCHASE_ORDERS", "INVOICES")
        )
        assertEquals("Sucharu Master Vendor", context.vendorName)
        assertEquals(3, context.allowedFeatures.size)
        assertTrue(context.allowedFeatures.contains("INVOICES"))
    }
}
