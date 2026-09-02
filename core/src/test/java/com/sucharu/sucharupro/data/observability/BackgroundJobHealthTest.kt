package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.job.observability.JobMetrics
import com.sucharu.sucharupro.data.observability.health.BackgroundJobHealthEvaluator
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Background Job health and lease recovery test suite (INFRA-04 Step 09).
 */
class BackgroundJobHealthTest {

    private lateinit var jobMetrics: JobMetrics
    private lateinit var evaluator: BackgroundJobHealthEvaluator

    @Before
    fun setUp() {
        jobMetrics = JobMetrics()
        evaluator = BackgroundJobHealthEvaluator(jobMetrics)
    }

    @Test
    fun test01_emptyQueue_isHealthy() {
        val health = evaluator.evaluate(pendingJobsCount = 0, processingJobsCount = 0)
        assertEquals(OperationalHealthStatus.HEALTHY, health.status)
    }

    @Test
    fun test02_highPendingBacklog_isDegraded() {
        val health = evaluator.evaluate(pendingJobsCount = 250, processingJobsCount = 10)
        assertEquals(OperationalHealthStatus.DEGRADED, health.status)
        assertTrue(health.issues.any { it.contains("High pending job backlog") })
    }

    @Test
    fun test03_criticalDeadLetterBacklog_isCritical() {
        repeat(55) { jobMetrics.recordDeadLetter() }
        val health = evaluator.evaluate(pendingJobsCount = 10, processingJobsCount = 2)
        assertEquals(OperationalHealthStatus.CRITICAL, health.status)
        assertTrue(health.issues.any { it.contains("Critical dead letter job backlog") })
    }
}
