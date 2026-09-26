package com.sucharu.sucharupro.domain.service.sla

import com.sucharu.sucharupro.domain.model.sla.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * BI-09 Domain Service for SLA / Delay Management & Exception Queue Governance.
 */
class SlaDelayManagementService {

    private val commitmentStore = ConcurrentHashMap<String, SlaOrderCommitment>()
    private val delayRecordsStore = ConcurrentHashMap<String, SlaDelayRecord>()

    init {
        // Default Sample SLA Order Commitments
        val c1 = SlaOrderCommitment(
            commitmentId = "SLA-2026-001",
            orderId = "ORD-1001",
            jobId = "JOB-2026-001",
            customerId = "CUST-1001",
            customerName = "Dhaka Printing Press & Media",
            promisedDeliveryDate = "2026-09-25T18:00:00Z",
            plannedProductionCompletionDate = "2026-09-24T18:00:00Z",
            slaStatus = SlaStatus.OVERDUE,
            delayDays = 1,
            currentStage = "PRINTING",
            createdAt = "2026-09-20T10:00:00Z",
            updatedAt = "2026-09-26T14:00:00Z",
            createdBy = "SYSTEM"
        )
        val c2 = SlaOrderCommitment(
            commitmentId = "SLA-2026-002",
            orderId = "ORD-1002",
            jobId = "JOB-2026-002",
            customerId = "CUST-1002",
            customerName = "Ideal Publications Ltd",
            promisedDeliveryDate = "2026-09-30T18:00:00Z",
            plannedProductionCompletionDate = "2026-09-28T18:00:00Z",
            slaStatus = SlaStatus.ON_TRACK,
            delayDays = 0,
            currentStage = "LAMINATION",
            createdAt = "2026-09-22T10:00:00Z",
            updatedAt = "2026-09-26T14:00:00Z",
            createdBy = "SYSTEM"
        )

        val d1 = SlaDelayRecord(
            delayRecordId = "DEL-001",
            commitmentId = "SLA-2026-001",
            orderId = "ORD-1001",
            delayCategory = DelayReasonCategory.MACHINE_OPERATION_DELAY,
            responsibleStage = "PRINTING",
            delayDurationDays = 1,
            rootCauseDescription = "Heidelberg press setup paper realignment delay",
            recordedAt = "2026-09-25T19:00:00Z",
            recordedBy = "STAFF-001"
        )

        commitmentStore[c1.commitmentId] = c1
        commitmentStore[c2.commitmentId] = c2
        delayRecordsStore[d1.delayRecordId] = d1
    }

    suspend fun createCommitment(commitment: SlaOrderCommitment): SlaOrderCommitment {
        commitmentStore[commitment.commitmentId] = commitment
        return commitment
    }

    suspend fun recordDelayReason(
        commitmentId: String,
        orderId: String,
        category: DelayReasonCategory,
        responsibleStage: String = "PRODUCTION",
        delayDurationDays: Int = 1,
        rootCauseDescription: String,
        actorId: String = "STAFF-001"
    ): SlaDelayRecord {
        val recordId = "DEL-" + UUID.randomUUID().toString().take(8).uppercase()
        val timestamp = "2026-09-26T20:45:00Z"

        val delayRecord = SlaDelayRecord(
            delayRecordId = recordId,
            commitmentId = commitmentId,
            orderId = orderId,
            delayCategory = category,
            responsibleStage = responsibleStage,
            delayDurationDays = delayDurationDays,
            rootCauseDescription = rootCauseDescription,
            recordedAt = timestamp,
            recordedBy = actorId
        )

        // Update SLA Commitment Status
        commitmentStore[commitmentId]?.let { current ->
            commitmentStore[commitmentId] = current.copy(
                slaStatus = SlaStatus.OVERDUE,
                delayDays = current.delayDays + delayDurationDays,
                updatedAt = timestamp,
                updatedBy = actorId
            )
        }

        delayRecordsStore[recordId] = delayRecord
        return delayRecord
    }

    suspend fun buildSlaManagementSummary(): SlaManagementSummary {
        val timestamp = "2026-09-26T20:45:00Z"
        val commitments = commitmentStore.values.toList()
        val delayRecords = delayRecordsStore.values.toList()

        val totalMonitored = commitments.size
        val onTrack = commitments.count { it.slaStatus == SlaStatus.ON_TRACK }
        val atRisk = commitments.count { it.slaStatus == SlaStatus.AT_RISK }
        val overdue = commitments.count { it.slaStatus == SlaStatus.OVERDUE || it.slaStatus == SlaStatus.COMPLETED_LATE }
        val completedOnTime = commitments.count { it.slaStatus == SlaStatus.COMPLETED_ON_TIME }
        val completedLate = commitments.count { it.slaStatus == SlaStatus.COMPLETED_LATE }

        val avgDelay = if (commitments.isNotEmpty()) {
            commitments.map { it.delayDays.toDouble() }.average()
        } else {
            0.0
        }

        val onTimePct = if (totalMonitored > 0) {
            BigDecimal(onTrack + completedOnTime).multiply(BigDecimal("100.00")).divide(BigDecimal(totalMonitored), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal("100.00")
        }

        val exceptions = commitments.filter { it.slaStatus == SlaStatus.AT_RISK || it.slaStatus == SlaStatus.OVERDUE }

        return SlaManagementSummary(
            totalMonitoredOrdersCount = totalMonitored,
            onTrackOrdersCount = onTrack,
            atRiskOrdersCount = atRisk,
            overdueOrdersCount = overdue,
            completedOnTimeCount = completedOnTime,
            completedLateCount = completedLate,
            averageDelayDays = avgDelay,
            slaOnTimePerformancePercentage = onTimePct,
            exceptionQueue = exceptions,
            delayRecords = delayRecords,
            generatedAt = timestamp
        )
    }

    suspend fun filterCommitmentsByStatus(status: SlaStatus): List<SlaOrderCommitment> {
        return commitmentStore.values.filter { it.slaStatus == status }
    }
}
