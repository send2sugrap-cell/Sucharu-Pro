package com.sucharu.sucharupro.domain.model.finance

/**
 * Immutable audit event for financial report actions (Module 09 Step 09).
 *
 * Only meaningful financial actions are audited — not internal UI recompositions.
 */
data class FinancialReportActivityEvent(
    val eventId: String,
    val projectId: String,
    val eventType: FinancialReportEventType,
    val reportType: FinancialReportType,
    val reportId: String,
    val performedBy: String,
    val snapshotId: String? = null,
    val controlException: String? = null,
    val metadata: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(reportId.isNotBlank()) { "Report ID cannot be blank." }
        require(performedBy.isNotBlank()) { "Performed by cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}

/**
 * Audit event types for financial reports (Module 09 Step 09).
 */
enum class FinancialReportEventType(val defaultLabel: String) {
    REPORT_GENERATED("Report Generated"),
    REPORT_VIEWED("Report Viewed"),
    REPORT_FILTERED("Report Filtered"),
    REPORT_SNAPSHOT_CREATED("Snapshot Created"),
    REPORT_EXPORT_REQUESTED("Export Requested"),
    REPORT_CONTROL_EXCEPTION("Control Exception"),
    REPORT_SUPERSEDED("Report Superseded")
}
