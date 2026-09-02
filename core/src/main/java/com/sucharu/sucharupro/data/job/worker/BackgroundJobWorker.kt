package com.sucharu.sucharupro.data.job.worker

import com.sucharu.sucharupro.data.job.lease.JobLeaseRecoveryService
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background worker pool running periodic claiming, execution, and lease recovery (INFRA-04 Step 04).
 */
class BackgroundJobWorker(
    val workerId: String = "worker-${UUID.randomUUID().toString().take(8)}",
    val concurrencyLimit: Int = 5,
    private val claimService: JobClaimService,
    private val executionEngine: JobExecutionEngine,
    private val leaseRecoveryService: JobLeaseRecoveryService,
    private val pollIntervalMs: Long = 1000L,
    private val leaseDurationMs: Long = 30000L
) {
    private val isRunning = AtomicBoolean(false)
    private var workerJob: Job? = null
    private val semaphore = Semaphore(concurrencyLimit)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Starts the worker loop in the background.
     */
    fun start(tenantContexts: List<TenantContext>) {
        if (isRunning.compareAndSet(false, true)) {
            workerJob = scope.launch {
                while (isActive && isRunning.get()) {
                    try {
                        for (tenant in tenantContexts) {
                            // 1. Recover stale leases
                            leaseRecoveryService.recoverStaleLeases(tenant)

                            // 2. Poll and claim jobs
                            pollAndExecuteBatch(tenant)
                        }
                    } catch (_: Throwable) {
                        // Resilient against loop exceptions
                    }
                    delay(pollIntervalMs)
                }
            }
        }
    }

    /**
     * Executes a single polling and execution pass for a tenant.
     * Useful for synchronous tests and deterministic polling.
     */
    suspend fun pollAndExecuteBatch(tenantContext: TenantContext): List<JobResult> {
        val claimed = claimService.claimJobs(
            workerId = workerId,
            batchSize = concurrencyLimit,
            leaseDurationMs = leaseDurationMs,
            tenantContext = tenantContext
        )

        if (claimed.isEmpty()) return emptyList()

        val results = mutableListOf<JobResult>()
        for (job in claimed) {
            semaphore.withPermit {
                val result = executionEngine.executeJob(job, workerId, tenantContext)
                results.add(result)
            }
        }
        return results
    }

    /**
     * Stops the background worker.
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            workerJob?.cancel()
            workerJob = null
        }
    }
}
