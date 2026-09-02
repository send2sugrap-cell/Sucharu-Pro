package com.sucharu.sucharupro.data.persistence.postgres

/**
 * Production-grade PostgreSQL connection and pool configuration (INFRA-02 Step 03).
 *
 * Implements 12-factor configuration, fail-fast startup validation, secret redaction,
 * and connection pool tuning parameters.
 */
data class PostgresConnectionConfig(
    val host: String = System.getenv("DATABASE_HOST") ?: "localhost",
    val port: Int = System.getenv("DATABASE_PORT")?.toIntOrNull() ?: 5432,
    val database: String = System.getenv("DATABASE_NAME") ?: "sucharu_pro_db",
    val user: String = System.getenv("DATABASE_USER") ?: "sucharu_app",
    val password: String = System.getenv("DATABASE_PASSWORD") ?: "",
    val maxPoolSize: Int = System.getenv("DATABASE_POOL_SIZE")?.toIntOrNull() ?: 10,
    val minIdleConnections: Int = System.getenv("DATABASE_MIN_IDLE")?.toIntOrNull() ?: 2,
    val connectionTimeoutMs: Long = System.getenv("DATABASE_CONN_TIMEOUT_MS")?.toLongOrNull() ?: 30000L,
    val idleTimeoutMs: Long = System.getenv("DATABASE_IDLE_TIMEOUT_MS")?.toLongOrNull() ?: 600000L,
    val maxLifetimeMs: Long = System.getenv("DATABASE_MAX_LIFETIME_MS")?.toLongOrNull() ?: 1800000L,
    val leakDetectionThresholdMs: Long = System.getenv("DATABASE_LEAK_DETECTION_MS")?.toLongOrNull() ?: 10000L,
    val sslMode: String = System.getenv("DATABASE_SSL_MODE") ?: "prefer",
    val poolName: String = "SucharuProPostgresPool"
) {
    /**
     * Builds JDBC URL with explicit SSL mode parameter.
     */
    fun toJdbcUrl(): String {
        return "jdbc:postgresql://$host:$port/$database?sslmode=$sslMode"
    }

    /**
     * Produces a sanitized copy for safe logging with credentials redacted.
     */
    fun toSafeString(): String {
        return "PostgresConnectionConfig(host='$host', port=$port, database='$database', user='$user', " +
                "password='[REDACTED]', maxPoolSize=$maxPoolSize, minIdle=$minIdleConnections, " +
                "connectionTimeoutMs=$connectionTimeoutMs, idleTimeoutMs=$idleTimeoutMs, " +
                "maxLifetimeMs=$maxLifetimeMs, sslMode='$sslMode', poolName='$poolName')"
    }

    /**
     * Validates configuration for production readiness, failing fast if mandatory parameters are missing or invalid.
     */
    fun validateForProduction(): List<String> {
        val errors = mutableListOf<String>()
        if (host.isBlank()) errors.add("DATABASE_HOST cannot be blank.")
        if (port !in 1..65535) errors.add("DATABASE_PORT must be between 1 and 65535 (was $port).")
        if (database.isBlank()) errors.add("DATABASE_NAME cannot be blank.")
        if (user.isBlank()) errors.add("DATABASE_USER cannot be blank.")
        if (password.isBlank()) errors.add("DATABASE_PASSWORD cannot be blank in production.")
        if (maxPoolSize < 1) errors.add("DATABASE_POOL_SIZE must be at least 1 (was $maxPoolSize).")
        if (minIdleConnections < 0) errors.add("DATABASE_MIN_IDLE cannot be negative (was $minIdleConnections).")
        if (minIdleConnections > maxPoolSize) errors.add("DATABASE_MIN_IDLE ($minIdleConnections) cannot exceed maxPoolSize ($maxPoolSize).")
        if (connectionTimeoutMs < 1000) errors.add("DATABASE_CONN_TIMEOUT_MS must be at least 1000ms (was $connectionTimeoutMs).")
        return errors
    }

    companion object {
        fun fromUrl(
            jdbcUrl: String,
            user: String = "postgres",
            password: String = "postgres",
            maxPoolSize: Int = 10
        ): PostgresConnectionConfig {
            return PostgresConnectionConfig(
                host = "custom",
                port = 5432,
                database = "custom",
                user = user,
                password = password,
                maxPoolSize = maxPoolSize
            )
        }
    }
}
