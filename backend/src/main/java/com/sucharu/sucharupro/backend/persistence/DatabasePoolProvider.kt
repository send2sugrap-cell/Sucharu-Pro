package com.sucharu.sucharupro.backend.persistence

import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production PostgreSQL Connection Pool Provider powered by HikariCP.
 * Thread-safe, non-blocking coroutine acquisition, connection health checks, and graceful shutdown.
 */
class DatabasePoolProvider(
    private val config: BackendConfig
) : PostgresConnectionProvider {

    private val logger = LoggerFactory.getLogger(DatabasePoolProvider::class.java)
    private val isClosed = AtomicBoolean(false)

    val dataSource: HikariDataSource by lazy {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.databaseUrl
            username = config.databaseUser
            password = config.databasePassword
            maximumPoolSize = config.databasePoolSize
            minimumIdle = 2.coerceAtMost(config.databasePoolSize)
            connectionTimeout = 10000 // 10s
            idleTimeout = 300000 // 5m
            maxLifetime = 1800000 // 30m
            leakDetectionThreshold = 60000 // 60s
            poolName = "SucharuBackendHikariPool"
            
            // Standard PostgreSQL driver optimizations
            addDataSourceProperty("reWriteBatchedInserts", "true")
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        logger.info("Initializing HikariCP pool '{}' to {}", hikariConfig.poolName, config.databaseUrl)
        HikariDataSource(hikariConfig)
    }

    override suspend fun acquireConnection(): Connection = withContext(Dispatchers.IO) {
        check(!isClosed.get()) { "DatabasePoolProvider has been closed." }
        dataSource.connection
    }

    override suspend fun releaseConnection(connection: Connection) = withContext(Dispatchers.IO) {
        if (!connection.isClosed) {
            connection.close()
        }
    }

    fun isHealthy(): Boolean {
        return try {
            if (isClosed.get()) return false
            dataSource.connection.use { conn ->
                conn.isValid(2)
            }
        } catch (e: Exception) {
            logger.warn("Database health check probe failed: {}", e.message)
            false
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            logger.info("Closing database connection pool...")
            try {
                dataSource.close()
                logger.info("Database connection pool closed successfully.")
            } catch (e: Exception) {
                logger.error("Error closing HikariCP pool", e)
            }
        }
    }
}
