package com.sucharu.sucharupro.data.job

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobResult
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobModelAndStateTest {

    @Test
    fun testJobDefinitionValidatesRequiredFields() {
        val job = JobDefinition(
            projectId = "tenant_test",
            jobType = "order.process_artwork",
            priority = JobPriority.HIGH,
            triggerType = JobTriggerType.EVENT
        )

        assertEquals("tenant_test", job.projectId)
        assertEquals("order.process_artwork", job.jobType)
        assertEquals(JobPriority.HIGH, job.priority)
        assertEquals(JobStatus.QUEUED, job.status)
        assertEquals(0, job.attemptCount)
        assertEquals(3, job.maxAttempts)
        assertFalse(job.status.isTerminal)
        assertTrue(job.status.isPendingOrRunning)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testJobDefinitionRejectsBlankProjectId() {
        JobDefinition(
            projectId = "",
            jobType = "order.process_artwork"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testJobDefinitionRejectsBlankJobType() {
        JobDefinition(
            projectId = "tenant_test",
            jobType = ""
        )
    }

    @Test
    fun testJobStatusTerminalStates() {
        assertTrue(JobStatus.SUCCEEDED.isTerminal)
        assertTrue(JobStatus.DEAD_LETTER.isTerminal)
        assertTrue(JobStatus.CANCELLED.isTerminal)
        assertTrue(JobStatus.EXPIRED.isTerminal)

        assertFalse(JobStatus.QUEUED.isTerminal)
        assertFalse(JobStatus.CLAIMED.isTerminal)
        assertFalse(JobStatus.RUNNING.isTerminal)
        assertFalse(JobStatus.RETRY_SCHEDULED.isTerminal)
    }

    @Test
    fun testJobResultTypes() {
        val success = JobResult.Success(message = "Done", outputMetadata = mapOf("key" to "value"))
        assertEquals("Done", success.message)
        assertEquals("value", success.outputMetadata["key"])

        val failure = JobResult.Failure(reason = "Timeout")
        assertTrue(failure.isRetryable)
        assertEquals("Timeout", failure.reason)

        val cancelled = JobResult.Cancelled(reason = "User cancelled")
        assertEquals("User cancelled", cancelled.reason)
    }
}
