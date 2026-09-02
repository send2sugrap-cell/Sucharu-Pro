package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorQualityValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorDisputeValidatorTest {

    @Test
    fun testValidDisputePasses() {
        val dispute = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-2026-0001",
            subject = "Damaged delivery pallets",
            description = "Pallets broken during transit",
            raisedBy = "user-1"
        )
        val result = VendorQualityValidator.validateDispute(dispute)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun testBlankSubjectOrDescriptionFails() {
        val blankSubject = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-01",
            subject = "",
            description = "Desc",
            raisedBy = "user-1"
        )
        assertTrue(VendorQualityValidator.validateDispute(blankSubject) is DomainResult.Error)

        val blankDesc = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-01",
            subject = "Subj",
            description = "",
            raisedBy = "user-1"
        )
        assertTrue(VendorQualityValidator.validateDispute(blankDesc) is DomainResult.Error)
    }

    @Test
    fun testNegativeDisputedQuantityFails() {
        val dispute = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-01",
            subject = "Subj",
            description = "Desc",
            disputedQuantity = BigDecimal("-1"),
            raisedBy = "user-1"
        )
        assertTrue(VendorQualityValidator.validateDispute(dispute) is DomainResult.Error)
    }
}
