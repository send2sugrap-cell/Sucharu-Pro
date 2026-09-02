package com.sucharu.sucharupro.data.job

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.event.model.RetryConfig
import com.sucharu.sucharupro.data.job.operations.DefaultJobOperationsService
import com.sucharu.sucharupro.data.job.postgres.PostgresJobDeadLetterRepository
import com.sucharu.sucharupro.data.job.postgres.PostgresJobDependencyRepository
import com.sucharu.sucharupro.data.job.postgres.PostgresJobExecutionRepository
import com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository
import com.sucharu.sucharupro.data.job.postgres.PostgresJobScheduleRepository
import com.sucharu.sucharupro.data.job.retry.JobRetryEngine
import com.sucharu.sucharupro.data.job.worker.JobExecutionEngine
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobResult
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.worker.JobExecutionContext
import com.sucharu.sucharupro.domain.job.worker.JobHandler
import com.sucharu.sucharupro.domain.job.worker.JobHandlerRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JobRetryAndDeadLetterTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var jobRepo: PostgresJobRepository
    private lateinit var executionRepo: PostgresJobExecutionRepository
    private lateinit var deadLetterRepo: PostgresJobDeadLetterRepository
    private lateinit var scheduleRepo: PostgresJobScheduleRepository
    private lateinit var dependencyRepo: PostgresJobDependencyRepository
    private lateinit var handlerRegistry: JobHandlerRegistry
    private lateinit var retryEngine: JobRetryEngine
    private lateinit var executionEngine: JobExecutionEngine
    private lateinit var operationsService: DefaultJobOperationsService

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        jobRepo = PostgresJobRepository(mockDb)
        executionRepo = PostgresJobExecutionRepository(mockDb)
        deadLetterRepo = PostgresJobDeadLetterRepository(mockDb)
        scheduleRepo = PostgresJobScheduleRepository(mockDb)
        dependencyRepo = PostgresJobDependencyRepository(mockDb)
        handlerRegistry = JobHandlerRegistry()
        retryEngine = JobRetryEngine(
            RetryConfig(
                maxAttempts = 2,
                initialBackoffMs = 100L,
                maxBackoffMs = 1000L,
                multiplier = 2.0,
                jitterFactor = 0.0
            )
        )
        executionEngine = JobExecutionEngine(
            handlerRegistry = handlerRegistry,
            jobRepository = jobRepo,
            executionRepository = executionRepo,
            deadLetterRepository = deadLetterRepo,
            dependencyRepository = dependencyRepo,
            retryEngine = retryEngine
        )
        operationsService = DefaultJobOperationsService(
            jobRepository = jobRepo,
            scheduleRepository = scheduleRepo,
            deadLetterRepository = deadLetterRepo
        )
    }

    @Test
    fun testTransientFailureSchedulesRetry() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")

            handlerRegistry.registerHandler(object : JobHandler {
                override val supportedJobType: String = "payment.verify"
                override suspend fun execute(context: JobExecutionContext): JobResult {
                    return JobResult.Failure("Network timeout", EventFailureClassification.TRANSIENT)
                }
            })

            val job = JobDefinition(
                jobId = "job-retry-1",
                projectId = "tenant_alpha",
                jobType = "payment.verify",
                maxAttempts = 3
            )
            jobRepo.enqueueJob(job, tenant)
            val claimed = jobRepo.claimEligibleJobs("worker-1", 1, 30000L, tenant)

            val result = executionEngine.executeJob(claimed[0], "worker-1", tenant)
            assertTrue(result is JobResult.Failure)

            val updatedJob = jobRepo.getJobById("job-retry-1", tenant)
            assertEquals(JobStatus.RETRY_SCHEDULED, updatedJob?.status)
            assertNotNull(updatedJob?.nextAttemptAt)
        }
    }

    @Test
    fun testExhaustedRetriesQuarantinesToDeadLetter() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")

            handlerRegistry.registerHandler(object : JobHandler {
                override val supportedJobType: String = "order.failing"
                override suspend fun execute(context: JobExecutionContext): JobResult {
                    return JobResult.Failure("Critical corruption", EventFailureClassification.NON_RETRYABLE)
                }
            })

            val job = JobDefinition(
                jobId = "job-fail-1",
                projectId = "tenant_alpha",
                jobType = "order.failing",
                maxAttempts = 1
            )
            jobRepo.enqueueJob(job, tenant)
            val claimed = jobRepo.claimEligibleJobs("worker-1", 1, 30000L, tenant)

            executionEngine.executeJob(claimed[0], "worker-1", tenant)

            val updatedJob = jobRepo.getJobById("job-fail-1", tenant)
            assertEquals(JobStatus.DEAD_LETTER, updatedJob?.status)

            val deadLetters = deadLetterRepo.listUnresolvedDeadLetters(10, tenant)
            assertEquals(1, deadLetters.size)
            assertEquals("job-fail-1", deadLetters[0].jobId)
            assertEquals(EventFailureClassification.NON_RETRYABLE, deadLetters[0].failureClassification)
        }
    }

    @Test
    fun testAdminReplaysDeadLetterJob() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val adminPrincipal = AuthenticatedPrincipal(
                userId = "admin-1",
                username = "admin_user",
                role = UserRole.ADMIN,
                projectId = "tenant_alpha",
                principalType = PrincipalType.HUMAN
            )

            handlerRegistry.registerHandler(object : JobHandler {
                override val supportedJobType: String = "sync.inventory"
                override suspend fun execute(context: JobExecutionContext): JobResult {
                    return JobResult.Failure("Third-party API down", EventFailureClassification.NON_RETRYABLE)
                }
            })

            val job = JobDefinition(
                jobId = "job-sync-1",
                projectId = "tenant_alpha",
                jobType = "sync.inventory",
                maxAttempts = 1
            )
            jobRepo.enqueueJob(job, tenant)
            val claimed = jobRepo.claimEligibleJobs("worker-1", 1, 30000L, tenant)
            executionEngine.executeJob(claimed[0], "worker-1", tenant)

            val deadLetter = deadLetterRepo.listUnresolvedDeadLetters(10, tenant)[0]

            val newJobId = operationsService.retryDeadLetterJob(deadLetter.deadLetterId, adminPrincipal)
            assertNotNull(newJobId)

            val replayedJob = jobRepo.getJobById(newJobId, tenant)
            assertNotNull(replayedJob)
            assertEquals(JobStatus.QUEUED, replayedJob?.status)
            assertEquals("sync.inventory", replayedJob?.jobType)

            val updatedDeadLetter = deadLetterRepo.getDeadLetterById(deadLetter.deadLetterId, tenant)
            assertTrue(updatedDeadLetter?.isResolved == true)
        }
    }
}
