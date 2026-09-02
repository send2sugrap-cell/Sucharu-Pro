package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorAddressValidator
import com.sucharu.sucharupro.domain.validation.vendor.VendorCapabilityValidator
import com.sucharu.sucharupro.domain.validation.vendor.VendorContactValidator
import com.sucharu.sucharupro.domain.validation.vendor.VendorProfileValidator
import org.junit.Assert.*
import org.junit.Test

class VendorProfileValidatorTest {

    @Test
    fun `valid vendor profile passes validation`() {
        val profile = VendorProfile(
            vendorId = "vnd_001",
            projectId = "p_001",
            displayName = "Bengal Printing Press",
            legalName = "Bengal Printing & Packaging Ltd.",
            primaryPhone = "+8801711223344",
            email = "contact@bengalprint.com",
            website = "https://bengalprint.com"
        )
        val res = VendorProfileValidator.validate(profile)
        assertTrue(res.isValid)
        assertTrue(res.errors.isEmpty())
    }

    @Test
    fun `vendor profile with blank required fields fails validation`() {
        val profile = VendorProfile(
            vendorId = "",
            projectId = "",
            displayName = "   "
        )
        val res = VendorProfileValidator.validate(profile)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("vendorId") })
        assertTrue(res.errors.any { it.contains("projectId") })
        assertTrue(res.errors.any { it.contains("displayName") })
    }

    @Test
    fun `vendor profile with invalid email or phone fails validation`() {
        val profile = VendorProfile(
            vendorId = "vnd_001",
            projectId = "p_001",
            displayName = "Valid Name",
            email = "not-an-email",
            primaryPhone = "abc-123"
        )
        val res = VendorProfileValidator.validate(profile)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("email") })
        assertTrue(res.errors.any { it.contains("primaryPhone") })
    }

    @Test
    fun `valid vendor contact passes validation`() {
        val contact = VendorContact(
            contactId = "cnt_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            contactType = ContactType.ACCOUNTS,
            name = "Accounts Manager",
            phone = "+8801811223344",
            email = "accounts@bengalprint.com"
        )
        val res = VendorContactValidator.validate(contact)
        assertTrue(res.isValid)
    }

    @Test
    fun `vendor contact with blank name fails validation`() {
        val contact = VendorContact(
            contactId = "cnt_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            name = ""
        )
        val res = VendorContactValidator.validate(contact)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("name") })
    }

    @Test
    fun `valid vendor address passes validation`() {
        val address = VendorAddress(
            addressId = "addr_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            addressType = AddressType.OFFICE,
            addressLine1 = "123 Commercial Area",
            city = "Dhaka",
            country = "Bangladesh"
        )
        val res = VendorAddressValidator.validate(address)
        assertTrue(res.isValid)
    }

    @Test
    fun `vendor address with blank addressLine1 fails validation`() {
        val address = VendorAddress(
            addressId = "addr_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            addressLine1 = "   ",
            city = "",
            country = ""
        )
        val res = VendorAddressValidator.validate(address)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("addressLine1") })
        assertTrue(res.errors.any { it.contains("city") })
        assertTrue(res.errors.any { it.contains("country") })
    }

    @Test
    fun `valid vendor capability passes validation`() {
        val cap = VendorCapability(
            capabilityId = "cap_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            capabilityType = CapabilityType.SPOT_UV,
            displayName = "Auto Spot UV & Drip Off Coating"
        )
        val res = VendorCapabilityValidator.validate(cap)
        assertTrue(res.isValid)
    }

    @Test
    fun `vendor capability with blank displayName fails validation`() {
        val cap = VendorCapability(
            capabilityId = "cap_001",
            vendorId = "vnd_001",
            projectId = "p_001",
            capabilityType = CapabilityType.SPOT_UV,
            displayName = ""
        )
        val res = VendorCapabilityValidator.validate(cap)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("displayName") })
    }
}
