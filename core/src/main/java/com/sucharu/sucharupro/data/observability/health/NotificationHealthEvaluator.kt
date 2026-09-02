package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.observability.DeliveryHealth
import com.sucharu.sucharupro.domain.observability.NotificationInfrastructureHealth
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import com.sucharu.sucharupro.domain.observability.ProviderHealthSnapshot
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Health evaluator for Notification Delivery Infrastructure & Providers (INFRA-04 Step 09).
 */
class NotificationHealthEvaluator {

    private class ChannelStats {
        val dispatched = AtomicLong(0)
        val delivered = AtomicLong(0)
        val failed = AtomicLong(0)
        val retried = AtomicLong(0)
        val suppressed = AtomicLong(0)
        val latencySum = AtomicLong(0)
    }

    private class ProviderStats {
        val successes = AtomicLong(0)
        val failures = AtomicLong(0)
        val consecutiveFailures = AtomicInteger(0)
        val latencySum = AtomicLong(0)
        var lastSuccessAt: Long? = null
        var lastFailureAt: Long? = null
        var circuitState: String = "CLOSED"
    }

    private val channelMap = ConcurrentHashMap<NotificationChannel, ChannelStats>()
    private val providerMap = ConcurrentHashMap<String, ProviderStats>()

    fun recordDelivery(channel: NotificationChannel, isSuccess: Boolean, latencyMs: Long, isSuppressed: Boolean = false) {
        val stats = channelMap.computeIfAbsent(channel) { ChannelStats() }
        stats.dispatched.incrementAndGet()
        if (isSuppressed) {
            stats.suppressed.incrementAndGet()
        } else if (isSuccess) {
            stats.delivered.incrementAndGet()
            stats.latencySum.addAndGet(latencyMs)
        } else {
            stats.failed.incrementAndGet()
        }
    }

    fun recordProviderResult(providerName: String, isSuccess: Boolean, latencyMs: Long) {
        val stats = providerMap.computeIfAbsent(providerName) { ProviderStats() }
        if (isSuccess) {
            stats.successes.incrementAndGet()
            stats.consecutiveFailures.set(0)
            stats.lastSuccessAt = System.currentTimeMillis()
            stats.circuitState = "CLOSED"
            stats.latencySum.addAndGet(latencyMs)
        } else {
            stats.failures.incrementAndGet()
            val consec = stats.consecutiveFailures.incrementAndGet()
            stats.lastFailureAt = System.currentTimeMillis()
            if (consec >= 5) {
                stats.circuitState = "OPEN"
            }
        }
    }

    fun evaluate(activeSuppressionsCount: Long = 0L, rateLimitHitsCount: Long = 0L): NotificationInfrastructureHealth {
        val issues = mutableListOf<String>()
        val channelHealthMap = mutableMapOf<NotificationChannel, DeliveryHealth>()

        var totalDispatched = 0L
        var totalDelivered = 0L

        for (channel in NotificationChannel.entries) {
            val stats = channelMap[channel] ?: ChannelStats()
            val d = stats.dispatched.get()
            val del = stats.delivered.get()
            val f = stats.failed.get()
            val r = stats.retried.get()
            val s = stats.suppressed.get()
            val lSum = stats.latencySum.get()

            totalDispatched += d
            totalDelivered += del

            val delRate = if (d > 0) (del.toDouble() / d) * 100.0 else 100.0
            val failRate = if (d > 0) (f.toDouble() / d) * 100.0 else 0.0
            val avgLat = if (del > 0) lSum.toDouble() / del else 0.0

            val chStatus = when {
                d > 10 && delRate < 80.0 -> {
                    issues.add("Channel ${channel.name} delivery rate critical (${String.format("%.1f", delRate)}%)")
                    OperationalHealthStatus.CRITICAL
                }
                d > 5 && delRate < 95.0 -> {
                    issues.add("Channel ${channel.name} delivery rate degraded (${String.format("%.1f", delRate)}%)")
                    OperationalHealthStatus.DEGRADED
                }
                else -> OperationalHealthStatus.HEALTHY
            }

            channelHealthMap[channel] = DeliveryHealth(
                channel = channel,
                totalDispatched = d,
                totalDelivered = del,
                totalFailed = f,
                totalRetried = r,
                totalSuppressed = s,
                deliveryRate = delRate,
                failureRate = failRate,
                averageLatencyMs = avgLat,
                status = chStatus
            )
        }

        val providerSnapshots = mutableListOf<ProviderHealthSnapshot>()
        for ((pName, pStats) in providerMap) {
            val s = pStats.successes.get()
            val f = pStats.failures.get()
            val total = s + f
            val sRate = if (total > 0) (s.toDouble() / total) * 100.0 else 100.0
            val fRate = if (total > 0) (f.toDouble() / total) * 100.0 else 0.0
            val avgLat = if (s > 0) pStats.latencySum.get().toDouble() / s else 0.0
            val consec = pStats.consecutiveFailures.get()

            val pStatus = when {
                consec >= 5 || (total > 10 && sRate < 70.0) -> {
                    issues.add("Provider '$pName' is UNAVAILABLE/CRITICAL ($consec consecutive failures)")
                    OperationalHealthStatus.CRITICAL
                }
                consec >= 2 || (total > 5 && sRate < 90.0) -> {
                    issues.add("Provider '$pName' is DEGRADED")
                    OperationalHealthStatus.DEGRADED
                }
                else -> OperationalHealthStatus.HEALTHY
            }

            val channel = when {
                pName.contains("SMS", ignoreCase = true) -> NotificationChannel.SMS
                pName.contains("Email", ignoreCase = true) -> NotificationChannel.EMAIL
                pName.contains("Push", ignoreCase = true) -> NotificationChannel.PUSH
                else -> NotificationChannel.IN_APP
            }

            providerSnapshots.add(
                ProviderHealthSnapshot(
                    providerName = pName,
                    channel = channel,
                    status = pStatus,
                    successRate = sRate,
                    failureRate = fRate,
                    averageLatencyMs = avgLat,
                    consecutiveFailures = consec,
                    circuitState = pStats.circuitState,
                    lastSuccessfulAt = pStats.lastSuccessAt,
                    lastFailureAt = pStats.lastFailureAt
                )
            )
        }

        val overallDelRate = if (totalDispatched > 0) (totalDelivered.toDouble() / totalDispatched) * 100.0 else 100.0
        val overallStatus = when {
            channelHealthMap.values.any { it.status == OperationalHealthStatus.CRITICAL } ||
            providerSnapshots.any { it.status == OperationalHealthStatus.CRITICAL } -> OperationalHealthStatus.CRITICAL

            channelHealthMap.values.any { it.status == OperationalHealthStatus.DEGRADED } ||
            providerSnapshots.any { it.status == OperationalHealthStatus.DEGRADED } -> OperationalHealthStatus.DEGRADED

            else -> OperationalHealthStatus.HEALTHY
        }

        return NotificationInfrastructureHealth(
            status = overallStatus,
            overallDeliveryRate = overallDelRate,
            channelHealth = channelHealthMap,
            providerHealth = providerSnapshots,
            activeSuppressions = activeSuppressionsCount,
            rateLimitHits = rateLimitHitsCount,
            issues = issues
        )
    }

    fun reset() {
        channelMap.clear()
        providerMap.clear()
    }
}
