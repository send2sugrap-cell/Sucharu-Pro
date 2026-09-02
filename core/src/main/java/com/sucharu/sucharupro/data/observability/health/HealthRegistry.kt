package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.data.observability.model.ComponentHealth
import com.sucharu.sucharupro.data.observability.model.HealthStatus
import com.sucharu.sucharupro.data.observability.model.ReadinessStatus
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Contract for subsystem health evaluation.
 */
interface HealthCheck {
    val name: String
    val isCritical: Boolean get() = true
    suspend fun check(): ComponentHealth
}

/**
 * Thread-safe component health registry with failure isolation and timeout bounds (INFRA-05 Step 06).
 */
class HealthRegistry(
    private val defaultTimeoutMs: Long = 2000L
) {

    private val checks = ConcurrentHashMap<String, HealthCheck>()

    fun register(check: HealthCheck) {
        checks[check.name] = check
    }

    fun unregister(name: String) {
        checks.remove(name)
    }

    suspend fun checkComponent(name: String, timeoutMs: Long = defaultTimeoutMs): ComponentHealth {
        val check = checks[name] ?: return ComponentHealth(
            name = name,
            status = HealthStatus.DOWN,
            message = "Component '$name' is not registered."
        )

        return try {
            val result = withTimeoutOrNull(timeoutMs) {
                check.check()
            }
            result ?: ComponentHealth(
                name = name,
                status = HealthStatus.DEGRADED,
                message = "Health check timed out after ${timeoutMs}ms."
            )
        } catch (e: Exception) {
            ComponentHealth(
                name = name,
                status = HealthStatus.DOWN,
                message = e.message ?: "Health check failed."
            )
        }
    }

    suspend fun evaluateLiveness(): Boolean {
        // Liveness is minimally dependent on process responsiveness
        return true
    }

    suspend fun evaluateReadiness(timeoutMs: Long = defaultTimeoutMs): ReadinessStatus {
        if (checks.isEmpty()) return ReadinessStatus.READY

        var hasDegraded = false
        for (check in checks.values) {
            val health = checkComponent(check.name, timeoutMs)
            if (health.status == HealthStatus.DOWN) {
                if (check.isCritical) {
                    return ReadinessStatus.NOT_READY
                } else {
                    hasDegraded = true
                }
            } else if (health.status == HealthStatus.DEGRADED) {
                hasDegraded = true
            }
        }

        return if (hasDegraded) ReadinessStatus.DEGRADED else ReadinessStatus.READY
    }

    suspend fun getFullReport(timeoutMs: Long = defaultTimeoutMs): Map<String, Any> {
        val components = mutableMapOf<String, Any>()
        var overallStatus = HealthStatus.UP

        for ((name, check) in checks) {
            val health = checkComponent(name, timeoutMs)
            components[name] = mapOf(
                "status" to health.status.name,
                "message" to (health.message ?: "Healthy"),
                "checkedAt" to health.checkedAt
            )
            if (health.status == HealthStatus.DOWN) {
                if (check.isCritical) overallStatus = HealthStatus.DOWN
                else if (overallStatus == HealthStatus.UP) overallStatus = HealthStatus.DEGRADED
            } else if (health.status == HealthStatus.DEGRADED && overallStatus == HealthStatus.UP) {
                overallStatus = HealthStatus.DEGRADED
            }
        }

        return mapOf(
            "status" to overallStatus.name,
            "readiness" to evaluateReadiness(timeoutMs).name,
            "components" to components,
            "timestamp" to System.currentTimeMillis()
        )
    }
}
