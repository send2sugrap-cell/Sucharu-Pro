package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalAnalyticsDomainTest {

    @Test
    fun testTrendMetricCalculationImproving() {
        val metric = VendorPortalTrendMetric.calculate(
            metricKey = "OTD_RATE",
            label = "On-Time Delivery Rate",
            current = 98.5,
            previous = 92.0,
            higherIsBetter = true,
            unit = "%"
        )

        assertEquals("OTD_RATE", metric.metricKey)
        assertEquals(98.5, metric.currentValue, 0.001)
        assertEquals(92.0, metric.previousValue, 0.001)
        assertEquals(6.5, metric.delta, 0.01)
        assertEquals(VendorPortalTrendDirection.IMPROVING, metric.direction)
    }

    @Test
    fun testTrendMetricCalculationDeclining() {
        val metric = VendorPortalTrendMetric.calculate(
            metricKey = "DEFECT_RATE",
            label = "Quality Defect Rate",
            current = 3.5,
            previous = 1.0,
            higherIsBetter = false,
            unit = "%"
        )

        assertEquals("DEFECT_RATE", metric.metricKey)
        assertEquals(2.5, metric.delta, 0.01)
        assertEquals(VendorPortalTrendDirection.DECLINING, metric.direction)
    }

    @Test
    fun testTrendMetricCalculationZeroPreviousSafe() {
        val metric = VendorPortalTrendMetric.calculate(
            metricKey = "ACTIVE_BLOCKERS",
            label = "Active Blockers",
            current = 0.0,
            previous = 0.0,
            higherIsBetter = false
        )

        assertEquals(0.0, metric.delta, 0.001)
        assertEquals(0.0, metric.percentageDelta, 0.001)
        assertEquals(VendorPortalTrendDirection.STABLE, metric.direction)
    }

    @Test
    fun testUnifiedAnalyticsHubInvariants() {
        val hub = VendorPortalUnifiedAnalyticsHub(
            vendorId = "VND-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            period = VendorPortalPeriod.LAST_30_DAYS,
            operational = VendorPortalOperationalAnalytics(
                activePurchaseOrders = 5,
                openWorkOrders = 2,
                completedWorkOrders = 10,
                pendingDeliveryNotices = 1,
                onTimeDeliveryRate = 97.5,
                poFulfillmentRate = 99.0,
                recentActivityCount = 18
            ),
            financial = VendorPortalFinancialAnalytics(
                submittedInvoicesCount = 8,
                approvedInvoicesCount = 6,
                paidInvoicesCount = 5,
                totalOutstandingAmount = Money(BigDecimal("120000.00")),
                totalDisputedAmount = Money.ZERO,
                totalSettledAmount = Money(BigDecimal("450000.00"))
            ),
            quality = VendorPortalQualityAnalytics(
                totalInspections = 12,
                passedQuantity = 5000.0,
                rejectedQuantity = 20.0,
                defectRate = 0.4,
                openRejectionCases = 0,
                activeDisputes = 0,
                openCapaCount = 0
            ),
            performance = VendorPortalPerformanceAnalytics(
                overallScore = 96.5,
                qualityKpi = 98.0,
                onTimeDeliveryKpi = 97.5,
                fulfillmentKpi = 99.0,
                totalEvaluations = 4,
                performanceRating = "EXCELLENT"
            ),
            compliance = VendorPortalComplianceAnalytics(
                complianceStatus = "COMPLIANT",
                totalCertifications = 5,
                expiringCertifications = 0,
                pendingRequirements = 0,
                overallRiskLevel = "LOW"
            ),
            collaboration = VendorPortalCollaborationAnalytics(
                openBlockers = 0,
                unreadMessages = 2,
                pendingAcknowledgements = 1,
                openDisputes = 0,
                unresolvedItems = 1
            ),
            trends = emptyList()
        )

        assertEquals("VND-001", hub.vendorId)
        assertEquals(VendorPortalPeriod.LAST_30_DAYS, hub.period)
        assertEquals(5, hub.operational.activePurchaseOrders)
        assertEquals(0.4, hub.quality.defectRate, 0.001)
        assertEquals("COMPLIANT", hub.compliance.complianceStatus)
        assertTrue(hub.financial.totalSettledAmount.amount > BigDecimal.ZERO)
    }
}
