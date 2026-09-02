package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.data.observability.model.ComponentHealth
import com.sucharu.sucharupro.data.observability.model.HealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

/**
 * Production Redis connectivity and readiness validator (INFRA-05 Step 07).
 * Validates Redis endpoint reachable when enabled; non-blocking and safe when disabled.
 * Redis is an infrastructure acceleration layer, never an authoritative store for durable business data.
 */
class RedisHealthChecker(
    val redisEnabled: Boolean = false,
    val redisUrl: String? = null,
    val timeoutMs: Int = 1500
) : HealthCheck {

    override val name: String = "redis"
    override val isCritical: Boolean = false // Redis is optional/acceleration; PostgreSQL remains authoritative

    override suspend fun check(): ComponentHealth {
        if (!redisEnabled || redisUrl.isNullOrBlank()) {
            return ComponentHealth(
                name = name,
                status = HealthStatus.UP,
                message = "Redis acceleration layer is disabled. PostgreSQL is authoritative.",
                details = mapOf("enabled" to false)
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val uri = URI(if (redisUrl.startsWith("redis://") || redisUrl.startsWith("rediss://")) redisUrl else "redis://$redisUrl")
                val host = uri.host ?: "localhost"
                val port = if (uri.port > 0) uri.port else 6379

                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    socket.soTimeout = timeoutMs

                    // Send PING command via RESP protocol
                    val out = socket.getOutputStream()
                    out.write("*1\r\n$4\r\nPING\r\n".toByteArray(Charsets.UTF_8))
                    out.flush()

                    val reader = socket.getInputStream().bufferedReader()
                    val line = reader.readLine()
                    if (line != null && (line.contains("+PONG") || line.contains("NOAUTH") || line.contains("PONG"))) {
                        ComponentHealth(
                            name = name,
                            status = HealthStatus.UP,
                            message = "Redis connection responsive at $host:$port.",
                            details = mapOf("enabled" to true, "host" to host, "port" to port)
                        )
                    } else {
                        ComponentHealth(
                            name = name,
                            status = HealthStatus.DEGRADED,
                            message = "Redis responded with unexpected output: $line",
                            details = mapOf("enabled" to true, "host" to host, "port" to port)
                        )
                    }
                }
            } catch (e: Exception) {
                ComponentHealth(
                    name = name,
                    status = HealthStatus.DEGRADED,
                    message = "Redis ping failed: ${e.message ?: "Connection failed"}",
                    details = mapOf("enabled" to true, "error" to (e.message ?: "unreachable"))
                )
            }
        }
    }
}
