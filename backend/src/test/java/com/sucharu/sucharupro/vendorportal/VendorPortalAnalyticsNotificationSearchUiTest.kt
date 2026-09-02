package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalAnalyticsNotificationSearchUiTest {

    @Test
    fun testUnifiedAnalyticsHubDtoIntegrity() {
        val hubDto = VendorPortalUnifiedAnalyticsHubDto(
            vendorId = "VND-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            period = "LAST_30_DAYS",
            operational = VendorPortalOperationalAnalyticsDto(
                activePurchaseOrders = 5,
                openWorkOrders = 2,
                completedWorkOrders = 10,
                pendingDeliveryNotices = 1,
                onTimeDeliveryRate = 97.5,
                poFulfillmentRate = 99.0,
                recentActivityCount = 18
            ),
            financial = VendorPortalFinancialAnalyticsDto(
                submittedInvoicesCount = 8,
                approvedInvoicesCount = 6,
                paidInvoicesCount = 5,
                totalOutstandingAmount = 120000.0,
                totalDisputedAmount = 0.0,
                totalSettledAmount = 450000.0,
                currency = "BDT",
                paymentTrend = "ON_TRACK"
            ),
            quality = VendorPortalQualityAnalyticsDto(
                totalInspections = 12,
                passedQuantity = 5000.0,
                rejectedQuantity = 20.0,
                defectRate = 0.4,
                openRejectionCases = 0,
                activeDisputes = 0,
                openCapaCount = 0
            ),
            performance = VendorPortalPerformanceAnalyticsDto(
                overallScore = 96.5,
                qualityKpi = 98.0,
                onTimeDeliveryKpi = 97.5,
                fulfillmentKpi = 99.0,
                totalEvaluations = 4,
                performanceRating = "EXCELLENT"
            ),
            compliance = VendorPortalComplianceAnalyticsDto(
                complianceStatus = "COMPLIANT",
                totalCertifications = 5,
                expiringCertifications = 0,
                pendingRequirements = 0,
                overallRiskLevel = "LOW"
            ),
            collaboration = VendorPortalCollaborationAnalyticsDto(
                openBlockers = 0,
                unreadMessages = 2,
                pendingAcknowledgements = 1,
                openDisputes = 0,
                unresolvedItems = 1
            ),
            trends = listOf(
                VendorPortalTrendMetricDto(
                    metricKey = "OTD",
                    label = "On-Time Delivery",
                    currentValue = 97.5,
                    previousValue = 94.0,
                    delta = 3.5,
                    percentageDelta = 3.72,
                    direction = "IMPROVING",
                    unit = "%"
                )
            ),
            generatedAt = 1756291200000L
        )

        assertEquals("VND-001", hubDto.vendorId)
        assertEquals("LAST_30_DAYS", hubDto.period)
        assertEquals(5, hubDto.operational.activePurchaseOrders)
        assertEquals(450000.0, hubDto.financial.totalSettledAmount, 0.01)
        assertEquals("IMPROVING", hubDto.trends[0].direction)
    }

    @Test
    fun testUnifiedWorkspaceSummaryDtoIntegrity() {
        val summary = VendorPortalUnifiedWorkspaceSummaryDto(
            vendorId = "VND-001",
            vendorName = "Apex Steel Ltd",
            activePoCount = 4,
            pendingInvoiceCount = 2,
            openDisputeCount = 0,
            unreadNotificationCount = 3,
            overallPerformanceScore = 97.0,
            complianceStatus = "COMPLIANT",
            navigationSections = listOf(
                VendorPortalWorkspaceNavigationSectionDto("dashboard", "Dashboard", "/vendor-portal/dashboard", 4, true, "dashboard", 1),
                VendorPortalWorkspaceNavigationSectionDto("analytics", "Analytics Hub", "/vendor-portal/analytics", 0, true, "bar_chart", 2)
            )
        )

        assertEquals("Apex Steel Ltd", summary.vendorName)
        assertEquals(4, summary.activePoCount)
        assertEquals(2, summary.navigationSections.size)
        assertEquals(3, summary.unreadNotificationCount)
    }
}
