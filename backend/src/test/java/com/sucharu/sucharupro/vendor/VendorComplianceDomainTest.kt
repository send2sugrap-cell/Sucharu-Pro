package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorPerformanceValidator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class VendorComplianceDomainTest {

    @Test
    fun testValidComplianceRequirement() {
        val req = VendorComplianceRequirement(
            requirementId = "COMP-01",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            requirementType = ComplianceRequirementType.TRADE_LICENSE,
            code = "TL-01",
            name = "Valid Trade License",
            description = "Trade license compliance",
            mandatory = true,
            riskLevel = ComplianceRiskLevel.HIGH,
            validityDays = 365,
            createdBy = "admin"
        )
        val res = VendorPerformanceValidator.validateComplianceRequirement(req)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testInvalidComplianceRequirement() {
        val req = VendorComplianceRequirement(
            requirementId = "COMP-01",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            requirementType = ComplianceRequirementType.OTHER,
            code = "",
            name = "",
            description = "",
            validityDays = -10,
            createdBy = ""
        )
        val res = VendorPerformanceValidator.validateComplianceRequirement(req)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testComplianceRecordDatesValidation() {
        val now = Instant.now()
        val rec = VendorComplianceRecord(
            recordId = "REC-01",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            requirementId = "COMP-01",
            requirementCode = "TL-01",
            requirementName = "Trade License",
            requirementType = ComplianceRequirementType.TRADE_LICENSE,
            effectiveDate = now,
            expiryDate = now.minusSeconds(86400),
            createdBy = "staff"
        )
        val res = VendorPerformanceValidator.validateComplianceRecord(rec)
        assertTrue(res is DomainResult.Error)
    }
}
