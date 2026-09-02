package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalExpiryAlertLevel
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalPerformanceComplianceValidator
import org.junit.Assert.*
import org.junit.Test

class VendorPortalPerformanceValidatorTest {

    @Test
    fun testZeroSafePercentageCalculation() {
        assertEquals(0.0, VendorPortalPerformanceComplianceValidator.calculatePercentage(0, 0), 0.001)
        assertEquals(50.0, VendorPortalPerformanceComplianceValidator.calculatePercentage(5, 10), 0.001)
        assertEquals(100.0, VendorPortalPerformanceComplianceValidator.calculatePercentage(10, 10), 0.001)
    }

    @Test
    fun testResolveExpiryAlertLevel() {
        val now = System.currentTimeMillis()
        val expired = now - 86400000L
        val critical = now + (3L * 86400000L)
        val warning = now + (20L * 86400000L)
        val normal = now + (60L * 86400000L)

        assertEquals(VendorPortalExpiryAlertLevel.EXPIRED, VendorPortalPerformanceComplianceValidator.resolveExpiryAlertLevel(expired))
        assertEquals(VendorPortalExpiryAlertLevel.CRITICAL_7_DAYS, VendorPortalPerformanceComplianceValidator.resolveExpiryAlertLevel(critical))
        assertEquals(VendorPortalExpiryAlertLevel.UPCOMING_30_DAYS, VendorPortalPerformanceComplianceValidator.resolveExpiryAlertLevel(warning))
        assertEquals(VendorPortalExpiryAlertLevel.NORMAL, VendorPortalPerformanceComplianceValidator.resolveExpiryAlertLevel(normal))
        assertEquals(VendorPortalExpiryAlertLevel.NORMAL, VendorPortalPerformanceComplianceValidator.resolveExpiryAlertLevel(null))
    }

    @Test
    fun testValidateEvaluationResponse() {
        val valid = VendorPortalPerformanceComplianceValidator.validateEvaluationResponse(
            "TENANT-001", "PRJ-001", "VND-001", "EV-001", "Score Clarification", "We improved OTD in Week 4", "USER-001"
        )
        assertTrue(valid is DomainResult.Success)

        val emptySubject = VendorPortalPerformanceComplianceValidator.validateEvaluationResponse(
            "TENANT-001", "PRJ-001", "VND-001", "EV-001", "", "Remarks", "USER-001"
        )
        assertTrue(emptySubject is DomainResult.Error)
    }

    @Test
    fun testValidateCorrectiveActionResponse() {
        val valid = VendorPortalPerformanceComplianceValidator.validateCorrectiveActionResponse(
            "TENANT-001", "PRJ-001", "VND-001", "CA-001", "Repaired sensor", 75.0, "USER-001"
        )
        assertTrue(valid is DomainResult.Success)

        val invalidProgress = VendorPortalPerformanceComplianceValidator.validateCorrectiveActionResponse(
            "TENANT-001", "PRJ-001", "VND-001", "CA-001", "Repaired sensor", 120.0, "USER-001"
        )
        assertTrue(invalidProgress is DomainResult.Error)
    }
}
