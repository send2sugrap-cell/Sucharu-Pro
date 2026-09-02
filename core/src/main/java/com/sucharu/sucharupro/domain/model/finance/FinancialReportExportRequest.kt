package com.sucharu.sucharupro.domain.model.finance

/**
 * Format options for financial report export.
 */
enum class FinancialReportExportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
}

/**
 * Immutable export request for financial reports (Module 09 Step 09).
 */
data class FinancialReportExportRequest(
    val exportId: String,
    val projectId: String,
    val reportType: FinancialReportType,
    val format: FinancialReportExportFormat,
    val filter: FinancialReportFilter,
    val requestedBy: String,
    val requestedAt: Long = System.currentTimeMillis()
) {
    init {
        require(exportId.isNotBlank()) { "Export ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(requestedBy.isNotBlank()) { "Requested by cannot be blank." }
        require(requestedAt > 0) { "Requested timestamp must be positive." }
    }
}
