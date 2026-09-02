package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorPerformanceValidator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class VendorPerformanceDomainTest {

    @Test
    fun testValidKpiModel() {
        val kpi = VendorPerformanceKpi(
            kpiId = "KPI-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            code = "ON_TIME_DELIVERY",
            name = "On-Time Delivery Rate",
            description = "Tracks on-time shipments",
            kpiType = KpiType.OPERATIONAL,
            measurementMethod = KpiMeasurementMethod.AUTOMATED,
            targetValue = 95.0,
            minimumAcceptableValue = 85.0,
            maximumAcceptableValue = 100.0,
            unit = "%",
            direction = KpiDirection.HIGHER_IS_BETTER,
            weight = 1.5,
            createdBy = "user_01"
        )
        val res = VendorPerformanceValidator.validateKpi(kpi)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testInvalidKpiMissingCodeAndNegativeWeight() {
        val kpi = VendorPerformanceKpi(
            kpiId = "KPI-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            code = "",
            name = "Test",
            description = "Desc",
            kpiType = KpiType.OPERATIONAL,
            targetValue = 90.0,
            weight = -1.0,
            createdBy = "user"
        )
        val res = VendorPerformanceValidator.validateKpi(kpi)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testValidMeasurement() {
        val now = Instant.now()
        val meas = VendorPerformanceMeasurement(
            measurementId = "MEAS-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            kpiId = "KPI-001",
            kpiCode = "OTD",
            periodStart = now.minusSeconds(86400),
            periodEnd = now,
            actualValue = 98.0,
            numerator = 98.0,
            denominator = 100.0,
            sampleSize = 50,
            measuredBy = "system"
        )
        val res = VendorPerformanceValidator.validateMeasurement(meas)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testMeasurementInvalidDateRange() {
        val now = Instant.now()
        val meas = VendorPerformanceMeasurement(
            measurementId = "MEAS-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            kpiId = "KPI-001",
            kpiCode = "OTD",
            periodStart = now,
            periodEnd = now.minusSeconds(86400),
            actualValue = 98.0,
            numerator = 98.0,
            denominator = 100.0,
            sampleSize = 10,
            measuredBy = "system"
        )
        val res = VendorPerformanceValidator.validateMeasurement(meas)
        assertTrue(res is DomainResult.Error)
    }
}
