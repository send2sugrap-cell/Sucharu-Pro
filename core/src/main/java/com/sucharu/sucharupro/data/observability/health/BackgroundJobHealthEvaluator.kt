package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.data.job.observability.JobMetrics
import com.sucharu.sucharupro.domain.observability.BackgroundJobHealth
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus

/**
 * Health evaluator for Background Job processing (INFRA-04 Step 09).
 */
class BackgroundJobHealthEvaluator(
    private val jobMetrics: JobMetrics = JobMetrics()
) {

    fun evaluate(
        pendingJobsCount: Long = 0L,
        processingJobsCount: Long = 0L
    ): BackgroundJobHealth {
        val issues = mutableListOf<String>()

        val enqueued = jobMetrics.getEnqueuedCount()
        val succeeded = jobMetrics.getSucceededCount()
        val failed = jobMetrics.getFailedCount()
        val retried = jobMetrics.getRetriedCount()
        val deadLetter = jobMetrics.getDeadLetterCount()
        val recoveredLeases = jobMetrics.getRecoveredLeaseCount()
        val avgLatency = jobMetrics.getAverageExecutionLatencyMs()

        var status = OperationalHealthStatus.HEALTHY
        if (deadLetter > 50 || pendingJobsCount > 1000) {
            status = OperationalHealthStatus.CRITICAL
            if (deadLetter > 50) issues.add("Critical dead letter job backlog: $deadLetter")
            if (pendingJobsCount > 1000) issues.add("Critical pending job backlog: $pendingJobsCount")
        } else if (deadLetter > 0 || pendingJobsCount > 200 || failed > 20 || recoveredLeases > 10) {
            status = OperationalHealthStatus.DEGRADED
            if (deadLetter > 0) issues.add("Dead letter jobs present: $deadLetter")
            if (pendingJobsCount > 200) issues.add("High pending job backlog: $pendingJobsCount")
            if (recoveredLeases > 10) issues.add("High worker lease recovery count: $recoveredLeases")
        }

        return BackgroundJobHealth(
            status = status,
            pendingJobs = pendingJobsCount,
            processingJobs = processingJobsCount,
            completedJobs = succeeded,
            failedJobs = failed,
            retriedJobs = retried,
            deadLetterJobs = deadLetter,
            recoveredLeases = recoveredLeases,
            averageExecutionLatencyMs = avgLatency,
            issues = issues
        )
    }
}
