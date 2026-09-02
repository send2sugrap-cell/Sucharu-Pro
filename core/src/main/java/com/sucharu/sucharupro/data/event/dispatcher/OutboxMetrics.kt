package com.sucharu.sucharupro.data.event.dispatcher

import java.util.concurrent.atomic.AtomicLong

/**
 * Production-grade outbox observability metrics collector (INFRA-04 Step 02).
 */
class OutboxMetrics {
    private val totalClaimed = AtomicLong(0)
    private val totalPublished = AtomicLong(0)
    private val totalRetried = AtomicLong(0)
    private val totalDeadLettered = AtomicLong(0)
    private val totalExecutionDurationMs = AtomicLong(0)
    private val totalDispatchedEvents = AtomicLong(0)

    fun recordClaimed(count: Int) {
        totalClaimed.addAndGet(count.toLong())
    }

    fun recordPublished(durationMs: Long) {
        totalPublished.incrementAndGet()
        totalDispatchedEvents.incrementAndGet()
        totalExecutionDurationMs.addAndGet(durationMs)
    }

    fun recordRetryScheduled(durationMs: Long) {
        totalRetried.incrementAndGet()
        totalDispatchedEvents.incrementAndGet()
        totalExecutionDurationMs.addAndGet(durationMs)
    }

    fun recordDeadLettered(durationMs: Long) {
        totalDeadLettered.incrementAndGet()
        totalDispatchedEvents.incrementAndGet()
        totalExecutionDurationMs.addAndGet(durationMs)
    }

    val claimedCount: Long get() = totalClaimed.get()
    val publishedCount: Long get() = totalPublished.get()
    val retriedCount: Long get() = totalRetried.get()
    val deadLetterCount: Long get() = totalDeadLettered.get()

    val averageLatencyMs: Double
        get() {
            val total = totalDispatchedEvents.get()
            return if (total > 0) totalExecutionDurationMs.get().toDouble() / total else 0.0
        }

    fun reset() {
        totalClaimed.set(0)
        totalPublished.set(0)
        totalRetried.set(0)
        totalDeadLettered.set(0)
        totalExecutionDurationMs.set(0)
        totalDispatchedEvents.set(0)
    }
}
