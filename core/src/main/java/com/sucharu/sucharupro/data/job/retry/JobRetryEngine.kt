package com.sucharu.sucharupro.data.job.retry

import com.sucharu.sucharupro.data.event.model.RetryConfig
import java.security.SecureRandom
import kotlin.math.min

/**
 * Retry calculation engine for background jobs with exponential backoff and jitter (INFRA-04 Step 04).
 */
class JobRetryEngine(
    val retryConfig: RetryConfig = RetryConfig(
        maxAttempts = 3,
        initialBackoffMs = 1000L,
        maxBackoffMs = 60000L,
        multiplier = 2.0,
        jitterFactor = 0.1
    )
) {
    private val random = SecureRandom()

    /**
     * Calculates the next attempt delay in milliseconds given current attempt count.
     */
    fun calculateNextAttemptDelay(attemptCount: Int, explicitRetryAfterMs: Long? = null): Long {
        if (explicitRetryAfterMs != null && explicitRetryAfterMs > 0) {
            return min(explicitRetryAfterMs, retryConfig.maxBackoffMs)
        }

        val randomFactor = random.nextDouble()
        return retryConfig.calculateDelayMs(attemptCount, randomFactor)
    }

    /**
     * Returns true if job can be retried given current attempt count and max attempts.
     */
    fun canRetry(attemptCount: Int, maxAttempts: Int = retryConfig.maxAttempts): Boolean {
        return attemptCount < maxAttempts
    }
}
