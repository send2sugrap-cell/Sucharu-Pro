package com.sucharu.sucharupro.domain.model.report

import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability

/**
 * Module 24 — Canonical Report Categories across the Sucharu Pro enterprise ecosystem.
 */
enum class ReportCategory(
    val categoryId: String,
    val displayName: String,
    val authoritativeModule: String
) {
    SALES("SALES", "Sales & Revenue", "Module 03 Quotation & Order"),
    CUSTOMER("CUSTOMER", "Customer & Receivables", "Module 02 Customer"),
    ORDER("ORDER", "Order Lifecycle & Fulfillment", "Module 03 Order"),
    PRODUCTION("PRODUCTION", "Production & Job Costing", "Module 04 Production"),
    QUALITY("QUALITY", "Quality Control & Rework", "Module 06 Quality Control"),
    INVENTORY("INVENTORY", "Finished Inventory & Stock", "Module 07 Inventory"),
    DELIVERY("DELIVERY", "Delivery & Dispatch", "Module 08 Delivery"),
    FINANCE("FINANCE", "Financial Ledgers & Statements", "Module 09 Finance"),
    PROFITABILITY("PROFITABILITY", "Profitability & Margin Analytics", "Module 09/15 Profitability"),
    AFFILIATE("AFFILIATE", "Affiliate Performance & Referral", "Module 20 Affiliate"),
    WALLET_PAYOUT("WALLET_PAYOUT", "Wallet Accounting & Payouts", "Module 23 Wallet & Payout"),
    MACHINE_OPERATIONS("MACHINE_OPERATIONS", "Machine OEE & Operational Health", "Module 21 Machine & OEE"),
    PREFLIGHT("PREFLIGHT", "Preflight Diagnostics & Proofing", "Module 22 Preflight Engine"),
    AUDIT("AUDIT", "Enterprise Audit & Governance", "Subsystem Audit Logs"),
    EXECUTIVE_ANALYTICS("EXECUTIVE_ANALYTICS", "Executive KPI & Strategic Analytics", "Cross-Module Executive Aggregations")
}

/**
 * Supported report period aggregation intervals.
 */
enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    ALL_TIME,
    CUSTOM
}

/**
 * Level of aggregation requested in report response.
 */
enum class ReportAggregationLevel {
    SUMMARY,
    DETAILED,
    TIME_SERIES,
    CATEGORY_BREAKDOWN
}

/**
 * Report export document formats.
 */
enum class ReportExportFormat(
    val extension: String,
    val mimeType: String
) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
    PDF("pdf", "application/pdf"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
}

/**
 * Descriptor definition for a registered canonical report.
 */
data class CanonicalReportDefinition(
    val reportTypeId: String,
    val category: ReportCategory,
    val displayName: String,
    val description: String,
    val authoritativeModule: String,
    val requiredCapability: AuthorizationCapability,
    val supportedFilters: List<String> = listOf("fromDate", "toDate", "status"),
    val defaultMetrics: List<String> = emptyList()
)

/**
 * Deterministic client request contract for executing a report query.
 */
data class ReportRequest(
    val reportCategory: ReportCategory,
    val reportType: String,
    val tenantId: String,
    val projectId: String,
    val fromDate: String? = null,
    val toDate: String? = null,
    val period: ReportPeriod = ReportPeriod.MONTHLY,
    val filters: Map<String, String> = emptyMap(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val sortBy: String? = null,
    val sortDirection: String = "ASC",
    val requestedMetrics: List<String> = emptyList(),
    val aggregationLevel: ReportAggregationLevel = ReportAggregationLevel.SUMMARY
)

/**
 * KPI / Summary Metric descriptor within a report response.
 */
data class ReportMetric(
    val metricId: String,
    val label: String,
    val value: String,
    val numericValue: Double? = null,
    val formattedValue: String = value,
    val unit: String? = null,
    val trendPercentage: Double? = null,
    val status: String? = null
)

/**
 * Metadata column descriptor for table grid view.
 */
data class ReportDataColumn(
    val key: String,
    val label: String,
    val dataType: String = "STRING"
)

/**
 * Single data row in report result set.
 */
data class ReportDataRow(
    val id: String? = null,
    val values: Map<String, String> = emptyMap()
)

/**
 * Single data point for chart visualizations.
 */
data class ReportChartDataPoint(
    val label: String,
    val timestamp: Long? = null,
    val value: Double,
    val group: String? = null
)

/**
 * Named chart series containing data points.
 */
data class ReportChartSeries(
    val seriesName: String,
    val points: List<ReportChartDataPoint> = emptyList()
)

/**
 * Pagination metadata for report query response.
 */
data class ReportPaginationMeta(
    val currentPage: Int = 1,
    val pageSize: Int = 50,
    val totalRows: Int = 0,
    val totalPages: Int = 1
)

/**
 * Execution metadata describing report generation context.
 */
data class ReportExecutionMeta(
    val reportCategory: ReportCategory,
    val reportType: String,
    val tenantId: String,
    val projectId: String,
    val generatedAt: String,
    val executionTimeMs: Long,
    val authoritativeModule: String
)

/**
 * Canonical Response contract returned by Module 24 Reporting API.
 */
data class ReportResponse(
    val meta: ReportExecutionMeta,
    val summaryMetrics: List<ReportMetric> = emptyList(),
    val columns: List<ReportDataColumn> = emptyList(),
    val rows: List<ReportDataRow> = emptyList(),
    val chartSeries: List<ReportChartSeries> = emptyList(),
    val pagination: ReportPaginationMeta = ReportPaginationMeta(),
    val appliedFilters: Map<String, String> = emptyMap()
)

/**
 * Catalogue item representing an available report to a user.
 */
data class ReportCatalogueItem(
    val category: ReportCategory,
    val reportType: String,
    val displayName: String,
    val description: String,
    val authoritativeModule: String,
    val requiredCapability: AuthorizationCapability,
    val defaultMetrics: List<String> = emptyList()
)

/**
 * Grouped category summary in catalogue response.
 */
data class ReportCategorySummary(
    val category: ReportCategory,
    val displayName: String,
    val reports: List<ReportCatalogueItem> = emptyList()
)

/**
 * Complete Report Catalogue response contract.
 */
data class ReportCatalogueResponse(
    val totalCategories: Int,
    val totalReports: Int,
    val categories: List<ReportCategorySummary> = emptyList()
)

/**
 * Request payload to export a report.
 */
data class ExportReportRequest(
    val queryRequest: ReportRequest,
    val format: ReportExportFormat = ReportExportFormat.CSV,
    val includeCharts: Boolean = false
)

/**
 * Generated document payload returned by export orchestration.
 */
data class ReportExportDocument(
    val reportType: String,
    val format: ReportExportFormat,
    val fileName: String,
    val mimeType: String,
    val contentBase64: String? = null,
    val contentLength: Long = 0,
    val generatedAt: String
)
