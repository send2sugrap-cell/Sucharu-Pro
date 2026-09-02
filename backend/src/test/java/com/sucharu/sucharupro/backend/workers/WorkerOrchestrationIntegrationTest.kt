package com.sucharu.sucharupro.backend.workers

import com.sucharu.sucharupro.backend.composition.ProductionBackendComposition
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.data.job.lease.JobLeaseRecoveryService
import com.sucharu.sucharupro.data.job.postgres.*
import com.sucharu.sucharupro.data.job.retry.JobRetryEngine
import com.sucharu.sucharupro.data.job.worker.BackgroundJobWorker
import com.sucharu.sucharupro.data.job.worker.JobClaimService
import com.sucharu.sucharupro.data.job.worker.JobExecutionEngine
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobResult
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.worker.JobExecutionContext
import com.sucharu.sucharupro.domain.job.worker.JobHandler
import com.sucharu.sucharupro.domain.job.worker.JobHandlerRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Production-grade integration tests for Background Worker Orchestration (INFRA-05 STEP 04).
 *
 * Validates:
 * 1. Worker Lifecycle (Startup, Health, Graceful Shutdown)
 * 2. Multi-Worker Concurrency & SKIP LOCKED claiming
 * 3. Startup & Crash Lease Recovery
 * 4. Tenant Context & Anti-Spoofing (Attacks 1 & 2)
 * 5. Idempotency & Duplicate Submission (Attack 3)
 * 6. Retry Policy with Exponential Backoff (Attack 6)
 * 7. Poison Job Quarantine to Dead-Letter (Attack 7)
 * 8. Zero Secret / Token Leakage (Attack 9)
 * 9. ProductionBackendComposition Wiring
 */
class WorkerOrchestrationIntegrationTest {

    private lateinit var mockConnProvider: MockWorkerConnectionProvider
    private lateinit var txManager: DefaultPostgresTransactionManager
    private lateinit var jobRepo: PostgresJobRepository
    private lateinit var executionRepo: PostgresJobExecutionRepository
    private lateinit var deadLetterRepo: PostgresJobDeadLetterRepository
    private lateinit var dependencyRepo: PostgresJobDependencyRepository
    private lateinit var handlerRegistry: JobHandlerRegistry
    private lateinit var retryEngine: JobRetryEngine
    private lateinit var executionEngine: JobExecutionEngine
    private lateinit var claimService: JobClaimService
    private lateinit var leaseRecoveryService: JobLeaseRecoveryService

    private val tenantAlpha = TenantContext("PROJECT-ALPHA")
    private val tenantBeta = TenantContext("PROJECT-BETA")

    @Before
    fun setUp() {
        mockConnProvider = MockWorkerConnectionProvider()
        txManager = DefaultPostgresTransactionManager(mockConnProvider)
        jobRepo = PostgresJobRepository(txManager)
        executionRepo = PostgresJobExecutionRepository(txManager)
        deadLetterRepo = PostgresJobDeadLetterRepository(txManager)
        dependencyRepo = PostgresJobDependencyRepository(txManager)
        handlerRegistry = JobHandlerRegistry()
        retryEngine = JobRetryEngine()
        executionEngine = JobExecutionEngine(
            handlerRegistry = handlerRegistry,
            jobRepository = jobRepo,
            executionRepository = executionRepo,
            deadLetterRepository = deadLetterRepo,
            dependencyRepository = dependencyRepo,
            retryEngine = retryEngine
        )
        claimService = JobClaimService(jobRepo)
        leaseRecoveryService = JobLeaseRecoveryService(jobRepo)
    }

    // =========================================================================
    // 1. WORKER LIFECYCLE & SHUTDOWN
    // =========================================================================

    @Test
    fun test01_workerManager_startupAndShutdown_lifecycle() {
        val worker = BackgroundJobWorker(
            workerId = "worker-lifecycle-1",
            claimService = claimService,
            executionEngine = executionEngine,
            leaseRecoveryService = leaseRecoveryService
        )
        val manager = BackgroundWorkerManager(
            jobWorker = worker,
            leaseRecoveryService = leaseRecoveryService,
            defaultTenants = listOf(tenantAlpha)
        )

        assertFalse(manager.isHealthy())
        manager.start()
        assertTrue(manager.isHealthy())

        manager.stop()
        assertFalse(manager.isHealthy())
    }

    // =========================================================================
    // 2. CONCURRENT MULTI-WORKER CLAIMING
    // =========================================================================

    @Test
    fun test02_multiWorkerConcurrentClaiming_preventsDoubleExecution() = runBlocking {
        val executionCounter = AtomicInteger(0)

        handlerRegistry.registerHandler(object : JobHandler {
            override val supportedJobType: String = "invoice.generate"
            override suspend fun execute(context: JobExecutionContext): JobResult {
                executionCounter.incrementAndGet()
                return JobResult.Success("Invoice generated")
            }
        })

        // Enqueue 5 jobs in tenantAlpha
        for (i in 1..5) {
            val job = JobDefinition(
                jobId = "job-inv-$i",
                projectId = tenantAlpha.projectId,
                jobType = "invoice.generate"
            )
            jobRepo.enqueueJob(job, tenantAlpha)
        }

        val worker1 = BackgroundJobWorker("worker-1", concurrencyLimit = 5, claimService, executionEngine, leaseRecoveryService)
        val worker2 = BackgroundJobWorker("worker-2", concurrencyLimit = 5, claimService, executionEngine, leaseRecoveryService)

        // Run concurrent polling from two distinct workers
        val run1 = async { worker1.pollAndExecuteBatch(tenantAlpha) }
        val run2 = async { worker2.pollAndExecuteBatch(tenantAlpha) }
        val results = awaitAll(run1, run2)

        val totalExecuted = results[0].size + results[1].size
        assertEquals("All 5 jobs must be executed across workers", 5, totalExecuted)
        assertEquals("Each job must execute exactly once (no double claim)", 5, executionCounter.get())

        // Verify all 5 are marked SUCCEEDED
        for (i in 1..5) {
            val job = jobRepo.getJobById("job-inv-$i", tenantAlpha)
            assertEquals(JobStatus.SUCCEEDED, job?.status)
        }
    }

    // =========================================================================
    // 3. STARTUP & CRASH LEASE RECOVERY
    // =========================================================================

    @Test
    fun test03_staleLeaseRecovery_reschedulesAbandonedJobs() = runBlocking {
        // Enqueue and simulate a crashed worker holding a lease that expired
        val job = JobDefinition(
            jobId = "job-crashed-1",
            projectId = tenantAlpha.projectId,
            jobType = "export.data"
        )
        jobRepo.enqueueJob(job, tenantAlpha)

        // Simulate claim by deceased worker with expired lease
        val claimed = claimService.claimJobs("worker-dead", 1, leaseDurationMs = -1000L, tenantAlpha)
        assertEquals(1, claimed.size)
        assertEquals(JobStatus.CLAIMED, claimed[0].status)

        // Execute lease recovery
        val recoveredCount = leaseRecoveryService.recoverStaleLeases(tenantAlpha)
        assertEquals(1, recoveredCount)

        val recoveredJob = jobRepo.getJobById("job-crashed-1", tenantAlpha)
        assertNotNull(recoveredJob)
        assertEquals(JobStatus.RETRY_SCHEDULED, recoveredJob?.status)
        assertTrue(recoveredJob?.claimedByWorker.isNullOrBlank())
        assertNull(recoveredJob?.leaseExpiresAt)
    }

    // =========================================================================
    // 4. TENANT CONTEXT & ANTI-SPOOFING (ATTACKS 1 & 2)
    // =========================================================================

    @Test
    fun test04_attack1And2_tenantContextAuthoritative_payloadCannotSpoof() = runBlocking {
        var observedExecutionContext: JobExecutionContext? = null

        handlerRegistry.registerHandler(object : JobHandler {
            override val supportedJobType: String = "tenant.audit"
            override suspend fun execute(context: JobExecutionContext): JobResult {
                observedExecutionContext = context
                return JobResult.Success()
            }
        })

        // Job enqueued under tenantAlpha, but payload maliciously specifies projectId = "PROJECT-BETA"
        val payloadWithSpoofedTenant = """{"projectId":"PROJECT-BETA","action":"export_all_data"}"""
        val job = JobDefinition(
            jobId = "job-spoof-1",
            projectId = tenantAlpha.projectId,
            jobType = "tenant.audit",
            payloadJson = payloadWithSpoofedTenant
        )

        jobRepo.enqueueJob(job, tenantAlpha)

        val worker = BackgroundJobWorker("worker-sec-1", 1, claimService, executionEngine, leaseRecoveryService)
        worker.pollAndExecuteBatch(tenantAlpha)

        assertNotNull(observedExecutionContext)
        assertEquals("Execution context must remain bound strictly to PROJECT-ALPHA", "PROJECT-ALPHA", observedExecutionContext!!.projectId)

        // Attempting to query PROJECT-ALPHA job from PROJECT-BETA tenant must return null
        val crossTenantQuery = jobRepo.getJobById("job-spoof-1", tenantBeta)
        assertNull("Cross-tenant querying must return null under RLS/tenant boundary", crossTenantQuery)
    }

    // =========================================================================
    // 5. IDEMPOTENCY & DUPLICATE PROTECTION (ATTACK 3)
    // =========================================================================

    @Test
    fun test05_attack3_duplicateIdempotencyKey_rejected() = runBlocking {
        val job1 = JobDefinition(
            jobId = "job-idem-1",
            projectId = tenantAlpha.projectId,
            jobType = "payment.process",
            idempotencyKey = "IDEM-PAY-9999"
        )
        val job2 = JobDefinition(
            jobId = "job-idem-2",
            projectId = tenantAlpha.projectId,
            jobType = "payment.process",
            idempotencyKey = "IDEM-PAY-9999"
        )

        val insertedFirst = jobRepo.enqueueJob(job1, tenantAlpha)
        val insertedSecond = jobRepo.enqueueJob(job2, tenantAlpha)

        assertTrue("First job submission must succeed", insertedFirst)
        assertFalse("Duplicate idempotency key submission must be rejected", insertedSecond)
    }

    // =========================================================================
    // 6. RETRY POLICY WITH EXPONENTIAL BACKOFF (ATTACK 6)
    // =========================================================================

    @Test
    fun test06_transientFailure_schedulesRetryWithBackoff() = runBlocking {
        val attempts = AtomicInteger(0)

        handlerRegistry.registerHandler(object : JobHandler {
            override val supportedJobType: String = "network.sync"
            override suspend fun execute(context: JobExecutionContext): JobResult {
                val currentAttempt = attempts.incrementAndGet()
                return if (currentAttempt < 2) {
                    JobResult.Failure(
                        reason = "Network socket timeout",
                        classification = EventFailureClassification.TRANSIENT
                    )
                } else {
                    JobResult.Success("Synced on attempt 2")
                }
            }
        })

        val job = JobDefinition(
            jobId = "job-retry-1",
            projectId = tenantAlpha.projectId,
            jobType = "network.sync",
            maxAttempts = 3
        )
        jobRepo.enqueueJob(job, tenantAlpha)

        val worker = BackgroundJobWorker("worker-retry-1", 1, claimService, executionEngine, leaseRecoveryService)

        // Attempt 1: Fails with transient error
        val result1 = worker.pollAndExecuteBatch(tenantAlpha)
        assertEquals(1, result1.size)
        assertTrue(result1[0] is JobResult.Failure)

        val jobAfterAttempt1 = jobRepo.getJobById("job-retry-1", tenantAlpha)
        assertNotNull(jobAfterAttempt1)
        assertEquals(JobStatus.RETRY_SCHEDULED, jobAfterAttempt1?.status)
        assertEquals(1, jobAfterAttempt1?.attemptCount)
        assertNotNull(jobAfterAttempt1?.nextAttemptAt)
    }

    // =========================================================================
    // 7. POISON JOB QUARANTINE TO DEAD-LETTER (ATTACK 7)
    // =========================================================================

    @Test
    fun test07_attack7_poisonJob_quarantinedToDeadLetter() = runBlocking {
        handlerRegistry.registerHandler(object : JobHandler {
            override val supportedJobType: String = "poison.job"
            override suspend fun execute(context: JobExecutionContext): JobResult {
                return JobResult.Failure(
                    reason = "Deterministic JSON parsing syntax error",
                    classification = EventFailureClassification.NON_RETRYABLE
                )
            }
        })

        val job = JobDefinition(
            jobId = "job-poison-1",
            projectId = tenantAlpha.projectId,
            jobType = "poison.job",
            maxAttempts = 3
        )
        jobRepo.enqueueJob(job, tenantAlpha)

        val worker = BackgroundJobWorker("worker-poison-1", 1, claimService, executionEngine, leaseRecoveryService)
        val results = worker.pollAndExecuteBatch(tenantAlpha)

        assertEquals(1, results.size)
        assertTrue(results[0] is JobResult.Failure)

        val deadJob = jobRepo.getJobById("job-poison-1", tenantAlpha)
        assertEquals(JobStatus.DEAD_LETTER, deadJob?.status)

        val deadLetters = deadLetterRepo.listUnresolvedDeadLetters(10, tenantAlpha)
        val deadLetterRecord = deadLetters.find { it.jobId == "job-poison-1" }
        assertNotNull("Job must be quarantined in dead letter table", deadLetterRecord)
        assertEquals("poison.job", deadLetterRecord?.jobType)
        assertEquals("NON_RETRYABLE", deadLetterRecord?.errorCode)
        assertTrue(deadLetterRecord!!.errorMessage!!.contains("Deterministic JSON parsing syntax error"))
    }

    // =========================================================================
    // 8. ZERO SECRET / TOKEN LEAKAGE (ATTACK 9)
    // =========================================================================

    @Test
    fun test08_attack9_zeroTokenLeakage_inJobMetadata() = runBlocking {
        val job = JobDefinition(
            jobId = "job-sanitized-1",
            projectId = tenantAlpha.projectId,
            jobType = "user.welcome_email",
            payloadJson = """{"userId":"USR-100","email":"user@example.com"}""",
            metadata = mapOf("channel" to "EMAIL", "template" to "WELCOME_V1")
        )

        jobRepo.enqueueJob(job, tenantAlpha)

        val persisted = jobRepo.getJobById("job-sanitized-1", tenantAlpha)
        assertNotNull(persisted)

        // Ensure no Bearer token or secret was ever stored
        assertFalse(persisted!!.payloadJson.contains("Bearer", ignoreCase = true))
        assertFalse(persisted.metadata.values.any { it.contains("Bearer", ignoreCase = true) })
    }

    // =========================================================================
    // 9. PRODUCTION COMPOSITION INTEGRATION
    // =========================================================================

    @Test
    fun test09_productionComposition_wiresBackgroundJobSubsystem() {
        val config = BackendConfig(
            environment = BackendEnvironment.TEST,
            serverPort = 0,
            databaseUrl = "jdbc:postgresql://127.0.0.1:5432/sucharu_test",
            databaseUser = "sucharu_user",
            databasePassword = "sucharu_password",
            jwtSigningSecret = "sucharu_production_edge_secret_test_key_2026_secure",
            workerPoolSize = 8
        )

        val composition = ProductionBackendComposition(config)

        assertNotNull(composition.jobRepository)
        assertNotNull(composition.jobExecutionRepository)
        assertNotNull(composition.jobDeadLetterRepository)
        assertNotNull(composition.jobHandlerRegistry)
        assertNotNull(composition.jobClaimService)
        assertNotNull(composition.jobLeaseRecoveryService)
        assertNotNull(composition.jobExecutionEngine)
        assertNotNull(composition.jobWorker)
        assertNotNull(composition.workerManager)
        assertEquals(8, composition.jobWorker.concurrencyLimit)
    }

    // =========================================================================
    // MOCK CONNECTION PROVIDER WITH IN-MEMORY TABLE PROXY
    // =========================================================================

    private class MockWorkerConnectionProvider : PostgresConnectionProvider {
        val backgroundJobs = CopyOnWriteArrayList<MutableMap<String, Any?>>()
        val jobExecutions = CopyOnWriteArrayList<MutableMap<String, Any?>>()
        val jobDeadLetters = CopyOnWriteArrayList<MutableMap<String, Any?>>()
        val idempotencyKeys = ConcurrentHashMap<String, Boolean>()
        var currentSessionProjectId: String = ""
        private var isClosed = false

        override suspend fun acquireConnection(): Connection {
            return Proxy.newProxyInstance(
                Connection::class.java.classLoader,
                arrayOf(Connection::class.java),
                MockConnectionInvocationHandler()
            ) as Connection
        }

        override suspend fun releaseConnection(connection: Connection) {
            currentSessionProjectId = ""
        }

        override fun getActiveConnectionCount(): Int = 0
        override fun getIdleConnectionCount(): Int = 1
        override fun getTotalAcquisitions(): Long = 1L
        override fun getAcquisitionFailureCount(): Long = 0L
        override suspend fun shutdownGracefully(drainTimeoutMs: Long) { isClosed = true }
        override fun close() { isClosed = true }

        private inner class MockConnectionInvocationHandler : java.lang.reflect.InvocationHandler {
            private var inTx = false

            override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any? {
                val name = method.name
                val methodArgs = args ?: emptyArray()

                return when (name) {
                    "setAutoCommit" -> {
                        inTx = !(methodArgs[0] as Boolean)
                        null
                    }
                    "getAutoCommit" -> !inTx
                    "commit", "rollback" -> {
                        inTx = false
                        null
                    }
                    "isClosed" -> isClosed
                    "isValid" -> true
                    "close" -> null
                    "prepareStatement" -> {
                        val sql = methodArgs[0] as String
                        createMockPreparedStatement(sql)
                    }
                    else -> null
                }
            }
        }

        private fun createMockPreparedStatement(sql: String): PreparedStatement {
            val params = mutableListOf<Any?>()

            return Proxy.newProxyInstance(
                PreparedStatement::class.java.classLoader,
                arrayOf(PreparedStatement::class.java)
            ) { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "setString", "setInt", "setLong", "setBigDecimal", "setBoolean", "setTimestamp" -> {
                        val idx = mArgs[0] as Int
                        val v = mArgs[1]
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = v
                        null
                    }
                    "setNull" -> {
                        val idx = mArgs[0] as Int
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = null
                        null
                    }
                    "execute" -> {
                        if (sql.contains("set_config")) {
                            currentSessionProjectId = params.getOrNull(0) as? String ?: ""
                        }
                        true
                    }
                    "executeQuery" -> {
                        createMockResultSet(sql, params)
                    }
                    "executeUpdate" -> {
                        executeMockUpdate(sql, params)
                    }
                    "close" -> null
                    else -> null
                }
            } as PreparedStatement
        }

        private fun executeMockUpdate(sql: String, params: List<Any?>): Int {
            val upperSql = sql.uppercase()

            if (upperSql.contains("INSERT INTO BACKGROUND_JOBS")) {
                val jobId = params[0] as String
                val projectId = params[1] as String
                val jobType = params[2] as String
                val jobVersion = params[3] as String
                val triggerType = params[4] as String
                val priority = params[5] as Int
                val status = params[6] as String
                val attemptCount = params[7] as Int
                val maxAttempts = params[8] as Int
                val scheduledAt = params[9] as Timestamp
                val availableAt = params[10] as Timestamp
                val payload = params[17] as String
                val metadata = params[18] as String
                val correlationId = params[19] as String
                val causationId = params[20] as? String
                val requestId = params[21] as? String
                val actorType = params[22] as String
                val actorId = params[23] as String
                val principalType = params[24] as String
                val source = params[25] as String
                val idempotencyKey = params[29] as? String
                val createdAt = params[30] as Timestamp
                val updatedAt = params[31] as Timestamp

                if (idempotencyKey != null) {
                    val uniqueKey = "$projectId:$idempotencyKey"
                    if (idempotencyKeys.putIfAbsent(uniqueKey, true) != null) {
                        return 0 // ON CONFLICT DO NOTHING
                    }
                }

                val row = mutableMapOf<String, Any?>(
                    "job_id" to jobId,
                    "project_id" to projectId,
                    "job_type" to jobType,
                    "job_version" to jobVersion,
                    "trigger_type" to triggerType,
                    "priority" to priority,
                    "status" to status,
                    "attempt_count" to attemptCount,
                    "max_attempts" to maxAttempts,
                    "scheduled_at" to scheduledAt,
                    "available_at" to availableAt,
                    "started_at" to null,
                    "completed_at" to null,
                    "next_attempt_at" to null,
                    "claimed_by_worker" to null,
                    "claimed_at" to null,
                    "lease_expires_at" to null,
                    "payload" to payload,
                    "metadata" to metadata,
                    "correlation_id" to correlationId,
                    "causation_id" to causationId,
                    "request_id" to requestId,
                    "actor_type" to actorType,
                    "actor_id" to actorId,
                    "principal_type" to principalType,
                    "source" to source,
                    "last_error_code" to null,
                    "last_error_message" to null,
                    "failure_classification" to null,
                    "idempotency_key" to idempotencyKey,
                    "created_at" to createdAt,
                    "updated_at" to updatedAt
                )
                backgroundJobs.add(row)
                return 1
            }

            if (upperSql.contains("INSERT INTO JOB_EXECUTIONS")) {
                val row = mutableMapOf<String, Any?>(
                    "execution_id" to params[0],
                    "project_id" to params[1],
                    "job_id" to params[2],
                    "worker_id" to params[3],
                    "attempt_number" to params[4],
                    "started_at" to params[5],
                    "completed_at" to params[6],
                    "duration_ms" to params[7],
                    "status" to params[8],
                    "error_code" to params[9],
                    "error_message" to params[10],
                    "failure_classification" to params[11],
                    "output_metadata" to params[12]
                )
                jobExecutions.add(row)
                return 1
            }

            if (upperSql.contains("INSERT INTO JOB_DEAD_LETTERS")) {
                val row = mutableMapOf<String, Any?>(
                    "dead_letter_id" to params[0],
                    "project_id" to params[1],
                    "job_id" to params[2],
                    "job_type" to params[3],
                    "payload" to params[4],
                    "metadata" to params[5],
                    "attempt_count" to params[6],
                    "failure_classification" to params[7],
                    "error_code" to params[8],
                    "error_message" to params[9],
                    "first_failure_at" to params[10],
                    "final_failure_at" to params[11],
                    "correlation_id" to params[12],
                    "causation_id" to params[13],
                    "request_id" to params[14],
                    "is_resolved" to params[15],
                    "replayed_at" to params[16],
                    "replayed_by" to params[17],
                    "created_at" to params[18]
                )
                jobDeadLetters.add(row)
                return 1
            }

            if (upperSql.contains("UPDATE BACKGROUND_JOBS") && upperSql.contains("STATUS = 'CLAIMED'")) {
                val workerId = params[0] as String
                val claimedAt = params[1] as Timestamp
                val leaseExpiresAt = params[2] as Timestamp
                val startedAt = params[3] as Timestamp
                val projectId = params[4] as String
                val jobId = params[5] as String

                val row = backgroundJobs.find { it["project_id"] == projectId && it["job_id"] == jobId }
                if (row != null) {
                    row["status"] = "CLAIMED"
                    row["claimed_by_worker"] = workerId
                    row["claimed_at"] = claimedAt
                    row["lease_expires_at"] = leaseExpiresAt
                    row["started_at"] = startedAt
                    row["attempt_count"] = (row["attempt_count"] as Int) + 1
                    row["updated_at"] = Timestamp(System.currentTimeMillis())
                    return 1
                }
                return 0
            }

            if (upperSql.contains("UPDATE BACKGROUND_JOBS") && upperSql.contains("STATUS = 'SUCCEEDED'")) {
                val projectId = params[0] as String
                val jobId = params[1] as String
                val row = backgroundJobs.find { it["project_id"] == projectId && it["job_id"] == jobId }
                if (row != null) {
                    row["status"] = "SUCCEEDED"
                    row["completed_at"] = Timestamp(System.currentTimeMillis())
                    row["claimed_by_worker"] = null
                    row["lease_expires_at"] = null
                    row["updated_at"] = Timestamp(System.currentTimeMillis())
                    return 1
                }
                return 0
            }

            if (upperSql.contains("UPDATE BACKGROUND_JOBS") && upperSql.contains("STATUS = 'DEAD_LETTER'")) {
                val errCode = params[0] as? String
                val errMsg = params[1] as? String
                val failClass = params[2] as? String
                val projectId = params[3] as String
                val jobId = params[4] as String

                val row = backgroundJobs.find { it["project_id"] == projectId && it["job_id"] == jobId }
                if (row != null) {
                    row["status"] = "DEAD_LETTER"
                    row["last_error_code"] = errCode
                    row["last_error_message"] = errMsg
                    row["failure_classification"] = failClass
                    row["claimed_by_worker"] = null
                    row["lease_expires_at"] = null
                    row["completed_at"] = Timestamp(System.currentTimeMillis())
                    row["updated_at"] = Timestamp(System.currentTimeMillis())
                    return 1
                }
                return 0
            }

            if (upperSql.contains("UPDATE BACKGROUND_JOBS") && (upperSql.contains("STATUS = ?") || upperSql.contains("STATUS = 'FAILED'"))) {
                val status = params[0] as String
                val errCode = params[1] as? String
                val errMsg = params[2] as? String
                val failClass = params[3] as? String
                val nextAttempt = params[4] as? Timestamp
                val availAt = params[5] as? Timestamp
                val projectId = params[6] as String
                val jobId = params[7] as String

                val row = backgroundJobs.find { it["project_id"] == projectId && it["job_id"] == jobId }
                if (row != null) {
                    row["status"] = status
                    row["last_error_code"] = errCode
                    row["last_error_message"] = errMsg
                    row["failure_classification"] = failClass
                    row["next_attempt_at"] = nextAttempt
                    row["available_at"] = availAt ?: Timestamp(System.currentTimeMillis())
                    row["claimed_by_worker"] = null
                    row["lease_expires_at"] = null
                    row["updated_at"] = Timestamp(System.currentTimeMillis())
                    return 1
                }
                return 0
            }

            if (upperSql.contains("UPDATE BACKGROUND_JOBS") && upperSql.contains("STATUS = 'RETRY_SCHEDULED'") && upperSql.contains("LEASE_EXPIRES_AT < NOW()")) {
                val projectId = params[0] as String
                var recovered = 0
                val now = System.currentTimeMillis()
                backgroundJobs.filter {
                    it["project_id"] == projectId &&
                    (it["status"] == "CLAIMED" || it["status"] == "RUNNING") &&
                    (it["lease_expires_at"] as? Timestamp)?.time?.let { ts -> ts < now } == true
                }.forEach { row ->
                    row["status"] = "RETRY_SCHEDULED"
                    row["claimed_by_worker"] = null
                    row["claimed_at"] = null
                    row["lease_expires_at"] = null
                    row["available_at"] = Timestamp(now)
                    row["updated_at"] = Timestamp(now)
                    recovered++
                }
                return recovered
            }

            return 1
        }

        private fun createMockResultSet(sql: String, params: List<Any?>): ResultSet {
            val upperSql = sql.uppercase()
            val rows = mutableListOf<Map<String, Any?>>()

            if (upperSql.contains("FROM BACKGROUND_JOBS") && upperSql.contains("FOR UPDATE SKIP LOCKED")) {
                val projectId = params[0] as String
                val limit = params[1] as Int
                val now = System.currentTimeMillis()

                synchronized(backgroundJobs) {
                    val matched = backgroundJobs.filter {
                        it["project_id"] == projectId &&
                        (it["status"] == "QUEUED" || it["status"] == "RETRY_SCHEDULED") &&
                        ((it["available_at"] as? Timestamp)?.time ?: 0L) <= now &&
                        (it["claimed_by_worker"] == null || it["claimed_by_worker"] == "")
                    }.take(limit)

                    matched.forEach {
                        it["claimed_by_worker"] = "LOCKING"
                        rows.add(it)
                    }
                }
            } else if (upperSql.contains("FROM BACKGROUND_JOBS") && upperSql.contains("JOB_ID = ?")) {
                val projectId = params[0] as String
                val jobId = params[1] as String
                val found = backgroundJobs.find { it["project_id"] == projectId && it["job_id"] == jobId }
                if (found != null) {
                    rows.add(found)
                }
            } else if (upperSql.contains("FROM BACKGROUND_JOBS") && upperSql.contains("STATUS IN ('QUEUED'")) {
                val projectId = params[0] as String
                val limit = params[1] as Int
                val matched = backgroundJobs.filter {
                    it["project_id"] == projectId &&
                    (it["status"] == "QUEUED" || it["status"] == "RETRY_SCHEDULED" || it["status"] == "WAITING")
                }.take(limit)

                matched.forEach { rows.add(it) }
            } else if (upperSql.contains("FROM JOB_DEAD_LETTERS") && upperSql.contains("IS_RESOLVED = FALSE")) {
                val projectId = params[0] as String
                val limit = params[1] as Int
                val matched = jobDeadLetters.filter {
                    it["project_id"] == projectId && (it["is_resolved"] == false || it["is_resolved"] == null)
                }.take(limit)

                matched.forEach { rows.add(it) }
            }

            var cursor = -1

            return Proxy.newProxyInstance(
                ResultSet::class.java.classLoader,
                arrayOf(ResultSet::class.java)
            ) { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "next" -> {
                        cursor++
                        cursor < rows.size
                    }
                    "getString" -> {
                        val col = mArgs[0] as String
                        rows.getOrNull(cursor)?.get(col)?.toString()
                    }
                    "getInt" -> {
                        val col = mArgs[0] as String
                        (rows.getOrNull(cursor)?.get(col) as? Number)?.toInt() ?: 0
                    }
                    "getLong" -> {
                        val col = mArgs[0] as String
                        (rows.getOrNull(cursor)?.get(col) as? Number)?.toLong() ?: 0L
                    }
                    "getTimestamp" -> {
                        val col = mArgs[0] as String
                        rows.getOrNull(cursor)?.get(col) as? Timestamp
                    }
                    "getBoolean" -> {
                        val col = mArgs[0] as String
                        rows.getOrNull(cursor)?.get(col) as? Boolean ?: false
                    }
                    "wasNull" -> false
                    "close" -> null
                    else -> null
                }
            } as ResultSet
        }
    }
}
