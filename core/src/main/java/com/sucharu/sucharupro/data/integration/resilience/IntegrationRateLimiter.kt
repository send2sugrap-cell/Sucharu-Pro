package com.sucharu.sucharupro.data.integration.resilience

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * Token-bucket rate limiter for external provider interactions (INFRA-05 Step 05).
 */
class IntegrationRateLimiter(
    val maxRequestsPerSecond: Int = 10,
    val burstCapacity: Int = 20
) {
    private val availableTokens = AtomicInteger(burstCapacity)
    private val lastRefillTimestamp = AtomicLong(System.currentTimeMillis())
    private val backoffUntilTimestamp = AtomicLong(0L)

    /**
     * Checks if a request is permitted under rate limit policy.
     * @return true if allowed; false if rate-limited.
     */
    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()

        // 1. Check active 429 backoff
        if (now < backoffUntilTimestamp.get()) {
            return false
        }

        refillTokens(now)

        while (true) {
            val current = availableTokens.get()
            if (current <= 0) return false
            if (availableTokens.compareAndSet(current, current - 1)) {
                return true
            }
        }
    }

    /**
     * Applies a mandatory backoff duration (e.g. from HTTP 429 Retry-After response header).
     */
    fun applyRetryAfter(retryAfterSeconds: Long) {
        val boundedSeconds = min(retryAfterSeconds, 300L) // Max 5 minutes backoff
        backoffUntilTimestamp.set(System.currentTimeMillis() + (boundedSeconds * 1000L))
    }

    private fun refillTokens(now: Long) {
        val lastRefill = lastRefillTimestamp.get()
        val elapsedMs = now - lastRefill
        if (elapsedMs >= 1000L) {
            if (lastRefillTimestamp.compareAndSet(lastRefill, now)) {
                val tokensToAdd = ((elapsedMs / 1000L) * maxRequestsPerSecond).toInt()
                availableTokens.updateAndGet { current ->
                    min(burstCapacity, current + tokensToAdd)
                }
            }
        }
    }
}

/**
 * Registry of rate limiters per provider / integration key.
 */
class IntegrationRateLimiterRegistry(
    private val defaultRps: Int = 10,
    private val defaultBurst: Int = 20
) {
    private val limiters = ConcurrentHashMap<String, IntegrationRateLimiter>()

    fun getRateLimiter(key: String): IntegrationRateLimiter {
        return limiters.computeIfAbsent(key) {
            IntegrationRateLimiter(maxRequestsPerSecond = defaultRps, burstCapacity = defaultBurst)
        }
    }

    fun resetAll() {
        limiters.clear()
    }
}
