package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.vendor.ComplianceRiskLevel
import com.sucharu.sucharupro.domain.model.vendor.ComplianceStatus
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class VendorComplianceExpiryTest {

    @Test
    fun testComplianceExpiryStatusCalculation() {
        val now = Instant.now()

        // Expired (yesterday)
        val expired = now.minusSeconds(86400)
        val (status1, risk1) = VendorPerformanceCalculator.determineComplianceStatusAndRisk(expired, now)
        assertEquals(ComplianceStatus.EXPIRED, status1)
        assertEquals(ComplianceRiskLevel.CRITICAL, risk1)

        // Expiring soon (10 days ahead)
        val expiringSoon = now.plusSeconds(86400 * 10)
        val (status2, risk2) = VendorPerformanceCalculator.determineComplianceStatusAndRisk(expiringSoon, now, warningThresholdDays = 30)
        assertEquals(ComplianceStatus.EXPIRING_SOON, status2)
        assertEquals(ComplianceRiskLevel.HIGH, risk2)

        // Valid verified (100 days ahead)
        val validActive = now.plusSeconds(86400 * 100)
        val (status3, risk3) = VendorPerformanceCalculator.determineComplianceStatusAndRisk(validActive, now, warningThresholdDays = 30)
        assertEquals(ComplianceStatus.VERIFIED, status3)
        assertEquals(ComplianceRiskLevel.LOW, risk3)

        // No expiry date
        val (status4, risk4) = VendorPerformanceCalculator.determineComplianceStatusAndRisk(null, now)
        assertEquals(ComplianceStatus.VERIFIED, status4)
        assertEquals(ComplianceRiskLevel.LOW, risk4)
    }
}
