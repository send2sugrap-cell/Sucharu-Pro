package com.sucharu.sucharupro.data.integration.resilience

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Circuit breaker states (INFRA-05 Step 05).
 */
enum class CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

/**
 * Circuit Breaker implementation for external provider integration failure isolation.
 */
class IntegrationCircuitBreaker(
    val failureThreshold: Int = 5,
    val resetTimeoutMs: Long = 30000L,
    val halfOpenProbeLimit: Int = 2
) {

    private val circuitState = AtomicReference(CircuitState.CLOSED)
    private val failureCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0L)
    private val halfOpenSuccessCount = AtomicInteger(0)

    fun getState(): CircuitState {
        val current = circuitState.get()
        if (current == CircuitState.OPEN) {
            val now = System.currentTimeMillis()
            if (now - lastFailureTime.get() >= resetTimeoutMs) {
                if (circuitState.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    halfOpenSuccessCount.set(0)
                    return CircuitState.HALF_OPEN
                }
            }
        }
        return circuitState.get()
    }

    /**
     * Checks if a request is allowed through the circuit.
     * @return true if allowed; false if circuit is OPEN.
     */
    fun allowRequest(): Boolean {
        return when (getState()) {
            CircuitState.CLOSED -> true
            CircuitState.HALF_OPEN -> true
            CircuitState.OPEN -> false
        }
    }

    /**
     * Records a successful provider response.
     */
    fun recordSuccess() {
        when (circuitState.get()) {
            CircuitState.HALF_OPEN -> {
                val successes = halfOpenSuccessCount.incrementAndGet()
                if (successes >= halfOpenProbeLimit) {
                    // Reset to CLOSED
                    circuitState.set(CircuitState.CLOSED)
                    failureCount.set(0)
                }
            }
            CircuitState.CLOSED -> {
                failureCount.set(0)
            }
            CircuitState.OPEN -> {}
        }
    }

    /**
     * Records an execution failure.
     */
    fun recordFailure() {
        lastFailureTime.set(System.currentTimeMillis())
        when (circuitState.get()) {
            CircuitState.HALF_OPEN -> {
                // Trip back to OPEN immediately on failure during probe
                circuitState.set(CircuitState.OPEN)
            }
            CircuitState.CLOSED -> {
                val failures = failureCount.incrementAndGet()
                if (failures >= failureThreshold) {
                    circuitState.set(CircuitState.OPEN)
                }
            }
            CircuitState.OPEN -> {}
        }
    }

    fun reset() {
        circuitState.set(CircuitState.CLOSED)
        failureCount.set(0)
        lastFailureTime.set(0L)
        halfOpenSuccessCount.set(0)
    }
}

/**
 * Registry managing circuit breakers per provider / integration identifier.
 */
class IntegrationCircuitBreakerRegistry(
    private val defaultFailureThreshold: Int = 5,
    private val defaultResetTimeoutMs: Long = 30000L
) {
    private val breakers = ConcurrentHashMap<String, IntegrationCircuitBreaker>()

    fun getBreaker(key: String): IntegrationCircuitBreaker {
        return breakers.computeIfAbsent(key) {
            IntegrationCircuitBreaker(
                failureThreshold = defaultFailureThreshold,
                resetTimeoutMs = defaultResetTimeoutMs
            )
        }
    }

    fun resetAll() {
        breakers.values.forEach { it.reset() }
        breakers.clear()
    }
}
