package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import org.junit.Assert.*
import org.junit.Test

class VendorValidatorTest {

    private fun validVendor(): Vendor = Vendor(
        vendorId = "vnd_001",
        projectId = "PRJ-01",
        vendorCode = "VND-0001",
        vendorName = "Super Printing Press",
        legalName = "Super Printing Press Ltd.",
        vendorType = VendorType.SERVICE_PROVIDER,
        vendorCategory = VendorCategory.PRINTING,
        status = VendorStatus.ACTIVE,
        primaryContactName = "Manager Rahim",
        primaryPhone = "+8801812345678",
        primaryEmail = "rahim@superprint.com",
        notes = "Regular supplier of lamination plates."
    )

    @Test
    fun testValidVendorPassesValidation() {
        val res = VendorValidator.validate(validVendor())
        assertTrue(res.isValid)
        assertTrue(res.errors.isEmpty())
        assertNull(res.errorMessage)
    }

    @Test
    fun testOversizedNameFails() {
        val longName = "A".repeat(201)
        val v = validVendor().copy(vendorName = longName)
        val res = VendorValidator.validate(v)
        assertFalse(res.isValid)
        assertTrue(res.errorMessage?.contains("vendorName exceeds maximum length") == true)
    }

    @Test
    fun testOversizedCodeFails() {
        val longCode = "C".repeat(65)
        val v = validVendor().copy(vendorCode = longCode)
        val res = VendorValidator.validate(v)
        assertFalse(res.isValid)
        assertTrue(res.errorMessage?.contains("vendorCode exceeds maximum length") == true)
    }

    @Test
    fun testInvalidEmailFormatFails() {
        val v = validVendor().copy(primaryEmail = "not-an-email")
        val res = VendorValidator.validate(v)
        assertFalse(res.isValid)
        assertTrue(res.errorMessage?.contains("valid email address format") == true)
    }

    @Test
    fun testStatusTransitions() {
        // Active -> Suspended -> Active
        assertTrue(VendorValidator.validateStatusTransition(VendorStatus.ACTIVE, VendorStatus.SUSPENDED).isValid)
        assertTrue(VendorValidator.validateStatusTransition(VendorStatus.SUSPENDED, VendorStatus.ACTIVE).isValid)

        // Active -> Archived
        assertTrue(VendorValidator.validateStatusTransition(VendorStatus.ACTIVE, VendorStatus.ARCHIVED).isValid)

        // Archived -> Active (Disallowed)
        val fromArchived = VendorValidator.validateStatusTransition(VendorStatus.ARCHIVED, VendorStatus.ACTIVE)
        assertFalse(fromArchived.isValid)
        assertTrue(fromArchived.errorMessage?.contains("Cannot transition status from ARCHIVED state") == true)
    }
}
