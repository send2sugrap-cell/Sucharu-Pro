package com.sucharu.sucharupro.domain.service.businessfinancialreporting

import com.sucharu.sucharupro.domain.model.businessfinancialreporting.*

/**
 * Service interface for Business Financial Reporting, Analytics & Management Intelligence.
 * Operates purely in a read-only manner against canonical financial repositories (Steps 01–07).
 */
interface BusinessFinancialReportingService {
    suspend fun generateExecutiveSummary(filter: BusinessFinancialReportFilter): BusinessExecutiveFinancialSummary
    suspend fun generateExpenseAnalytics(filter: BusinessFinancialReportFilter): BusinessExpenseAnalyticsReport
    suspend fun generateVendorPayableAnalytics(filter: BusinessFinancialReportFilter): VendorPayableAnalyticsReport
    suspend fun generateLedgerReport(filter: BusinessFinancialReportFilter): BusinessLedgerReport
    suspend fun generateCostCenterReport(filter: BusinessFinancialReportFilter): BusinessCostCenterReport
    suspend fun generateProjectCostReport(filter: BusinessFinancialReportFilter): JobProjectCostReport
    suspend fun generateCommitmentAccrualReport(filter: BusinessFinancialReportFilter): CommitmentAccrualReport
    suspend fun generateReconciliationReport(filter: BusinessFinancialReportFilter): BusinessReconciliationReport
    suspend fun generateAdjustmentReport(filter: BusinessFinancialReportFilter): BusinessFinancialAdjustmentReport
    suspend fun generatePeriodEndReadinessReport(tenantId: String, projectId: String, periodId: String): BusinessPeriodEndReadinessReport

    suspend fun createReportSnapshot(filter: BusinessFinancialReportFilter, metricsJson: String, generatedBy: String): BusinessFinancialReportSnapshot
    suspend fun getReportSnapshot(tenantId: String, snapshotId: String): BusinessFinancialReportSnapshot?
    suspend fun listReportSnapshots(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType? = null,
        periodId: String? = null,
        limit: Int = 50
    ): List<BusinessFinancialReportSnapshot>

    suspend fun exportReport(filter: BusinessFinancialReportFilter): BusinessFinancialExportDocument
}
