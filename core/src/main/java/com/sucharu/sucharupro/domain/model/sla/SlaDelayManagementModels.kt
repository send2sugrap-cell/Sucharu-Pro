package com.sucharu.sucharupro.domain.model.sla

import java.math.BigDecimal

/**
 * BI-09 Factual SLA Status Lifecycle.
 */
enum class SlaStatus {
    NOT_STARTED,
    ON_TRACK,
    AT_RISK,
    DUE_TODAY,
    OVERDUE,
    COMPLETED_ON_TIME,
    COMPLETED_LATE
}

/**
 * BI-09 Controlled Delay Reason Categories.
 */
enum class DelayReasonCategory {
    CUSTOMER_APPROVAL_DELAY,
    DESIGN_CHANGE,
    QC_REWORK,
    PRODUCTION_DELAY,
    MACHINE_OPERATION_DELAY,
    OUTSOURCED_FINISHING_DELAY,
    DELIVERY_DISPATCH_DELAY,
    CUSTOMER_UNAVAILABLE,
    PAYMENT_HOLD,
    MATERIAL_SUPPLIER_DEPENDENCY,
    OTHER
}

/**
 * SLA Order Commitment Record.
 */
data class SlaOrderCommitment(
    val commitmentId: String,
    val orderId: String,
    val jobId: String? = null,
    val customerId: String,
    val customerName: String,
    val promisedDeliveryDate: String,
    val plannedProductionCompletionDate: String? = null,
    val actualProductionCompletionDate: String? = null,
    val actualDeliveryDate: String? = null,

    val slaStatus: SlaStatus = SlaStatus.ON_TRACK,
    val delayDays: Int = 0,
    val currentStage: String = "PRINTING",

    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(commitmentId.isNotBlank()) { "Commitment ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(promisedDeliveryDate.isNotBlank()) { "Promised delivery date cannot be blank." }
    }
}

/**
 * Auditable SLA Delay Event Record.
 */
data class SlaDelayRecord(
    val delayRecordId: String,
    val commitmentId: String,
    val orderId: String,
    val delayCategory: DelayReasonCategory,
    val responsibleStage: String = "PRODUCTION",
    val delayDurationDays: Int = 1,
    val rootCauseDescription: String,
    val recordedAt: String,
    val recordedBy: String
)

/**
 * BI-09 Master SLA & Delay Management Summary.
 */
data class SlaManagementSummary(
    val totalMonitoredOrdersCount: Int,
    val onTrackOrdersCount: Int,
    val atRiskOrdersCount: Int,
    val overdueOrdersCount: Int,
    val completedOnTimeCount: Int,
    val completedLateCount: Int,
    val averageDelayDays: Double,
    val slaOnTimePerformancePercentage: BigDecimal,
    val exceptionQueue: List<SlaOrderCommitment> = emptyList(),
    val delayRecords: List<SlaDelayRecord> = emptyList(),
    val generatedAt: String
)
