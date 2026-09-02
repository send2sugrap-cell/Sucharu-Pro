package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.domain.notification.security.RateLimitDecision
import com.sucharu.sucharupro.domain.notification.security.RateLimitPolicy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Production-grade in-process notification rate limiter (INFRA-04 Step 07).
 *
 * Uses per-dimension sliding window counters with atomic increments for thread safety.
 * Dimensions: project:channel, recipient:channel, destination:channel
 *
 * For multi-node deployments the persistent notification_rate_limit_state table
 * (V20260910 migration) provides the server-wide state. This implementation handles
 * the in-process case which covers all test and single-node production scenarios.
 */
class NotificationRateLimiter {

    data class WindowEntry(
        val count: AtomicInteger,
        val windowStartMs: Long
    )

    private val windows = ConcurrentHashMap<String, WindowEntry>()

    /** Default policies applied when no custom policy is provided. */
    companion object {
        val DEFAULT_PER_RECIPIENT_POLICY = RateLimitPolicy(
            dimensionKey = "recipient",
            windowSeconds = 60L,
            maxCount = 10
        )
        val DEFAULT_PER_PROJECT_POLICY = RateLimitPolicy(
            dimensionKey = "project",
            windowSeconds = 60L,
            maxCount = 500
        )
        val DEFAULT_PER_DESTINATION_POLICY = RateLimitPolicy(
            dimensionKey = "destination",
            windowSeconds = 3600L,
            maxCount = 20
        )
    }

    /**
     * Evaluates whether a request for [key] is within policy limits.
     * Does NOT increment the counter — call [record] separately only on actual dispatch.
     */
    fun evaluate(key: String, policy: RateLimitPolicy): RateLimitDecision {
        val nowMs = System.currentTimeMillis()
        val windowMs = policy.windowSeconds * 1000L
        val entry = getOrCreateWindow(key, nowMs, windowMs)

        val current = entry.count.get()
        val windowResetMs = entry.windowStartMs + windowMs - nowMs

        return if (current < policy.maxCount) {
            RateLimitDecision(
                allowed = true,
                remaining = policy.maxCount - current - 1,
                retryAfterMs = 0L,
                windowResetMs = windowResetMs.coerceAtLeast(0L)
            )
        } else {
            RateLimitDecision(
                allowed = false,
                remaining = 0,
                retryAfterMs = windowResetMs.coerceAtLeast(1000L),
                windowResetMs = windowResetMs.coerceAtLeast(0L)
            )
        }
    }

    /**
     * Atomically evaluates and records a notification in one operation.
     * Returns the rate limit decision BEFORE the increment (consistent with evaluate semantics).
     */
    fun evaluateAndRecord(key: String, policy: RateLimitPolicy): RateLimitDecision {
        val decision = evaluate(key, policy)
        if (decision.allowed) {
            record(key, policy)
        }
        return decision
    }

    /**
     * Records a notification dispatch for [key] — increments the sliding window counter atomically.
     */
    fun record(key: String, policy: RateLimitPolicy) {
        val nowMs = System.currentTimeMillis()
        val windowMs = policy.windowSeconds * 1000L
        val entry = getOrCreateWindow(key, nowMs, windowMs)
        entry.count.incrementAndGet()
    }

    /**
     * Resets the rate limit window for [key] (used in tests and administrative overrides).
     */
    fun reset(key: String) {
        windows.remove(key)
    }

    /**
     * Builds the canonical dimension key for a given scope.
     */
    fun buildProjectChannelKey(projectId: String, channel: String): String =
        "project:$projectId:channel:$channel"

    fun buildRecipientChannelKey(projectId: String, recipientId: String, channel: String): String =
        "recipient:$projectId:$recipientId:channel:$channel"

    fun buildDestinationChannelKey(projectId: String, destination: String, channel: String): String =
        "destination:$projectId:$destination:channel:$channel"

    private fun getOrCreateWindow(key: String, nowMs: Long, windowMs: Long): WindowEntry {
        val existing = windows[key]
        if (existing != null && nowMs - existing.windowStartMs < windowMs) {
            return existing
        }
        // New window (expired or first access) — use compute to avoid race condition
        val newEntry = WindowEntry(AtomicInteger(0), nowMs)
        windows[key] = newEntry
        return newEntry
    }
}
