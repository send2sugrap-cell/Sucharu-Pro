package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.event.dispatcher.OutboxMetrics
import com.sucharu.sucharupro.data.job.observability.JobMetrics
import com.sucharu.sucharupro.data.workflow.observability.WorkflowMetrics
import org.junit.Assert.*
import org.junit.Test

/**
 * Regression test verifying existing metrics abstractions remain completely backward-compatible (INFRA-04 Step 09).
 */
class RegressionObservabilityTest {

    @Test
    fun test01_outboxMetrics_unbroken() {
        val outbox = OutboxMetrics()
        outbox.recordClaimed(5)
        outbox.recordPublished(10)
        assertEquals(5L, outbox.claimedCount)
        assertEquals(1L, outbox.publishedCount)
    }

    @Test
    fun test02_jobMetrics_unbroken() {
        val jobs = JobMetrics()
        jobs.recordEnqueued("INVOICE_SYNC", "p-001")
        jobs.recordSucceeded(50)
        assertEquals(1L, jobs.getEnqueuedCount())
        assertEquals(1L, jobs.getSucceededCount())
    }

    @Test
    fun test03_workflowMetrics_unbroken() {
        val wf = WorkflowMetrics()
        wf.recordWorkflowStarted("p-001")
        wf.recordWorkflowCompleted("p-001")
        assertEquals(1L, wf.getStartedCount())
        assertEquals(1L, wf.getCompletedCount())
    }
}
