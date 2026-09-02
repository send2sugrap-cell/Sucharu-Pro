package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.ApiErrorResponse
import com.sucharu.sucharupro.data.api.model.ApiException
import com.sucharu.sucharupro.data.api.model.ErrorCode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory token bucket rate limiter for API abuse prevention (INFRA-02 Step 04).
 */
class BackendRateLimiter(
    private val maxRequestsPerWindow: Int = 100,
    private val windowDurationMs: Long = 60000L
) {
    private data class ClientWindow(
        val windowStart: AtomicLong,
        val requestCount: AtomicInteger
    )

    private val clients = ConcurrentHashMap<String, ClientWindow>()

    /**
     * Checks if the client has exceeded rate limits. Throws [ApiException] with `RATE_LIMITED` code if exceeded.
     */
    fun checkRateLimit(clientKey: String) {
        val now = System.currentTimeMillis()
        val window = clients.computeIfAbsent(clientKey) {
            ClientWindow(AtomicLong(now), AtomicInteger(0))
        }

        if (now - window.windowStart.get() > windowDurationMs) {
            window.windowStart.set(now)
            window.requestCount.set(0)
        }

        val count = window.requestCount.incrementAndGet()
        if (count > maxRequestsPerWindow) {
            throw ApiException(
                ApiErrorResponse(
                    errorCode = ErrorCode.RATE_LIMITED,
                    message = "Rate limit exceeded. Maximum $maxRequestsPerWindow requests allowed per minute."
                )
            )
        }
    }

    fun reset() {
        clients.clear()
    }
}
