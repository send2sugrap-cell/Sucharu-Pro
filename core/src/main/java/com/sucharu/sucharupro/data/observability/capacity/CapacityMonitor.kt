package com.sucharu.sucharupro.data.observability.capacity

import com.sucharu.sucharupro.domain.observability.CapacitySnapshot
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus

/**
 * Capacity & Queue Readiness Monitor (INFRA-04 Step 09).
 */
class CapacityMonitor {

    fun captureSnapshot(
        outboxDepth: Long = 0L,
        notificationQueueDepth: Long = 0L,
        jobQueueDepth: Long = 0L,
        activeWorkflowsCount: Long = 0L,
        providerThroughputPerSec: Double = 100.0
    ): CapacitySnapshot {
        val totalPending = outboxDepth + notificationQueueDepth + jobQueueDepth
        val drainRate = if (providerThroughputPerSec > 0.0) providerThroughputPerSec else 1.0
        val timeToDrainSec = (totalPending / drainRate).toLong()

        val status = when {
            totalPending > 5000 || timeToDrainSec > 300 -> OperationalHealthStatus.CRITICAL
            totalPending > 1000 || timeToDrainSec > 60 -> OperationalHealthStatus.DEGRADED
            else -> OperationalHealthStatus.HEALTHY
        }

        return CapacitySnapshot(
            timestamp = System.currentTimeMillis(),
            outboxDepth = outboxDepth,
            notificationQueueDepth = notificationQueueDepth,
            jobQueueDepth = jobQueueDepth,
            activeWorkflowsCount = activeWorkflowsCount,
            providerThroughputPerSec = providerThroughputPerSec,
            estimatedTimeToDrainSec = timeToDrainSec,
            status = status
        )
    }
}
