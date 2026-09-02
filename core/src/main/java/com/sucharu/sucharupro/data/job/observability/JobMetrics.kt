package com.sucharu.sucharupro.data.job.observability

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe metrics collector for background job execution (INFRA-04 Step 04).
 */
class JobMetrics {
    private val enqueuedJobs = AtomicLong(0)
    private val claimedJobs = AtomicLong(0)
    private val succeededJobs = AtomicLong(0)
    private val failedJobs = AtomicLong(0)
    private val retriedJobs = AtomicLong(0)
    private val deadLetterJobs = AtomicLong(0)
    private val cancelledJobs = AtomicLong(0)
    private val recoveredLeases = AtomicLong(0)
    private val totalExecutionTimeMs = AtomicLong(0)

    private val jobsByType = ConcurrentHashMap<String, AtomicLong>()
    private val jobsByTenant = ConcurrentHashMap<String, AtomicLong>()

    fun recordEnqueued(jobType: String, projectId: String) {
        enqueuedJobs.incrementAndGet()
        jobsByType.computeIfAbsent(jobType) { AtomicLong(0) }.incrementAndGet()
        jobsByTenant.computeIfAbsent(projectId) { AtomicLong(0) }.incrementAndGet()
    }

    fun recordClaimed() = claimedJobs.incrementAndGet()
    fun recordSucceeded(durationMs: Long) {
        succeededJobs.incrementAndGet()
        totalExecutionTimeMs.addAndGet(durationMs)
    }
    fun recordFailed() = failedJobs.incrementAndGet()
    fun recordRetried() = retriedJobs.incrementAndGet()
    fun recordDeadLetter() = deadLetterJobs.incrementAndGet()
    fun recordCancelled() = cancelledJobs.incrementAndGet()
    fun recordRecoveredLeases(count: Int) = recoveredLeases.addAndGet(count.toLong())

    fun getEnqueuedCount(): Long = enqueuedJobs.get()
    fun getClaimedCount(): Long = claimedJobs.get()
    fun getSucceededCount(): Long = succeededJobs.get()
    fun getFailedCount(): Long = failedJobs.get()
    fun getRetriedCount(): Long = retriedJobs.get()
    fun getDeadLetterCount(): Long = deadLetterJobs.get()
    fun getCancelledCount(): Long = cancelledJobs.get()
    fun getRecoveredLeaseCount(): Long = recoveredLeases.get()

    fun getAverageExecutionLatencyMs(): Double {
        val total = succeededJobs.get()
        return if (total > 0) totalExecutionTimeMs.get().toDouble() / total else 0.0
    }

    fun reset() {
        enqueuedJobs.set(0)
        claimedJobs.set(0)
        succeededJobs.set(0)
        failedJobs.set(0)
        retriedJobs.set(0)
        deadLetterJobs.set(0)
        cancelledJobs.set(0)
        recoveredLeases.set(0)
        totalExecutionTimeMs.set(0)
        jobsByType.clear()
        jobsByTenant.clear()
    }
}
