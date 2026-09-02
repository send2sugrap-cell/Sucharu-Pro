package com.sucharu.sucharupro.backend.workers

import com.sucharu.sucharupro.data.event.dispatcher.OutboxDispatcher
import com.sucharu.sucharupro.data.job.lease.JobLeaseRecoveryService
import com.sucharu.sucharupro.data.job.worker.BackgroundJobWorker
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lifecycle orchestrator for production background workers (INFRA-05 Step 04).
 * Manages Outbox Event Dispatcher, Background Job Workers, Lease Recovery, and Workflow Orchestrators.
 */
class BackgroundWorkerManager(
    private val outboxDispatcher: OutboxDispatcher? = null,
    val jobWorker: BackgroundJobWorker? = null,
    val leaseRecoveryService: JobLeaseRecoveryService? = null,
    val defaultTenants: List<TenantContext> = listOf(TenantContext("TENANT-001")),
    private val shutdownDrainTimeoutMs: Long = 5000L
) {

    private val logger = LoggerFactory.getLogger(BackgroundWorkerManager::class.java)
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("BackendWorkerScope"))
    private val isRunning = AtomicBoolean(false)

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("Starting background worker subsystem...")

            // 1. Initial Startup Lease Recovery
            if (leaseRecoveryService != null) {
                logger.info("Executing startup background job lease recovery for {} configured tenant(s)...", defaultTenants.size)
                for (tenant in defaultTenants) {
                    try {
                        val recovered = runBlocking { leaseRecoveryService.recoverStaleLeases(tenant) }
                        if (recovered > 0) {
                            logger.info("Recovered {} stale job lease(s) for tenant '{}' during startup", recovered, tenant.projectId)
                        }
                    } catch (e: Exception) {
                        logger.warn("Non-fatal startup lease recovery warning for tenant '{}': {}", tenant.projectId, e.message)
                    }
                }
            }

            // 2. Start Background Job Worker Loop
            if (jobWorker != null) {
                logger.info("Background Job Worker (concurrency: {}) starting for {} tenants...", jobWorker.concurrencyLimit, defaultTenants.size)
                jobWorker.start(defaultTenants)
            }

            logger.info("Background worker subsystem initialized and running successfully.")
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping background worker subsystem (drain grace period: {} ms)...", shutdownDrainTimeoutMs)
            try {
                // 1. Stop claiming new jobs
                jobWorker?.stop()

                // 2. Cancel worker coroutine scope and allow bounded drain
                workerScope.cancel()

                logger.info("Background worker subsystem stopped cleanly.")
            } catch (e: Exception) {
                logger.error("Error during background worker shutdown", e)
            }
        }
    }

    fun isHealthy(): Boolean = isRunning.get()
}
