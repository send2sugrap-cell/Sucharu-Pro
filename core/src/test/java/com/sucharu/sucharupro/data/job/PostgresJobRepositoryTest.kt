package com.sucharu.sucharupro.data.job

import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PostgresJobRepositoryTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var jobRepo: PostgresJobRepository

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        jobRepo = PostgresJobRepository(mockDb)
    }

    @Test
    fun testEnqueueAndFetchJob() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val job = JobDefinition(
                jobId = "job-101",
                projectId = "tenant_alpha",
                jobType = "order.render_proof",
                priority = JobPriority.HIGH
            )

            val enqueued = jobRepo.enqueueJob(job, tenant)
            assertTrue(enqueued)

            val fetched = jobRepo.getJobById("job-101", tenant)
            assertNotNull(fetched)
            assertEquals("job-101", fetched?.jobId)
            assertEquals("tenant_alpha", fetched?.projectId)
            assertEquals("order.render_proof", fetched?.jobType)
            assertEquals(JobPriority.HIGH, fetched?.priority)
            assertEquals(JobStatus.QUEUED, fetched?.status)
        }
    }

    @Test
    fun testIdempotentEnqueueSuppressesDuplicates() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val job1 = JobDefinition(
                jobId = "job-101",
                projectId = "tenant_alpha",
                jobType = "order.render_proof",
                idempotencyKey = "unique-key-123"
            )
            val job2 = JobDefinition(
                jobId = "job-102",
                projectId = "tenant_alpha",
                jobType = "order.render_proof",
                idempotencyKey = "unique-key-123"
            )

            val first = jobRepo.enqueueJob(job1, tenant)
            val second = jobRepo.enqueueJob(job2, tenant)

            assertTrue(first)
            assertTrue(!second) // duplicate suppressed
        }
    }

    @Test
    fun testClaimEligibleJobsPrioritizesCritical() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val normalJob = JobDefinition(
                jobId = "job-normal",
                projectId = "tenant_alpha",
                jobType = "order.normal",
                priority = JobPriority.NORMAL
            )
            val criticalJob = JobDefinition(
                jobId = "job-critical",
                projectId = "tenant_alpha",
                jobType = "order.critical",
                priority = JobPriority.CRITICAL
            )

            jobRepo.enqueueJob(normalJob, tenant)
            jobRepo.enqueueJob(criticalJob, tenant)

            val claimed = jobRepo.claimEligibleJobs("worker-1", limit = 1, tenantContext = tenant)
            assertEquals(1, claimed.size)
            assertEquals("job-critical", claimed[0].jobId)
            assertEquals(JobStatus.CLAIMED, claimed[0].status)
            assertEquals("worker-1", claimed[0].claimedByWorker)
        }
    }

    @Test
    fun testTenantIsolationPreventsCrossTenantAccess() {
        runBlocking {
            val tenantA = TenantContext("tenant_a")
            val tenantB = TenantContext("tenant_b")

            val jobA = JobDefinition(
                jobId = "job-a",
                projectId = "tenant_a",
                jobType = "order.render"
            )
            jobRepo.enqueueJob(jobA, tenantA)

            val fetchedFromB = jobRepo.getJobById("job-a", tenantB)
            assertNull(fetchedFromB)

            val claimedByB = jobRepo.claimEligibleJobs("worker-b", limit = 10, tenantContext = tenantB)
            assertTrue(claimedByB.isEmpty())
        }
    }
}
