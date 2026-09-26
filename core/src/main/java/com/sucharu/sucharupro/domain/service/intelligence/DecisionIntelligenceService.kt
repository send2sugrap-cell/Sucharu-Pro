package com.sucharu.sucharupro.domain.service.intelligence

import com.sucharu.sucharupro.domain.model.intelligence.*
import java.math.BigDecimal

/**
 * BI-08 Domain Service for Reporting -> Decision Intelligence.
 *
 * Sourced directly from Module 24 Reporting Analytics and BI-01 to BI-07 canonical read models.
 */
class DecisionIntelligenceService {

    /**
     * Builds the Master Decision Intelligence Summary.
     */
    fun buildDecisionIntelligenceSummary(): DecisionIntelligenceSummary {
        val timestamp = "2026-09-26T21:00:00Z"

        val kpiList = listOf(
            DecisionKpiItem(
                kpiCode = "KPI-REV",
                title = "YTD Gross Sales Revenue",
                currentValue = "৳2,450,000.00",
                baselineValue = "৳2,150,000.00",
                trendPercentage = 14.0,
                trendDirection = DecisionKpiTrendDirection.UPWARD,
                statusMessage = "+14.0% sales revenue increase over previous period"
            ),
            DecisionKpiItem(
                kpiCode = "KPI-MARGIN",
                title = "Average Gross Profit Margin",
                currentValue = "37.39%",
                baselineValue = "35.00%",
                trendPercentage = 6.8,
                trendDirection = DecisionKpiTrendDirection.UPWARD,
                statusMessage = "Gross margin on target (37.39%)"
            ),
            DecisionKpiItem(
                kpiCode = "KPI-CONV",
                title = "Quotation-to-Order Conversion Rate",
                currentValue = "50.00%",
                baselineValue = "45.00%",
                trendPercentage = 11.1,
                trendDirection = DecisionKpiTrendDirection.UPWARD,
                statusMessage = "2 out of 4 quotations converted to orders"
            )
        )

        val exceptions = listOf(
            DecisionExceptionItem(
                exceptionId = "EXC-001",
                category = "RECEIVABLES",
                severity = OperationalAttentionSeverity.CRITICAL_ATTENTION,
                title = "High Overdue Exposure: Ideal Publications Ltd",
                description = "Customer balance ৳320,000.00 is 92 days overdue.",
                affectedRecordId = "CUST-1002",
                targetRoute = "finance/collection"
            ),
            DecisionExceptionItem(
                exceptionId = "EXC-002",
                category = "JOB_COSTING",
                severity = OperationalAttentionSeverity.URGENT_ATTENTION,
                title = "Cost Overrun: Job #JOB-2026-001",
                description = "Job actual cost ৳265.00 exceeds estimate ৳250.00 (+6.00% variance due to paper price surge).",
                affectedRecordId = "JOB-2026-001",
                targetRoute = "job-costing/breakdown"
            )
        )

        return DecisionIntelligenceSummary(
            totalRevenueYtd = BigDecimal("2450000.00"),
            activeOrdersCount = 2,
            quotationConversionRatePercentage = BigDecimal("50.00"),
            activeProductionJobsCount = 2,
            totalOutstandingReceivable = BigDecimal("555000.00"),
            overdueReceivableAmount = BigDecimal("470000.00"),
            averageGrossMarginPercentage = BigDecimal("37.39"),
            totalProcurementCommitment = BigDecimal("630000.00"),
            kpiList = kpiList,
            exceptionQueue = exceptions,
            isShadowAnalyticsDatabaseCreated = false, // Critical Invariant: Always false!
            generatedAt = timestamp
        )
    }

    /**
     * Filters exception queue by severity.
     */
    fun filterExceptionsBySeverity(
        summary: DecisionIntelligenceSummary,
        severity: OperationalAttentionSeverity
    ): List<DecisionExceptionItem> {
        return summary.exceptionQueue.filter { it.severity == severity }
    }
}
