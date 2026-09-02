package com.sucharu.sucharupro.domain.observability

import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import java.util.UUID

/**
 * Operational health status enum.
 */
enum class OperationalHealthStatus {
    HEALTHY,
    DEGRADED,
    CRITICAL,
    UNKNOWN
}

/**
 * Generic health assessment for any platform subsystem.
 */
data class SubsystemHealth(
    val subsystemName: String,
    val status: OperationalHealthStatus,
    val message: String,
    val latencyMs: Double = 0.0,
    val errorRate: Double = 0.0,
    val lastCheckedAt: Long = System.currentTimeMillis(),
    val issues: List<String> = emptyList()
)

/**
 * Queue/backlog depth and latency health metrics.
 */
data class QueueHealth(
    val queueName: String,
    val pendingCount: Long,
    val processingCount: Long,
    val retryCount: Long,
    val deadLetterCount: Long,
    val oldestPendingAgeMs: Long,
    val oldestProcessingAgeMs: Long,
    val status: OperationalHealthStatus
)

/**
 * Channel-specific delivery health statistics.
 */
data class DeliveryHealth(
    val channel: NotificationChannel,
    val totalDispatched: Long,
    val totalDelivered: Long,
    val totalFailed: Long,
    val totalRetried: Long,
    val totalSuppressed: Long,
    val deliveryRate: Double,
    val failureRate: Double,
    val averageLatencyMs: Double,
    val p95LatencyMs: Double = averageLatencyMs * 1.5,
    val status: OperationalHealthStatus
)

/**
 * External provider health and availability snapshot.
 */
data class ProviderHealthSnapshot(
    val providerName: String,
    val channel: NotificationChannel,
    val status: OperationalHealthStatus,
    val successRate: Double,
    val failureRate: Double,
    val averageLatencyMs: Double,
    val consecutiveFailures: Int = 0,
    val circuitState: String = "CLOSED",
    val lastSuccessfulAt: Long? = null,
    val lastFailureAt: Long? = null
)

/**
 * Domain event & transactional outbox infrastructure health.
 */
data class EventInfrastructureHealth(
    val status: OperationalHealthStatus,
    val outboxHealth: QueueHealth,
    val deadLetterCount: Long,
    val totalPublished: Long,
    val totalConsumed: Long,
    val publicationLatencyMs: Double,
    val consumerLatencyMs: Double,
    val issues: List<String> = emptyList()
)

/**
 * Notification delivery infrastructure health.
 */
data class NotificationInfrastructureHealth(
    val status: OperationalHealthStatus,
    val overallDeliveryRate: Double,
    val channelHealth: Map<NotificationChannel, DeliveryHealth>,
    val providerHealth: List<ProviderHealthSnapshot>,
    val activeSuppressions: Long,
    val rateLimitHits: Long,
    val issues: List<String> = emptyList()
)

/**
 * Background job processing health.
 */
data class BackgroundJobHealth(
    val status: OperationalHealthStatus,
    val pendingJobs: Long,
    val processingJobs: Long,
    val completedJobs: Long,
    val failedJobs: Long,
    val retriedJobs: Long,
    val deadLetterJobs: Long,
    val recoveredLeases: Long,
    val averageExecutionLatencyMs: Double,
    val issues: List<String> = emptyList()
)

/**
 * Saga workflow engine health.
 */
data class WorkflowHealth(
    val status: OperationalHealthStatus,
    val workflowsStarted: Long,
    val workflowsCompleted: Long,
    val workflowsFailed: Long,
    val workflowsCompensated: Long,
    val approvalsWaiting: Long,
    val activeWorkflows: Long,
    val issues: List<String> = emptyList()
)

/**
 * AI Agent notification integration health.
 */
data class AiAgentIntegrationHealth(
    val status: OperationalHealthStatus,
    val totalRequests: Long,
    val totalDrafts: Long,
    val totalExecutions: Long,
    val totalDenials: Long,
    val totalConfirmationsPending: Long,
    val rateLimitBlocks: Long,
    val credentialBlocks: Long,
    val issues: List<String> = emptyList()
)

/**
 * n8n automation webhook integration health.
 */
data class N8nIntegrationHealth(
    val status: OperationalHealthStatus,
    val totalDispatches: Long,
    val successfulWebhooks: Long,
    val failedWebhooks: Long,
    val retriedWebhooks: Long,
    val signatureRejections: Long,
    val averageLatencyMs: Double,
    val issues: List<String> = emptyList()
)

/**
 * Tenant-scoped aggregated operational summary.
 */
data class TenantHealthSummary(
    val projectId: String,
    val status: OperationalHealthStatus,
    val eventPendingCount: Long,
    val notificationDeliveryRate: Double,
    val activeJobsCount: Long,
    val activeWorkflowsCount: Long,
    val activeAlertsCount: Int,
    val sloCompliancePercentage: Double,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Full system health summary.
 */
data class SystemHealthSummary(
    val status: OperationalHealthStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val eventInfrastructure: EventInfrastructureHealth,
    val notificationInfrastructure: NotificationInfrastructureHealth,
    val backgroundJobInfrastructure: BackgroundJobHealth,
    val workflowInfrastructure: WorkflowHealth,
    val aiAgentIntegration: AiAgentIntegrationHealth,
    val n8nIntegration: N8nIntegrationHealth,
    val openAlertsCount: Int,
    val systemSloCompliancePercentage: Double,
    val globalIssues: List<String> = emptyList()
)

/**
 * Service Level Objective definition.
 */
data class SloDefinition(
    val sloId: String,
    val name: String,
    val subsystem: String,
    val targetPercentage: Double, // e.g. 99.0
    val measurementWindowSeconds: Long = 3600,
    val warningThreshold: Double = 98.0,
    val criticalThreshold: Double = 95.0
)

/**
 * Measured SLO performance.
 */
data class SloMeasurement(
    val sloId: String,
    val name: String,
    val subsystem: String,
    val currentPercentage: Double,
    val targetPercentage: Double,
    val isMeetingSlo: Boolean,
    val status: OperationalHealthStatus,
    val measurementWindowSeconds: Long,
    val sampleCount: Long,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * SLA Measurement.
 */
data class SlaMeasurement(
    val slaId: String,
    val name: String,
    val targetAvailability: Double,
    val measuredAvailability: Double,
    val isBreached: Boolean,
    val breachDurationMs: Long = 0,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Operational Alert Severity.
 */
enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Operational Alert Lifecycle Status.
 */
enum class AlertStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED
}

/**
 * Operational Alert model.
 */
data class OperationalAlert(
    val alertId: String = "alt-${UUID.randomUUID().toString().take(12)}",
    val projectId: String,
    val alertKey: String,
    val deduplicationKey: String,
    val title: String,
    val summary: String,
    val severity: AlertSeverity,
    val status: AlertStatus = AlertStatus.OPEN,
    val subsystem: String,
    val failureClass: String? = null,
    val occurrences: Int = 1,
    val firstOccurredAt: Long = System.currentTimeMillis(),
    val lastOccurredAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val resolutionNotes: String? = null
)

/**
 * Queue & system capacity snapshot for autoscaling / queue monitoring.
 */
data class CapacitySnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val outboxDepth: Long,
    val notificationQueueDepth: Long,
    val jobQueueDepth: Long,
    val activeWorkflowsCount: Long,
    val providerThroughputPerSec: Double,
    val estimatedTimeToDrainSec: Long,
    val status: OperationalHealthStatus
)

/**
 * Distributed Trace Context model.
 */
data class TraceContext(
    val traceId: String,
    val spanId: String = UUID.randomUUID().toString().take(16),
    val correlationId: String,
    val causationId: String? = null,
    val requestId: String? = null
) {
    companion object {
        fun createNew(correlationId: String, requestId: String? = null): TraceContext = TraceContext(
            traceId = "trc-${UUID.randomUUID().toString().replace("-", "").take(16)}",
            spanId = UUID.randomUUID().toString().replace("-", "").take(16),
            correlationId = correlationId,
            requestId = requestId
        )
    }
}
