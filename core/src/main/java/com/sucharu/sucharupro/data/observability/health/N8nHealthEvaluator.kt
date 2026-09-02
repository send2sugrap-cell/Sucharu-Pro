package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.domain.observability.N8nIntegrationHealth
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import java.util.concurrent.atomic.AtomicLong

/**
 * Health evaluator for n8n Webhook Automation integration (INFRA-04 Step 09).
 */
class N8nHealthEvaluator {

    private val dispatches = AtomicLong(0)
    private val successes = AtomicLong(0)
    private val failures = AtomicLong(0)
    private val retries = AtomicLong(0)
    private val sigRejections = AtomicLong(0)
    private val latencySum = AtomicLong(0)

    fun recordWebhook(isSuccess: Boolean, latencyMs: Long, isSignatureRejected: Boolean = false, isRetry: Boolean = false) {
        dispatches.incrementAndGet()
        if (isSuccess) {
            successes.incrementAndGet()
            latencySum.addAndGet(latencyMs)
        } else {
            failures.incrementAndGet()
        }
        if (isSignatureRejected) sigRejections.incrementAndGet()
        if (isRetry) retries.incrementAndGet()
    }

    fun evaluate(): N8nIntegrationHealth {
        val issues = mutableListOf<String>()
        val d = dispatches.get()
        val s = successes.get()
        val f = failures.get()
        val r = retries.get()
        val sigR = sigRejections.get()
        val avgLat = if (s > 0) latencySum.get().toDouble() / s else 0.0

        var status = OperationalHealthStatus.HEALTHY
        if (sigR > 10 || (d > 20 && (f.toDouble() / d) > 0.4)) {
            status = OperationalHealthStatus.CRITICAL
            if (sigR > 10) issues.add("High n8n webhook signature rejection rate: $sigR")
            if (d > 20 && (f.toDouble() / d) > 0.4) issues.add("Over 40% n8n webhook failure rate")
        } else if (sigR > 0 || (d > 10 && (f.toDouble() / d) > 0.15)) {
            status = OperationalHealthStatus.DEGRADED
            if (sigR > 0) issues.add("n8n signature rejection detected: $sigR")
            if (f > 5) issues.add("Elevated n8n webhook failure count: $f")
        }

        return N8nIntegrationHealth(
            status = status,
            totalDispatches = d,
            successfulWebhooks = s,
            failedWebhooks = f,
            retriedWebhooks = r,
            signatureRejections = sigR,
            averageLatencyMs = avgLat,
            issues = issues
        )
    }

    fun reset() {
        dispatches.set(0)
        successes.set(0)
        failures.set(0)
        retries.set(0)
        sigRejections.set(0)
        latencySum.set(0)
    }
}
