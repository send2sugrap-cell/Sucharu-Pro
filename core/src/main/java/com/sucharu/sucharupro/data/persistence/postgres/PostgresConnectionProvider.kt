package com.sucharu.sucharupro.data.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe, coroutine-aware PostgreSQL Connection Pool & Provider (INFRA-02 Step 03).
 */
interface PostgresConnectionProvider : AutoCloseable {
    suspend fun acquireConnection(): Connection
    suspend fun releaseConnection(connection: Connection)
    fun getActiveConnectionCount(): Int = 0
    fun getIdleConnectionCount(): Int = 0
    fun getTotalAcquisitions(): Long = 0L
    fun getAcquisitionFailureCount(): Long = 0L
    suspend fun shutdownGracefully(drainTimeoutMs: Long = 5000L) {
        close()
    }
}

/**
 * Production-hardened PostgreSQL connection pool implementation.
 */
class DefaultPostgresConnectionProvider(
    private val config: PostgresConnectionConfig,
    private val customJdbcUrl: String? = null
) : PostgresConnectionProvider {

    private val isClosed = AtomicBoolean(false)
    private val activeConnections = AtomicInteger(0)
    private val totalAcquisitions = AtomicLong(0)
    private val acquisitionFailures = AtomicLong(0)
    private val pool = ArrayBlockingQueue<Connection>(config.maxPoolSize)

    init {
        try {
            Class.forName("org.postgresql.Driver")
        } catch (_: ClassNotFoundException) {
            // Driver may be auto-loaded via ServiceLoader
        }
    }

    private fun getEffectiveUrl(): String = customJdbcUrl ?: config.toJdbcUrl()

    private fun createNewConnection(): Connection {
        val conn = DriverManager.getConnection(getEffectiveUrl(), config.user, config.password)
        conn.autoCommit = true
        return conn
    }

    override suspend fun acquireConnection(): Connection = withContext(Dispatchers.IO) {
        check(!isClosed.get()) { "Connection pool '${config.poolName}' is closed." }

        // 1. Try to get an idle connection from the pool
        var conn = pool.poll()
        if (conn != null) {
            try {
                if (conn.isValid(2)) {
                    totalAcquisitions.incrementAndGet()
                    return@withContext conn
                } else {
                    conn.close()
                    activeConnections.decrementAndGet()
                }
            } catch (_: Exception) {
                conn.close()
                activeConnections.decrementAndGet()
            }
        }

        // 2. If pool is not at max capacity, create a new connection
        if (activeConnections.get() < config.maxPoolSize) {
            val count = activeConnections.incrementAndGet()
            if (count <= config.maxPoolSize) {
                try {
                    val newConn = createNewConnection()
                    totalAcquisitions.incrementAndGet()
                    return@withContext newConn
                } catch (e: Exception) {
                    activeConnections.decrementAndGet()
                    acquisitionFailures.incrementAndGet()
                    throw e
                }
            } else {
                activeConnections.decrementAndGet()
            }
        }

        // 3. Wait for an available connection from the pool
        val waitedConn = pool.poll(config.connectionTimeoutMs, TimeUnit.MILLISECONDS)
        if (waitedConn == null) {
            acquisitionFailures.incrementAndGet()
            throw SQLException("Timeout acquiring PostgreSQL connection after ${config.connectionTimeoutMs}ms (pool exhausted: active=${activeConnections.get()}, max=${config.maxPoolSize}).")
        }

        if (!waitedConn.isValid(2)) {
            waitedConn.close()
            activeConnections.decrementAndGet()
            val newConn = createNewConnection()
            totalAcquisitions.incrementAndGet()
            return@withContext newConn
        }

        totalAcquisitions.incrementAndGet()
        waitedConn
    }

    override suspend fun releaseConnection(connection: Connection) = withContext(Dispatchers.IO) {
        if (isClosed.get()) {
            try {
                connection.close()
            } finally {
                activeConnections.decrementAndGet()
            }
            return@withContext
        }

        try {
            if (!connection.isClosed && connection.isValid(2)) {
                // Ensure transaction is rolled back if uncommitted
                if (!connection.autoCommit) {
                    connection.rollback()
                    connection.autoCommit = true
                }
                
                // Clear tenant session context to prevent cross-tenant state leakage on reuse
                try {
                    connection.prepareStatement("SELECT set_config('app.current_project_id', '', false)").use { stmt ->
                        stmt.execute()
                    }
                } catch (_: Exception) {
                    // Ignore session reset error on mock/fallback
                }

                if (!pool.offer(connection)) {
                    // Pool is full, close excess
                    connection.close()
                    activeConnections.decrementAndGet()
                }
            } else {
                connection.close()
                activeConnections.decrementAndGet()
            }
        } catch (_: Exception) {
            try { connection.close() } catch (_: Exception) {}
            activeConnections.decrementAndGet()
        }
    }

    override fun getActiveConnectionCount(): Int = activeConnections.get() - pool.size
    override fun getIdleConnectionCount(): Int = pool.size
    override fun getTotalAcquisitions(): Long = totalAcquisitions.get()
    override fun getAcquisitionFailureCount(): Long = acquisitionFailures.get()

    override suspend fun shutdownGracefully(drainTimeoutMs: Long) = withContext(Dispatchers.IO) {
        if (isClosed.compareAndSet(false, true)) {
            val start = System.currentTimeMillis()
            while (pool.isNotEmpty() || (activeConnections.get() > 0 && System.currentTimeMillis() - start < drainTimeoutMs)) {
                val conn = pool.poll(100, TimeUnit.MILLISECONDS)
                if (conn != null) {
                    try { conn.close() } catch (_: Exception) {}
                    activeConnections.decrementAndGet()
                }
            }
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            while (pool.isNotEmpty()) {
                val conn = pool.poll()
                try { conn?.close() } catch (_: Exception) {}
                activeConnections.decrementAndGet()
            }
        }
    }
}
