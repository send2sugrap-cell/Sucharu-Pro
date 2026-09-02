package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorQualityValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorRejectionValidatorTest {

    @Test
    fun testValidRejectionPasses() {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-2026-0001",
            rejectionReason = "Surface scratches on delivered aluminum plates",
            rejectedQuantity = BigDecimal("25"),
            rejectedValue = Money(1250.0)
        )
        val result = VendorQualityValidator.validateRejection(rejection)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun testZeroOrNegativeQuantityFails() {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-2026-0001",
            rejectionReason = "Damage",
            rejectedQuantity = BigDecimal.ZERO
        )
        val result = VendorQualityValidator.validateRejection(rejection)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun testBlankIdentifiersFail() {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "",
            rejectionReason = "Damage",
            rejectedQuantity = BigDecimal("10")
        )
        val result = VendorQualityValidator.validateRejection(rejection)
        assertTrue(result is DomainResult.Error)
    }
}
