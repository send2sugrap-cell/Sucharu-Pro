package com.sucharu.sucharupro.domain.model.vendor

import org.junit.Assert.*
import org.junit.Test

class VendorDomainTest {

    @Test
    fun testValidVendorCreation() {
        val vendor = Vendor(
            vendorId = "vnd_001",
            projectId = "PRJ-01",
            vendorCode = "VND-0001",
            vendorName = "Prime Printing Services",
            legalName = "Prime Printing Solutions Ltd.",
            vendorType = VendorType.PRODUCTION_VENDOR,
            vendorCategory = VendorCategory.PRINTING,
            status = VendorStatus.ACTIVE,
            primaryContactName = "Akash Ahmed",
            primaryPhone = "+8801711223344",
            primaryEmail = "akash@primeprint.com",
            notes = "Specialized in high-speed offset printing."
        )

        assertEquals("vnd_001", vendor.vendorId)
        assertEquals("PRJ-01", vendor.projectId)
        assertEquals("VND-0001", vendor.vendorCode)
        assertEquals("Prime Printing Services", vendor.vendorName)
        assertEquals(VendorType.PRODUCTION_VENDOR, vendor.vendorType)
        assertEquals(VendorCategory.PRINTING, vendor.vendorCategory)
        assertEquals(VendorStatus.ACTIVE, vendor.status)
        assertTrue(vendor.status.isActive)
        assertTrue(vendor.status.isModifiable)
        assertEquals(1L, vendor.version)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVendorWithBlankIdThrows() {
        Vendor(
            vendorId = "",
            projectId = "PRJ-01",
            vendorCode = "VND-0001",
            vendorName = "Prime"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVendorWithBlankProjectIdThrows() {
        Vendor(
            vendorId = "vnd_001",
            projectId = "   ",
            vendorCode = "VND-0001",
            vendorName = "Prime"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVendorWithBlankCodeThrows() {
        Vendor(
            vendorId = "vnd_001",
            projectId = "PRJ-01",
            vendorCode = "",
            vendorName = "Prime"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVendorWithBlankNameThrows() {
        Vendor(
            vendorId = "vnd_001",
            projectId = "PRJ-01",
            vendorCode = "VND-0001",
            vendorName = "   "
        )
    }

    @Test
    fun testVendorStatusProperties() {
        assertTrue(VendorStatus.ACTIVE.isActive)
        assertFalse(VendorStatus.DRAFT.isActive)
        assertFalse(VendorStatus.SUSPENDED.isActive)
        assertFalse(VendorStatus.INACTIVE.isActive)
        assertFalse(VendorStatus.ARCHIVED.isActive)

        assertTrue(VendorStatus.ACTIVE.isModifiable)
        assertTrue(VendorStatus.DRAFT.isModifiable)
        assertTrue(VendorStatus.SUSPENDED.isModifiable)
        assertTrue(VendorStatus.INACTIVE.isModifiable)
        assertFalse(VendorStatus.ARCHIVED.isModifiable)
    }
}
