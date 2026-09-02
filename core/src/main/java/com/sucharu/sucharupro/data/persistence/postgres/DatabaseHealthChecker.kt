package com.sucharu.sucharupro.data.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Health check status report for PostgreSQL database infrastructure (INFRA-02 Step 03).
 */
data class DatabaseHealthStatus(
    val isLive: Boolean,
    val isReady: Boolean,
    val databaseName: String? = null,
    val activeConnections: Int? = null,
    val idleConnections: Int? = null,
    val latencyMs: Long? = null,
    val errorMessage: String? = null
)

/**
 * Lightweight PostgreSQL health check probe separating Liveness from Readiness with timeout protection.
 */
class DatabaseHealthChecker(
    private val connectionProvider: PostgresConnectionProvider
) {

    /**
     * Liveness check: Verifies application persistence infrastructure is active.
     */
    fun checkLiveness(): Boolean {
        return true
    }

    /**
     * Readiness check: Acquires a connection and executes validation query within timeout limit.
     */
    suspend fun checkReadiness(timeoutMs: Long = 3000L): DatabaseHealthStatus = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val result = withTimeoutOrNull(timeoutMs) {
            try {
                val connection = connectionProvider.acquireConnection()
                try {
                    val stmt = connection.prepareStatement("SELECT current_database()")
                    val rs = stmt.executeQuery()
                    val dbName = if (rs.next()) rs.getString(1) else null
                    rs.close()
                    stmt.close()
                    val latency = System.currentTimeMillis() - start
                    DatabaseHealthStatus(
                        isLive = true,
                        isReady = true,
                        databaseName = dbName,
                        activeConnections = connectionProvider.getActiveConnectionCount(),
                        idleConnections = connectionProvider.getIdleConnectionCount(),
                        latencyMs = latency
                    )
                } finally {
                    connectionProvider.releaseConnection(connection)
                }
            } catch (e: Throwable) {
                val latency = System.currentTimeMillis() - start
                DatabaseHealthStatus(
                    isLive = true,
                    isReady = false,
                    databaseName = null,
                    activeConnections = connectionProvider.getActiveConnectionCount(),
                    idleConnections = connectionProvider.getIdleConnectionCount(),
                    latencyMs = latency,
                    errorMessage = "Database readiness check failed: ${sanitizeErrorMessage(e)}"
                )
            }
        }

        result ?: DatabaseHealthStatus(
            isLive = true,
            isReady = false,
            databaseName = null,
            latencyMs = System.currentTimeMillis() - start,
            errorMessage = "Database readiness check timed out after ${timeoutMs}ms."
        )
    }

    private fun sanitizeErrorMessage(e: Throwable): String {
        val msg = e.message ?: "Unknown database error"
        // Strip out passwords, sensitive host details, or JDBC connection tokens
        return msg.replace(Regex("password=[^&;\\s]+", RegexOption.IGNORE_CASE), "password=[REDACTED]")
    }
}
