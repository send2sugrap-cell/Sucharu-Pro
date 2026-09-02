package com.sucharu.sucharupro.data.observability.model

import java.util.UUID

/**
 * High-level service and component health states (INFRA-05 Step 06).
 */
enum class HealthStatus {
    UP,
    DEGRADED,
    DOWN,
    STARTING;

    val isHealthy: Boolean get() = this == UP || this == DEGRADED
}

/**
 * Traffic readiness status determining whether instance is prepared to serve API requests.
 */
enum class ReadinessStatus {
    READY,
    DEGRADED,
    NOT_READY;

    val canAcceptTraffic: Boolean get() = this == READY || this == DEGRADED
}

/**
 * Canonical types of metric instruments.
 */
enum class MetricType {
    COUNTER,
    GAUGE,
    TIMER,
    HISTOGRAM
}

/**
 * Security event types for audit, alerting, and operational telemetry.
 */
enum class SecurityEventType {
    AUTHENTICATION_FAILED,
    INVALID_TOKEN,
    EXPIRED_TOKEN,
    INVALID_SIGNATURE,
    AUTHORIZATION_DENIED,
    OWNERSHIP_DENIED,
    TENANT_SPOOF_ATTEMPT,
    WEBHOOK_SIGNATURE_FAILED,
    WEBHOOK_REPLAY,
    SSRF_BLOCKED,
    RATE_LIMITED,
    CIRCUIT_BREAKER_OPEN,
    SECRET_LEAKAGE_PREVENTION_EVENT
}

/**
 * Operational event types representing lifecycle and infrastructure state changes.
 */
enum class OperationalEventType {
    SERVER_STARTED,
    SERVER_STOPPING,
    SERVER_STOPPED,
    WORKER_STARTED,
    WORKER_STOPPING,
    WORKER_RECOVERY,
    DATABASE_DEGRADED,
    DATABASE_RECOVERED,
    MIGRATION_FAILED,
    MIGRATION_READY,
    CIRCUIT_OPENED,
    CIRCUIT_RECOVERED,
    JOB_DEAD_LETTERED,
    QUEUE_BACKLOG_HIGH
}

/**
 * Log severity levels.
 */
enum class ObservabilityLogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL
}

/**
 * Health assessment of an individual subsystem / component.
 */
data class ComponentHealth(
    val name: String,
    val status: HealthStatus,
    val message: String? = null,
    val details: Map<String, Any> = emptyMap(),
    val checkedAt: Long = System.currentTimeMillis()
)

/**
 * Security telemetry event record.
 */
data class SecurityEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: SecurityEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val correlationId: String,
    val component: String,
    val reasonCode: String,
    val severity: String = "WARN",
    val details: Map<String, String> = emptyMap()
)

/**
 * Operational state event record.
 */
data class OperationalEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: OperationalEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val correlationId: String,
    val component: String,
    val summary: String,
    val details: Map<String, String> = emptyMap()
)

/**
 * Internal Administrative Operational Summary (Protected, Non-Public).
 */
data class OperationalSnapshot(
    val serverStatus: HealthStatus,
    val readiness: ReadinessStatus,
    val databaseStatus: HealthStatus,
    val workerStatus: HealthStatus,
    val integrationStatus: HealthStatus,
    val redisStatus: HealthStatus = HealthStatus.UP,
    val activeWorkers: Int,
    val queueDepth: Int,
    val totalRequests: Long,
    val errorRatePercentage: Double,
    val deadLetterCount: Long,
    val circuitBreakerOpenCount: Int,
    val releaseMetadata: ReleaseMetadata? = null,
    val timestamp: Long = System.currentTimeMillis()
)
