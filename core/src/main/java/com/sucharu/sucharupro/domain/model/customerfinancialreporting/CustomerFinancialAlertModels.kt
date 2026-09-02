package com.sucharu.sucharupro.domain.model.customerfinancialreporting

import java.util.UUID

/**
 * Controlled classifications for Customer Financial Alerts (Module 14 Step 12).
 */
enum class CustomerFinancialAlertType {
    INVOICE_DUE_SOON,
    INVOICE_DUE_TODAY,
    INVOICE_OVERDUE,
    PAYMENT_PROMISE_DUE,
    PAYMENT_PROMISE_MISSED,
    CREDIT_LIMIT_APPROACHING,
    CREDIT_LIMIT_EXCEEDED,
    FINANCIAL_HOLD_ACTIVE,
    FINANCIAL_HOLD_RELEASED,
    UNALLOCATED_PAYMENT,
    RECONCILIATION_DISCREPANCY,
    REFUND_PENDING,
    REFUND_COMPLETED,
    COLLECTION_ACTION_DUE,
    COLLECTION_ACTION_OVERDUE,
    FINANCIAL_REPORT_READY,
    FINANCIAL_DOCUMENT_READY
}

/**
 * Severity level deterministically calculated from canonical financial state.
 */
enum class CustomerFinancialAlertSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    val isUrgent: Boolean get() = this in setOf(HIGH, CRITICAL)
}

/**
 * Lifecycle status of a Customer Financial Alert.
 */
enum class CustomerFinancialAlertStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED,
    DISMISSED,
    EXPIRED;

    val isTerminal: Boolean get() = this in setOf(RESOLVED, DISMISSED, EXPIRED)
    val isActive: Boolean get() = this in setOf(OPEN, ACKNOWLEDGED)

    fun canTransitionTo(target: CustomerFinancialAlertStatus): Boolean {
        if (this == target) return true
        return when (this) {
            OPEN -> target in setOf(ACKNOWLEDGED, RESOLVED, DISMISSED, EXPIRED)
            ACKNOWLEDGED -> target in setOf(RESOLVED, DISMISSED, EXPIRED)
            RESOLVED, DISMISSED, EXPIRED -> false // Terminal states
        }
    }
}

/**
 * Append-only audit classification for alert lifecycle operations.
 */
enum class CustomerFinancialAlertEventType {
    ALERT_CREATED,
    ALERT_ACKNOWLEDGED,
    ALERT_RESOLVED,
    ALERT_DISMISSED,
    ALERT_EXPIRED,
    NOTIFICATION_TRIGGERED,
    NOTIFICATION_SENT,
    NOTIFICATION_FAILED,
    SCHEDULE_CREATED,
    SCHEDULE_UPDATED,
    SCHEDULE_PAUSED,
    SCHEDULE_RESUMED,
    SCHEDULE_CANCELLED,
    SCHEDULE_EXECUTION_STARTED,
    SCHEDULE_EXECUTION_SUCCEEDED,
    SCHEDULE_EXECUTION_FAILED
}

/**
 * Immutable audit event record for customer financial alerts.
 */
data class CustomerFinancialAlertAuditEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val projectId: String,
    val alertId: String,
    val eventType: CustomerFinancialAlertEventType,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val detailsJson: String = "{}"
)

/**
 * Canonical Customer Financial Alert Aggregate.
 */
data class CustomerFinancialAlert(
    val alertId: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val alertType: CustomerFinancialAlertType,
    val severity: CustomerFinancialAlertSeverity,
    val status: CustomerFinancialAlertStatus = CustomerFinancialAlertStatus.OPEN,
    val title: String,
    val safeMessage: String,
    val sourceType: String,
    val sourceId: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val dueAt: Long? = null,
    val resolvedAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val dismissedAt: Long? = null,
    val dismissedBy: String? = null,
    val dismissalReason: String? = null,
    val expiresAt: Long? = null,
    val correlationId: String? = null,
    val deduplicationKey: String,
    val metadata: Map<String, String> = emptyMap(),
    val version: Long = 1L
) {
    val isOpen: Boolean get() = status == CustomerFinancialAlertStatus.OPEN
    val isAcknowledged: Boolean get() = status == CustomerFinancialAlertStatus.ACKNOWLEDGED
    val isResolved: Boolean get() = status == CustomerFinancialAlertStatus.RESOLVED
    val isDismissed: Boolean get() = status == CustomerFinancialAlertStatus.DISMISSED
    val isExpired: Boolean get() = status == CustomerFinancialAlertStatus.EXPIRED || (expiresAt != null && System.currentTimeMillis() > expiresAt)

    companion object {
        fun buildDeduplicationKey(
            tenantId: String,
            projectId: String,
            customerId: String,
            alertType: CustomerFinancialAlertType,
            sourceType: String,
            sourceId: String
        ): String {
            return "$tenantId:$projectId:$customerId:${alertType.name}:$sourceType:$sourceId"
        }
    }
}

/**
 * Recurring frequency for scheduled financial reports.
 */
enum class CustomerFinancialScheduleFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * Status of a scheduled financial report delivery.
 */
enum class CustomerFinancialReportScheduleStatus {
    ACTIVE,
    PAUSED,
    CANCELLED;

    val isActive: Boolean get() = this == ACTIVE
    val isPaused: Boolean get() = this == PAUSED
    val isCancelled: Boolean get() = this == CANCELLED

    fun canTransitionTo(target: CustomerFinancialReportScheduleStatus): Boolean {
        if (this == target) return true
        return when (this) {
            ACTIVE -> target in setOf(PAUSED, CANCELLED)
            PAUSED -> target in setOf(ACTIVE, CANCELLED)
            CANCELLED -> false // Terminal
        }
    }
}

/**
 * Canonical Customer Financial Report Schedule Aggregate.
 */
data class CustomerFinancialReportSchedule(
    val scheduleId: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val reportType: CustomerFinancialReportType,
    val format: CustomerFinancialReportFormat = CustomerFinancialReportFormat.PDF,
    val frequency: CustomerFinancialScheduleFrequency,
    val timezone: String = "Asia/Dhaka",
    val status: CustomerFinancialReportScheduleStatus = CustomerFinancialReportScheduleStatus.ACTIVE,
    val nextRunAt: Long,
    val lastRunAt: Long? = null,
    val lastRunStatus: String? = null,
    val consecutiveFailures: Int = 0,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Execution status for scheduled report jobs.
 */
enum class CustomerFinancialScheduleExecutionStatus {
    SUCCESS,
    FAILED,
    SKIPPED
}

/**
 * Execution record for a single run of a scheduled report.
 */
data class CustomerFinancialScheduleExecution(
    val executionId: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val projectId: String,
    val scheduleId: String,
    val customerId: String,
    val reportType: CustomerFinancialReportType,
    val format: CustomerFinancialReportFormat,
    val executedAt: Long = System.currentTimeMillis(),
    val status: CustomerFinancialScheduleExecutionStatus,
    val documentDeliveryId: String? = null,
    val errorMessage: String? = null,
    val correlationId: String? = null
)

/**
 * Summary KPI of financial alerts for a customer or tenant.
 */
data class CustomerFinancialAlertSummary(
    val totalOpen: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val infoCount: Int = 0,
    val acknowledgedCount: Int = 0,
    val resolvedCount: Int = 0,
    val dismissedCount: Int = 0
)
