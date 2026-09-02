package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.toDto
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalPerformanceUiTest {

    @Test
    fun testMappingPerformanceOverviewToUiDto() {
        val overview = VendorPortalPerformanceOverview(
            vendorId = "VND-UI-01",
            overallScore = 91.2,
            rating = PerformanceRating.EXCELLENT,
            riskLevel = ComplianceRiskLevel.LOW,
            onTimeDeliveryRate = 96.5,
            poFulfillmentRate = 99.0,
            defectRate = 0.2,
            qualityRating = "EXCELLENT",
            totalScorecards = 10,
            activeEvaluations = 2,
            openCorrectiveActions = 1,
            topStrengths = listOf("Superior delivery speed"),
            improvementAreas = listOf("Batch size documentation")
        )

        val dto = overview.toDto()
        assertEquals("VND-UI-01", dto.vendorId)
        assertEquals(91.2, dto.overallScore, 0.001)
        assertEquals("EXCELLENT", dto.rating)
        assertEquals("LOW", dto.riskLevel)
        assertEquals(1, dto.topStrengths.size)
        assertEquals(1, dto.improvementAreas.size)
    }

    @Test
    fun testMappingScorecardAndKpisToUiDto() {
        val kpi = VendorPortalPerformanceKpiSummary(
            kpiId = "KPI-01",
            code = "OTD",
            name = "On Time Delivery",
            description = "Measure delivery on time",
            kpiType = KpiType.DELIVERY,
            targetValue = 95.0,
            actualValue = 98.0,
            normalizedScore = 100.0,
            weightedScore = 30.0,
            weight = 0.3,
            unit = "%",
            direction = KpiDirection.HIGHER_IS_BETTER,
            sampleSize = 10,
            confidenceState = MeasurementConfidenceState.SUFFICIENT_DATA
        )

        val scorecard = VendorPortalPerformanceScorecardSummary(
            scorecardId = "SC-UI-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-UI-01",
            periodType = EvaluationPeriodType.QUARTERLY,
            periodStart = 1700000000000L,
            periodEnd = 1705000000000L,
            overallScore = 96.0,
            rating = PerformanceRating.EXCELLENT,
            riskLevel = ComplianceRiskLevel.LOW,
            dataCompleteness = 100.0,
            sampleSize = 10,
            status = ScorecardStatus.APPROVED,
            items = listOf(kpi),
            generatedAt = 1705000000000L
        )

        val dto = scorecard.toDto()
        assertEquals("SC-UI-01", dto.scorecardId)
        assertEquals("QUARTERLY", dto.periodType)
        assertEquals(1, dto.items.size)
        assertEquals("OTD", dto.items.first().code)
    }

    @Test
    fun testMappingComplianceOverviewAndEvidenceToUiDto() {
        val overview = VendorPortalComplianceOverview(
            vendorId = "VND-UI-01",
            overallRiskLevel = ComplianceRiskLevel.LOW,
            overallComplianceStatus = ComplianceStatus.VERIFIED,
            totalRequirements = 8,
            compliantCount = 8,
            pendingCount = 0,
            nonCompliantCount = 0,
            expiredCertificationsCount = 0,
            upcomingExpiringCertificationsCount = 1,
            openCorrectiveActionsCount = 0,
            complianceRate = 100.0
        )

        val dto = overview.toDto()
        assertEquals("VND-UI-01", dto.vendorId)
        assertEquals(100.0, dto.complianceRate, 0.001)
        assertEquals(1, dto.upcomingExpiringCertificationsCount)
    }
}
