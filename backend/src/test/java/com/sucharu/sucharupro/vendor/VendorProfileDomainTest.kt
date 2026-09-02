package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test

class VendorProfileDomainTest {

    @Test
    fun `CapabilityStatus helpers return correct booleans`() {
        assertTrue(CapabilityStatus.ACTIVE.isActive)
        assertTrue(CapabilityStatus.ACTIVE.isSelectable)

        assertFalse(CapabilityStatus.INACTIVE.isActive)
        assertFalse(CapabilityStatus.INACTIVE.isSelectable)

        assertFalse(CapabilityStatus.SUSPENDED.isActive)
        assertFalse(CapabilityStatus.SUSPENDED.isSelectable)
    }

    @Test
    fun `CapabilityType enum values are comprehensive for printing and packaging press`() {
        val types = CapabilityType.values().map { it.name }
        assertTrue(types.contains("PRINTING"))
        assertTrue(types.contains("CTP"))
        assertTrue(types.contains("LAMINATION"))
        assertTrue(types.contains("FOILING"))
        assertTrue(types.contains("DIE_CUTTING"))
        assertTrue(types.contains("PERFECT_BINDING"))
        assertTrue(types.contains("SADDLE_STITCH"))
        assertTrue(types.contains("PACKAGING"))
    }

    @Test
    fun `ContactType and AddressType enum values contain expected variants`() {
        val contactTypes = ContactType.values().map { it.name }
        assertTrue(contactTypes.contains("PRIMARY"))
        assertTrue(contactTypes.contains("ACCOUNTS"))
        assertTrue(contactTypes.contains("PRODUCTION"))
        assertTrue(contactTypes.contains("DISPATCH"))

        val addressTypes = AddressType.values().map { it.name }
        assertTrue(addressTypes.contains("REGISTERED"))
        assertTrue(addressTypes.contains("OFFICE"))
        assertTrue(addressTypes.contains("FACTORY"))
        assertTrue(addressTypes.contains("WAREHOUSE"))
        assertTrue(addressTypes.contains("BILLING"))
        assertTrue(addressTypes.contains("DELIVERY"))
    }

    @Test
    fun `VendorProfile creation with all fields`() {
        val profile = VendorProfile(
            vendorId = "vnd_001",
            projectId = "p_001",
            legalName = "Bengal Printing & Packaging Ltd.",
            displayName = "Bengal Printing",
            contactPerson = "Rafiqul Islam",
            primaryPhone = "+8801711223344",
            alternatePhone = "+8801811223344",
            email = "info@bengalprint.com",
            website = "https://bengalprint.com",
            taxId = "TIN-987654321",
            businessRegistrationNumber = "BRN-123456",
            notes = "Specializes in multi-color offset and foil stamping",
            createdAt = 1000L,
            updatedAt = 1000L,
            createdBy = "user_admin",
            updatedBy = "user_admin",
            version = 1L
        )

        assertEquals("vnd_001", profile.vendorId)
        assertEquals("Bengal Printing", profile.displayName)
        assertEquals("info@bengalprint.com", profile.email)
        assertEquals(1L, profile.version)
    }

    @Test
    fun `VendorContact creation and immutability check`() {
        val contact = VendorContact(
            contactId = "cnt_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            contactType = ContactType.PRODUCTION,
            name = "Shafiq Ahmed",
            designation = "Production Manager",
            phone = "+8801700000000",
            email = "shafiq@bengalprint.com",
            isPrimary = false,
            active = true
        )

        assertEquals("cnt_001", contact.contactId)
        assertEquals(ContactType.PRODUCTION, contact.contactType)
        assertTrue(contact.active)

        val updated = contact.copy(isPrimary = true, active = false)
        assertTrue(updated.isPrimary)
        assertFalse(updated.active)
        assertFalse(contact.isPrimary)
    }

    @Test
    fun `VendorAddress creation and copy test`() {
        val address = VendorAddress(
            addressId = "addr_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            addressType = AddressType.FACTORY,
            addressLine1 = "Plot 42, Tejgaon I/A",
            city = "Dhaka",
            district = "Dhaka",
            postalCode = "1208",
            country = "Bangladesh",
            isPrimary = true,
            active = true
        )

        assertEquals("addr_001", address.addressId)
        assertEquals(AddressType.FACTORY, address.addressType)
        assertEquals("Dhaka", address.city)
        assertTrue(address.isPrimary)
    }

    @Test
    fun `VendorCapability entity creation test`() {
        val cap = VendorCapability(
            capabilityId = "cap_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            capabilityType = CapabilityType.DIE_CUTTING,
            displayName = "High Speed Die Cutting",
            status = CapabilityStatus.ACTIVE,
            notes = "Heidelberg Cylinder Die Cutter"
        )

        assertEquals(CapabilityType.DIE_CUTTING, cap.capabilityType)
        assertEquals("High Speed Die Cutting", cap.displayName)
        assertTrue(cap.status.isActive)
    }
}
