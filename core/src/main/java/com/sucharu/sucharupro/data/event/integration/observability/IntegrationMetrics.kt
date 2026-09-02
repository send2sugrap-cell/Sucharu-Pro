package com.sucharu.sucharupro.data.event.integration.observability

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe metrics collector for integration event routing and consumer execution (INFRA-04 Step 03).
 */
class IntegrationMetrics {

    private val totalEventsRouted = AtomicLong(0)
    private val totalConsumerExecutions = AtomicLong(0)
    private val totalConsumerSuccesses = AtomicLong(0)
    private val totalConsumerFailures = AtomicLong(0)
    private val totalDuplicatesSkipped = AtomicLong(0)

    private val notificationDeliveries = AtomicLong(0)
    private val realTimeDeliveries = AtomicLong(0)
    private val n8nDispatches = AtomicLong(0)
    private val aiAgentDeliveries = AtomicLong(0)

    private val totalDurationMs = AtomicLong(0)
    private val executionsByConsumer = ConcurrentHashMap<String, AtomicLong>()

    fun recordRoutedEvent() = totalEventsRouted.incrementAndGet()

    fun recordExecution(consumerId: String, durationMs: Long, isSuccess: Boolean, isDuplicate: Boolean) {
        totalConsumerExecutions.incrementAndGet()
        totalDurationMs.addAndGet(durationMs)
        executionsByConsumer.computeIfAbsent(consumerId) { AtomicLong(0) }.incrementAndGet()

        if (isDuplicate) {
            totalDuplicatesSkipped.incrementAndGet()
        } else if (isSuccess) {
            totalConsumerSuccesses.incrementAndGet()
        } else {
            totalConsumerFailures.incrementAndGet()
        }
    }

    fun recordNotificationDelivery() = notificationDeliveries.incrementAndGet()
    fun recordRealTimeDelivery() = realTimeDeliveries.incrementAndGet()
    fun recordN8nDispatch() = n8nDispatches.incrementAndGet()
    fun recordAiAgentDelivery() = aiAgentDeliveries.incrementAndGet()

    fun getSummary(): Map<String, Long> {
        val count = totalConsumerExecutions.get()
        val totalMs = totalDurationMs.get()
        val avgLatency = if (count > 0) totalMs / count else 0L

        return mapOf(
            "totalEventsRouted" to totalEventsRouted.get(),
            "totalConsumerExecutions" to count,
            "totalConsumerSuccesses" to totalConsumerSuccesses.get(),
            "totalConsumerFailures" to totalConsumerFailures.get(),
            "totalDuplicatesSkipped" to totalDuplicatesSkipped.get(),
            "notificationDeliveries" to notificationDeliveries.get(),
            "realTimeDeliveries" to realTimeDeliveries.get(),
            "n8nDispatches" to n8nDispatches.get(),
            "aiAgentDeliveries" to aiAgentDeliveries.get(),
            "avgExecutionLatencyMs" to avgLatency
        )
    }

    fun reset() {
        totalEventsRouted.set(0)
        totalConsumerExecutions.set(0)
        totalConsumerSuccesses.set(0)
        totalConsumerFailures.set(0)
        totalDuplicatesSkipped.set(0)
        notificationDeliveries.set(0)
        realTimeDeliveries.set(0)
        n8nDispatches.set(0)
        aiAgentDeliveries.set(0)
        totalDurationMs.set(0)
        executionsByConsumer.clear()
    }
}
