package com.sucharu.sucharupro.data.persistence.postgres

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Standard structured persistence log events (INFRA-02 Step 03).
 */
enum class PersistenceLogEvent {
    DATABASE_CONNECTION_INITIALIZED,
    DATABASE_CONNECTION_ACQUIRED,
    DATABASE_CONNECTION_RELEASED,
    DATABASE_CONNECTION_FAILURE,
    DATABASE_HEALTH_CHECK,
    DATABASE_HEALTH_FAILURE,
    DATABASE_TRANSACTION_BEGIN,
    DATABASE_TRANSACTION_COMMIT,
    DATABASE_TRANSACTION_ROLLBACK,
    DATABASE_MIGRATION_STARTED,
    DATABASE_MIGRATION_COMPLETED,
    DATABASE_MIGRATION_FAILED,
    DATABASE_POOL_EXHAUSTED,
    DATABASE_OCC_CONFLICT,
    DATABASE_BACKUP_COMPLETED,
    DATABASE_RESTORE_COMPLETED
}

/**
 * Metric snapshot for PostgreSQL persistence monitoring.
 */
data class PersistenceMetricsSnapshot(
    val activeConnections: Int,
    val idleConnections: Int,
    val totalAcquisitions: Long,
    val acquisitionFailures: Long,
    val transactionsCommitted: Long,
    val transactionsRolledBack: Long,
    val averageLatencyMs: Double
)

/**
 * Lightweight, production-grade observability and structured logging sink for PostgreSQL persistence (INFRA-02 Step 03).
 */
object PostgresObservability {

    private val transactionsCommitted = AtomicLong(0)
    private val transactionsRolledBack = AtomicLong(0)
    private val totalLatencyMs = AtomicLong(0)
    private val totalOperations = AtomicLong(0)

    private val logEventListeners = mutableListOf<(event: PersistenceLogEvent, metadata: Map<String, Any?>) -> Unit>()

    fun recordTransactionCommit(latencyMs: Long) {
        transactionsCommitted.incrementAndGet()
        totalLatencyMs.addAndGet(latencyMs)
        totalOperations.incrementAndGet()
        logEvent(
            PersistenceLogEvent.DATABASE_TRANSACTION_COMMIT,
            mapOf("latencyMs" to latencyMs, "status" to "COMMITTED")
        )
    }

    fun recordTransactionRollback(latencyMs: Long, reason: String? = null) {
        transactionsRolledBack.incrementAndGet()
        totalLatencyMs.addAndGet(latencyMs)
        totalOperations.incrementAndGet()
        logEvent(
            PersistenceLogEvent.DATABASE_TRANSACTION_ROLLBACK,
            mapOf("latencyMs" to latencyMs, "status" to "ROLLED_BACK", "reason" to (reason ?: "Exception"))
        )
    }

    fun logEvent(event: PersistenceLogEvent, metadata: Map<String, Any?> = emptyMap()) {
        val sanitized = metadata.mapValues { (k, v) ->
            if (k.contains("password", ignoreCase = true) || k.contains("secret", ignoreCase = true)) {
                "[REDACTED]"
            } else {
                v
            }
        }
        for (listener in logEventListeners) {
            try {
                listener(event, sanitized)
            } catch (_: Exception) {}
        }
    }

    fun addEventListener(listener: (event: PersistenceLogEvent, metadata: Map<String, Any?>) -> Unit) {
        logEventListeners.add(listener)
    }

    fun clearListeners() {
        logEventListeners.clear()
    }

    fun getMetricsSnapshot(provider: PostgresConnectionProvider): PersistenceMetricsSnapshot {
        val ops = totalOperations.get()
        val avgLatency = if (ops > 0) totalLatencyMs.get().toDouble() / ops else 0.0
        return PersistenceMetricsSnapshot(
            activeConnections = provider.getActiveConnectionCount(),
            idleConnections = provider.getIdleConnectionCount(),
            totalAcquisitions = provider.getTotalAcquisitions(),
            acquisitionFailures = provider.getAcquisitionFailureCount(),
            transactionsCommitted = transactionsCommitted.get(),
            transactionsRolledBack = transactionsRolledBack.get(),
            averageLatencyMs = avgLatency
        )
    }

    fun resetMetrics() {
        transactionsCommitted.set(0)
        transactionsRolledBack.set(0)
        totalLatencyMs.set(0)
        totalOperations.set(0)
    }
}
