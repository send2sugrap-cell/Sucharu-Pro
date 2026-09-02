package com.sucharu.sucharupro.data.job

import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.job.lease.JobLeaseRecoveryService
import com.sucharu.sucharupro.data.job.postgres.PostgresJobDeadLetterRepository
import com.sucharu.sucharupro.data.job.postgres.PostgresJobDependencyRepository
import com.sucharu.sucharupro.data.job.postgres.PostgresJobExecutionRepository
import com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository
import com.sucharu.sucharupro.data.job.worker.BackgroundJobWorker
import com.sucharu.sucharupro.data.job.worker.JobClaimService
import com.sucharu.sucharupro.data.job.worker.JobExecutionEngine
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobResult
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.worker.JobExecutionContext
import com.sucharu.sucharupro.domain.job.worker.JobHandler
import com.sucharu.sucharupro.domain.job.worker.JobHandlerRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class JobExecutionAndWorkerConcurrencyTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var jobRepo: PostgresJobRepository
    private lateinit var executionRepo: PostgresJobExecutionRepository
    private lateinit var deadLetterRepo: PostgresJobDeadLetterRepository
    private lateinit var dependencyRepo: PostgresJobDependencyRepository
    private lateinit var handlerRegistry: JobHandlerRegistry
    private lateinit var executionEngine: JobExecutionEngine
    private lateinit var claimService: JobClaimService
    private lateinit var leaseRecoveryService: JobLeaseRecoveryService

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        jobRepo = PostgresJobRepository(mockDb)
        executionRepo = PostgresJobExecutionRepository(mockDb)
        deadLetterRepo = PostgresJobDeadLetterRepository(mockDb)
        dependencyRepo = PostgresJobDependencyRepository(mockDb)
        handlerRegistry = JobHandlerRegistry()
        executionEngine = JobExecutionEngine(
            handlerRegistry = handlerRegistry,
            jobRepository = jobRepo,
            executionRepository = executionRepo,
            deadLetterRepository = deadLetterRepo,
            dependencyRepository = dependencyRepo
        )
        claimService = JobClaimService(jobRepo)
        leaseRecoveryService = JobLeaseRecoveryService(jobRepo)
    }

    @Test
    fun testJobExecutionSuccessRecordsHistory() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val executedCount = AtomicInteger(0)

            handlerRegistry.registerHandler(object : JobHandler {
                override val supportedJobType: String = "report.generate"
                override suspend fun execute(context: JobExecutionContext): JobResult {
                    executedCount.incrementAndGet()
                    return JobResult.Success("Report generated successfully")
                }
            })

            val job = JobDefinition(
                jobId = "job-rep-1",
                projectId = "tenant_alpha",
                jobType = "report.generate"
            )
            jobRepo.enqueueJob(job, tenant)

            val worker = BackgroundJobWorker(
                workerId = "worker-test-1",
                claimService = claimService,
                executionEngine = executionEngine,
                leaseRecoveryService = leaseRecoveryService
            )

            val results = worker.pollAndExecuteBatch(tenant)
            assertEquals(1, results.size)
            assertTrue(results[0] is JobResult.Success)
            assertEquals(1, executedCount.get())

            val fetched = jobRepo.getJobById("job-rep-1", tenant)
            assertEquals(JobStatus.SUCCEEDED, fetched?.status)

            val history = executionRepo.getExecutionsForJob("job-rep-1", tenant)
            assertEquals(1, history.size)
            assertEquals(JobStatus.SUCCEEDED, history[0].status)
            assertEquals("worker-test-1", history[0].workerId)
        }
    }

    @Test
    fun testMultipleWorkersDoNotExecuteSameJobSimultaneously() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val executionCount = AtomicInteger(0)

            handlerRegistry.registerHandler(object : JobHandler {
                override val supportedJobType: String = "order.batch_process"
                override suspend fun execute(context: JobExecutionContext): JobResult {
                    executionCount.incrementAndGet()
                    return JobResult.Success("Batch processed")
                }
            })

            // Enqueue 5 jobs
            for (i in 1..5) {
                jobRepo.enqueueJob(
                    JobDefinition(
                        jobId = "job-$i",
                        projectId = "tenant_alpha",
                        jobType = "order.batch_process"
                    ),
                    tenant
                )
            }

            val worker1 = BackgroundJobWorker("worker-1", 10, claimService, executionEngine, leaseRecoveryService)
            val worker2 = BackgroundJobWorker("worker-2", 10, claimService, executionEngine, leaseRecoveryService)

            val d1 = async { worker1.pollAndExecuteBatch(tenant) }
            val d2 = async { worker2.pollAndExecuteBatch(tenant) }

            val res1 = d1.await()
            val res2 = d2.await()

            // Total executed across both workers must equal exactly 5
            assertEquals(5, res1.size + res2.size)
            assertEquals(5, executionCount.get())
        }
    }

    @Test
    fun testExpiredLeaseRecoveryReclaimsStaleJobs() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val job = JobDefinition(
                jobId = "job-stale-1",
                projectId = "tenant_alpha",
                jobType = "order.stale"
            )
            jobRepo.enqueueJob(job, tenant)

            // Claim with 1ms lease so it expires immediately
            jobRepo.claimEligibleJobs("dead-worker", limit = 1, leaseDurationMs = 1L, tenantContext = tenant)

            val claimedJob = jobRepo.getJobById("job-stale-1", tenant)
            assertEquals(JobStatus.CLAIMED, claimedJob?.status)

            // Wait 5ms for lease to expire
            kotlinx.coroutines.delay(5)

            val recovered = leaseRecoveryService.recoverStaleLeases(tenant)
            assertEquals(1, recovered)

            val recoveredJob = jobRepo.getJobById("job-stale-1", tenant)
            assertEquals(JobStatus.RETRY_SCHEDULED, recoveredJob?.status)
        }
    }
}
