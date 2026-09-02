package com.sucharu.sucharupro.data.observability.metrics

import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Production-grade Central Metrics Registry with Strict Low-Cardinality Protection (INFRA-05 Step 06).
 *
 * Rules:
 * 1. Metric labels are strictly bounded to allowed low-cardinality dimensions.
 * 2. High-cardinality IDs (userId, orderId, jwt, tenantId, payload) are strictly rejected from labels.
 * 3. Thread-safe, non-blocking lock-free operations using AtomicLong and ConcurrentHashMap.
 * 4. Supports safe Prometheus exposition format export.
 */
class ObservabilityMetricsRegistry {

    companion object {
        val ALLOWED_DIMENSIONS = setOf(
            "method",
            "route",
            "status_class",
            "status",
            "role",
            "capability",
            "resource_type",
            "reason",
            "job_type",
            "job_priority",
            "provider",
            "operation",
            "failure_class",
            "channel",
            "environment",
            "event_type",
            "event_category",
            "notification_channel"
        )
    }

    private val counters = ConcurrentHashMap<String, AtomicLong>()
    private val gauges = ConcurrentHashMap<String, AtomicLong>()
    private val latencySums = ConcurrentHashMap<String, AtomicLong>()
    private val latencyCounts = ConcurrentHashMap<String, AtomicLong>()

    private fun normalizeKey(metricName: String, tags: Map<String, String>): String {
        // Filter out forbidden or high-cardinality dimensions
        val filteredTags = tags.filterKeys { ALLOWED_DIMENSIONS.contains(it) }
            .toSortedMap()
            .entries
            .joinToString(separator = ",") { "${it.key}=\"${it.value.take(32)}\"" }

        return if (filteredTags.isEmpty()) metricName else "$metricName{$filteredTags}"
    }

    // --- Core Counter / Gauge / Latency Operations ---

    fun increment(metricName: String, amount: Long = 1, tags: Map<String, String> = emptyMap()) {
        val key = normalizeKey(metricName, tags)
        counters.computeIfAbsent(key) { AtomicLong(0) }.addAndGet(amount)
    }

    fun getCounter(metricName: String, tags: Map<String, String> = emptyMap()): Long {
        val key = normalizeKey(metricName, tags)
        return counters[key]?.get() ?: 0L
    }

    fun setGauge(metricName: String, value: Long, tags: Map<String, String> = emptyMap()) {
        val key = normalizeKey(metricName, tags)
        gauges.computeIfAbsent(key) { AtomicLong(0) }.set(value)
    }

    fun getGauge(metricName: String, tags: Map<String, String> = emptyMap()): Long {
        val key = normalizeKey(metricName, tags)
        return gauges[key]?.get() ?: 0L
    }

    fun recordLatency(metricName: String, latencyMs: Long, tags: Map<String, String> = emptyMap()) {
        val key = normalizeKey(metricName, tags)
        latencySums.computeIfAbsent(key) { AtomicLong(0) }.addAndGet(latencyMs)
        latencyCounts.computeIfAbsent(key) { AtomicLong(0) }.incrementAndGet()
    }

    fun getAverageLatency(metricName: String, tags: Map<String, String> = emptyMap()): Double {
        val key = normalizeKey(metricName, tags)
        val count = latencyCounts[key]?.get() ?: 0L
        val sum = latencySums[key]?.get() ?: 0L
        return if (count > 0) sum.toDouble() / count else 0.0
    }

    // --- HTTP Metrics (Section 4.1) ---

    fun recordHttpRequest(method: String, route: String, statusCode: Int, durationMs: Long) {
        val statusClass = "${statusCode / 100}xx"
        val tags = mapOf(
            "method" to method.uppercase(),
            "route" to sanitizeRoute(route),
            "status_class" to statusClass
        )
        increment("http_requests_total", 1, tags)
        recordLatency("http_request_duration_ms", durationMs, tags)
        if (statusCode >= 400) {
            increment("http_errors_total", 1, tags)
        }
    }

    fun recordSlowRequest(route: String, method: String, durationMs: Long) {
        val tags = mapOf("method" to method.uppercase(), "route" to sanitizeRoute(route))
        increment("http_slow_requests_total", 1, tags)
    }

    private fun sanitizeRoute(route: String): String {
        // Replace dynamic IDs in path with bounded placeholders to prevent metric explosion
        return route.replace(Regex("/[a-f0-9\\-]{36}"), "/:id")
            .replace(Regex("/[A-Z0-9_-]{4,}"), "/:id")
    }

    // --- Authentication Metrics (Section 4.2) ---

    fun recordAuthSuccess() {
        increment("authentication_success_total", 1)
    }

    fun recordAuthFailure(reason: String = "invalid_credentials") {
        increment("authentication_failure_total", 1)
        val tags = mapOf("reason" to reason.take(24))
        increment("authentication_failure_total", 1, tags)
    }

    // --- Authorization Metrics (Section 4.3) ---

    fun recordAuthzAllowed(role: String, capability: String) {
        increment("authorization_allowed_total", 1)
        val tags = mapOf("role" to role, "capability" to capability)
        increment("authorization_allowed_total", 1, tags)
    }

    fun recordAuthzDenied(role: String, capability: String, reason: String = "insufficient_permissions") {
        increment("authorization_denied_total", 1)
        val tags = mapOf("role" to role, "capability" to capability, "reason" to reason.take(24))
        increment("authorization_denied_total", 1, tags)
    }

    fun recordTenantBoundaryViolation(reason: String = "cross_tenant_access_blocked") {
        increment("tenant_boundary_violation_total", 1)
        val tags = mapOf("reason" to reason)
        increment("tenant_boundary_violation_total", 1, tags)
    }

    // --- Worker & Background Job Metrics (Section 4.5) ---

    fun recordJobEnqueued(jobType: String) {
        val tags = mapOf("job_type" to jobType.take(32))
        increment("jobs_enqueued_total", 1, tags)
    }

    fun recordJobClaimed(jobType: String) {
        val tags = mapOf("job_type" to jobType.take(32))
        increment("jobs_claimed_total", 1, tags)
    }

    fun recordJobStarted(jobType: String) {
        val tags = mapOf("job_type" to jobType.take(32))
        increment("jobs_started_total", 1, tags)
    }

    fun recordJobSucceeded(jobType: String, durationMs: Long) {
        val tags = mapOf("job_type" to jobType.take(32))
        increment("jobs_succeeded_total", 1, tags)
        recordLatency("job_execution_duration_ms", durationMs, tags)
    }

    fun recordJobFailed(jobType: String, failureClass: String) {
        val tags = mapOf("job_type" to jobType.take(32), "failure_class" to failureClass.take(32))
        increment("jobs_failed_total", 1, tags)
    }

    fun recordJobRetried(jobType: String) {
        val tags = mapOf("job_type" to jobType.take(32))
        increment("jobs_retried_total", 1, tags)
    }

    fun recordJobDeadLettered(jobType: String) {
        val tags = mapOf("job_type" to jobType.take(32))
        increment("jobs_dead_lettered_total", 1, tags)
    }

    fun recordJobLeaseRecovery() {
        increment("job_lease_recovery_total", 1)
    }

    // --- Webhook Metrics (Section 4.6) ---

    fun recordWebhookReceived(provider: String) {
        val tags = mapOf("provider" to provider.lowercase())
        increment("webhook_received_total", 1, tags)
    }

    fun recordWebhookVerified(provider: String) {
        val tags = mapOf("provider" to provider.lowercase())
        increment("webhook_verified_total", 1, tags)
    }

    fun recordWebhookRejected(provider: String, reason: String) {
        val tags = mapOf("provider" to provider.lowercase(), "reason" to reason.take(24))
        increment("webhook_rejected_total", 1, tags)
    }

    fun recordWebhookDuplicate(provider: String) {
        val tags = mapOf("provider" to provider.lowercase())
        increment("webhook_duplicate_total", 1, tags)
    }

    // --- External Integration & Circuit Breaker Metrics (Section 4.7 & 4.8) ---

    fun recordIntegrationRequest(provider: String, operation: String, isSuccess: Boolean, durationMs: Long, isCircuitOpen: Boolean = false) {
        val tags = mapOf("provider" to provider.lowercase(), "operation" to operation.take(32))
        increment("integration_requests_total", 1, tags)
        if (isSuccess) {
            increment("integration_success_total", 1, tags)
        } else {
            increment("integration_failure_total", 1, tags)
        }
        if (isCircuitOpen) {
            increment("integration_circuit_open_total", 1, tags)
        }
        recordLatency("integration_duration_ms", durationMs, tags)
    }

    fun recordCircuitBreakerOpen(provider: String) {
        val tags = mapOf("provider" to provider.lowercase())
        increment("circuit_breaker_open_total", 1, tags)
    }

    fun recordCircuitBreakerRecovery(provider: String) {
        val tags = mapOf("provider" to provider.lowercase())
        increment("circuit_breaker_recovery_total", 1, tags)
    }

    // --- Legacy / Domain Event Metrics ---

    fun recordEventPublished(eventType: String, projectId: String, durationMs: Long) {
        val tags = mapOf("event_type" to eventType)
        increment("events_published_total", 1, tags)
        recordLatency("event_publish_latency_ms", durationMs, tags)
    }

    fun recordEventConsumed(eventType: String, projectId: String, durationMs: Long) {
        val tags = mapOf("event_type" to eventType)
        increment("events_consumed_total", 1, tags)
        recordLatency("event_consumer_latency_ms", durationMs, tags)
    }

    fun recordNotificationDispatched(channel: NotificationChannel, projectId: String) {
        val tags = mapOf("notification_channel" to channel.name)
        increment("notifications_dispatched_total", 1, tags)
    }

    fun recordNotificationDelivered(channel: NotificationChannel, projectId: String, durationMs: Long) {
        val tags = mapOf("notification_channel" to channel.name)
        increment("notifications_delivered_total", 1, tags)
        recordLatency("notification_delivery_latency_ms", durationMs, tags)
    }

    // --- Prometheus Exposition Formatter ---

    fun formatPrometheus(): String {
        val sb = StringBuilder()
        sb.append("# HELP sucharu_metrics Sucharu Pro ERP Application Metrics\n")

        for ((key, value) in counters) {
            sb.append("${sanitizeMetricKey(key)} ${value.get()}\n")
        }
        for ((key, value) in gauges) {
            sb.append("${sanitizeMetricKey(key)} ${value.get()}\n")
        }
        for ((key, sum) in latencySums) {
            val count = latencyCounts[key]?.get() ?: 0L
            val metricBase = key.substringBefore("{")
            val tags = if (key.contains("{")) "{" + key.substringAfter("{") else ""
            sb.append("${sanitizeMetricKey("${metricBase}_sum$tags")} ${sum.get()}\n")
            sb.append("${sanitizeMetricKey("${metricBase}_count$tags")} $count\n")
        }

        return sb.toString()
    }

    private fun sanitizeMetricKey(key: String): String {
        return key.replace("-", "_")
    }

    fun reset() {
        counters.clear()
        gauges.clear()
        latencySums.clear()
        latencyCounts.clear()
    }
}
