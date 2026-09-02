package com.sucharu.sucharupro.backend.config

import com.sucharu.sucharupro.data.observability.model.ReleaseMetadata

/**
 * Execution environment classification.
 */
enum class BackendEnvironment {
    DEVELOPMENT,
    TEST,
    PRODUCTION;

    companion object {
        fun fromString(value: String?): BackendEnvironment {
            return when (value?.trim()?.uppercase()) {
                "PROD", "PRODUCTION" -> PRODUCTION
                "TEST", "STAGING" -> TEST
                else -> DEVELOPMENT
            }
        }
    }
}

/**
 * Flyway migration execution policy at backend startup.
 */
enum class MigrationMode {
    AUTO_APPLY,
    VALIDATE_ONLY,
    DISABLED;

    companion object {
        fun fromString(value: String?): MigrationMode {
            return when (value?.trim()?.uppercase()) {
                "VALIDATE", "VALIDATE_ONLY" -> VALIDATE_ONLY
                "NONE", "DISABLED" -> DISABLED
                else -> AUTO_APPLY
            }
        }
    }
}

/**
 * Immutable typed configuration for the standalone Sucharu Pro backend runtime (INFRA-05 Step 01-07).
 * Guarantees zero hardcoded credentials, environment-driven injection, and fail-fast validation in PRODUCTION.
 *
 * Supported Configuration Categories:
 * - RELEASE: appName, appVersion, buildVersion, gitRevision, buildTimestamp
 * - SERVER: serverHost, serverPort, environment
 * - DATABASE: databaseUrl, databaseUser, databasePassword, databasePoolSize, databaseMinIdle, databaseConnTimeoutMs
 * - REDIS: redisEnabled, redisUrl, redisTimeoutMs
 * - WORKER: workerPoolSize, workerPollIntervalMs, workerLeaseDurationMs
 * - INTEGRATION: integrationConnectTimeoutMs, integrationReadTimeoutMs
 * - WEBHOOK: webhookReplayMaxAgeSeconds
 * - OBSERVABILITY: observabilityEnabled, metricsEnabled, metricsEndpointEnabled, slowRequestThresholdMs, healthCheckTimeoutMs, metricsOperationalAccessRequired
 * - SECURITY: jwtSigningSecret, jwtIssuer, jwtAudience, accessTokenTtlSeconds, refreshTokenTtlSeconds
 * - FLYWAY: flywayEnabled, migrationMode
 * - SHUTDOWN: gracefulShutdownTimeoutMs
 */
data class BackendConfig(
    // RELEASE
    val appName: String = "sucharu-backend",
    val appVersion: String = "1.0.0",
    val buildVersion: String = "1.0.0-PROD",
    val gitRevision: String = "HEAD",
    val buildTimestamp: String = "2026-08-25T00:00:00Z",

    // SERVER
    val serverHost: String = "0.0.0.0",
    val serverPort: Int = 8080,
    val environment: BackendEnvironment = BackendEnvironment.DEVELOPMENT,

    // DATABASE
    val databaseUrl: String = "jdbc:postgresql://localhost:5432/sucharu_pro",
    val databaseUser: String = "sucharu_app",
    val databasePassword: String = "",
    val databasePoolSize: Int = 10,
    val databaseMinIdle: Int = 2,
    val databaseConnTimeoutMs: Long = 30000L,

    // REDIS
    val redisEnabled: Boolean = false,
    val redisUrl: String? = null,
    val redisTimeoutMs: Long = 2000L,

    // WORKER
    val workerPoolSize: Int = 5,
    val workerPollIntervalMs: Long = 2000L,
    val workerLeaseDurationMs: Long = 30000L,

    // INTEGRATION & WEBHOOK
    val integrationConnectTimeoutMs: Long = 5000L,
    val integrationReadTimeoutMs: Long = 10000L,
    val webhookReplayMaxAgeSeconds: Long = 300L,

    // OBSERVABILITY
    val logLevel: String = "INFO",
    val observabilityEnabled: Boolean = true,
    val metricsEnabled: Boolean = true,
    val metricsEndpointEnabled: Boolean = true,
    val slowRequestThresholdMs: Long = 1000L,
    val healthCheckTimeoutMs: Long = 2000L,
    val metricsOperationalAccessRequired: Boolean = false,

    // SECURITY
    val jwtSigningSecret: String = "",
    val jwtIssuer: String = "sucharu-backend-server",
    val jwtAudience: String = "sucharu-api-clients",
    val accessTokenTtlSeconds: Long = 900L,
    val refreshTokenTtlSeconds: Long = 604800L,

    // FLYWAY
    val flywayEnabled: Boolean = true,
    val migrationMode: MigrationMode = MigrationMode.AUTO_APPLY,

    // SHUTDOWN
    val gracefulShutdownTimeoutMs: Long = 5000L
) {

    fun getReleaseMetadata(): ReleaseMetadata = ReleaseMetadata(
        appName = appName,
        appVersion = appVersion,
        buildVersion = buildVersion,
        gitRevision = gitRevision,
        environment = environment.name.lowercase(),
        buildTimestamp = buildTimestamp
    )

    companion object {
        fun fromEnvironment(): BackendConfig {
            val env = BackendEnvironment.fromString(System.getenv("ENVIRONMENT") ?: System.getenv("APP_ENV"))
            val port = System.getenv("PORT")?.toIntOrNull() 
                ?: System.getenv("SERVER_PORT")?.toIntOrNull() 
                ?: 8080

            val dbHost = System.getenv("DATABASE_HOST") ?: "localhost"
            val dbPort = System.getenv("DATABASE_PORT") ?: "5432"
            val dbName = System.getenv("DATABASE_NAME") ?: "sucharu_pro"
            val defaultUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
            val dbUrl = System.getenv("DATABASE_URL") ?: defaultUrl

            return BackendConfig(
                appName = System.getenv("APP_NAME") ?: "sucharu-backend",
                appVersion = System.getenv("APP_VERSION") ?: "1.0.0",
                buildVersion = System.getenv("BUILD_VERSION") ?: "${System.getenv("APP_VERSION") ?: "1.0.0"}-${env.name}",
                gitRevision = System.getenv("GIT_COMMIT") ?: System.getenv("BUILD_REVISION") ?: "HEAD",
                buildTimestamp = System.getenv("BUILD_TIMESTAMP") ?: "2026-08-25T00:00:00Z",
                serverHost = System.getenv("SERVER_HOST") ?: "0.0.0.0",
                serverPort = port,
                environment = env,
                databaseUrl = dbUrl,
                databaseUser = System.getenv("DATABASE_USER") ?: "sucharu_app",
                databasePassword = System.getenv("DATABASE_PASSWORD") ?: "",
                databasePoolSize = System.getenv("DATABASE_POOL_SIZE")?.toIntOrNull() ?: 10,
                databaseMinIdle = System.getenv("DATABASE_MIN_IDLE")?.toIntOrNull() ?: 2,
                databaseConnTimeoutMs = System.getenv("DATABASE_CONN_TIMEOUT_MS")?.toLongOrNull() ?: 30000L,
                redisEnabled = System.getenv("REDIS_ENABLED")?.toBooleanStrictOrNull() ?: false,
                redisUrl = System.getenv("REDIS_URL"),
                redisTimeoutMs = System.getenv("REDIS_TIMEOUT_MS")?.toLongOrNull() ?: 2000L,
                workerPoolSize = System.getenv("WORKER_POOL_SIZE")?.toIntOrNull() ?: 5,
                workerPollIntervalMs = System.getenv("WORKER_POLL_INTERVAL_MS")?.toLongOrNull() ?: 2000L,
                workerLeaseDurationMs = System.getenv("WORKER_LEASE_DURATION_MS")?.toLongOrNull() ?: 30000L,
                integrationConnectTimeoutMs = System.getenv("INTEGRATION_CONNECT_TIMEOUT_MS")?.toLongOrNull() ?: 5000L,
                integrationReadTimeoutMs = System.getenv("INTEGRATION_READ_TIMEOUT_MS")?.toLongOrNull() ?: 10000L,
                webhookReplayMaxAgeSeconds = System.getenv("WEBHOOK_REPLAY_MAX_AGE_SECONDS")?.toLongOrNull() ?: 300L,
                logLevel = System.getenv("LOG_LEVEL") ?: "INFO",
                observabilityEnabled = System.getenv("OBSERVABILITY_ENABLED")?.toBooleanStrictOrNull() ?: true,
                metricsEnabled = System.getenv("METRICS_ENABLED")?.toBooleanStrictOrNull() ?: true,
                metricsEndpointEnabled = System.getenv("METRICS_ENDPOINT_ENABLED")?.toBooleanStrictOrNull() ?: true,
                slowRequestThresholdMs = System.getenv("SLOW_REQUEST_THRESHOLD_MS")?.toLongOrNull() ?: 1000L,
                healthCheckTimeoutMs = System.getenv("HEALTH_CHECK_TIMEOUT_MS")?.toLongOrNull() ?: 2000L,
                metricsOperationalAccessRequired = System.getenv("METRICS_AUTH_REQUIRED")?.toBooleanStrictOrNull() ?: false,
                jwtSigningSecret = System.getenv("JWT_SIGNING_SECRET") ?: "",
                jwtIssuer = System.getenv("JWT_ISSUER") ?: "sucharu-backend-server",
                jwtAudience = System.getenv("JWT_AUDIENCE") ?: "sucharu-api-clients",
                accessTokenTtlSeconds = System.getenv("ACCESS_TOKEN_TTL_SECONDS")?.toLongOrNull() ?: 900L,
                refreshTokenTtlSeconds = System.getenv("REFRESH_TOKEN_TTL_SECONDS")?.toLongOrNull() ?: 604800L,
                flywayEnabled = System.getenv("FLYWAY_ENABLED")?.toBooleanStrictOrNull() ?: true,
                migrationMode = MigrationMode.fromString(System.getenv("MIGRATION_MODE")),
                gracefulShutdownTimeoutMs = System.getenv("GRACEFUL_SHUTDOWN_TIMEOUT_MS")?.toLongOrNull() ?: 5000L
            )
        }
    }

    /**
     * Strict production validation. Fails fast with clear actionable errors if required
     * credentials, URLs, or signing secrets are missing or insecure. Zero secret leakage.
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (serverPort <= 0 || serverPort > 65535) {
            errors.add("Invalid server port: $serverPort. Must be between 1 and 65535.")
        }
        if (databasePoolSize <= 0) {
            errors.add("Database pool size must be at least 1 (got $databasePoolSize).")
        }
        if (workerPoolSize <= 0) {
            errors.add("Worker pool size must be at least 1 (got $workerPoolSize).")
        }
        if (workerPollIntervalMs <= 0) {
            errors.add("Worker poll interval must be greater than 0 ms (got $workerPollIntervalMs).")
        }
        if (workerLeaseDurationMs <= 0) {
            errors.add("Worker lease duration must be greater than 0 ms (got $workerLeaseDurationMs).")
        }
        if (healthCheckTimeoutMs <= 0) {
            errors.add("Health check timeout must be greater than 0 ms (got $healthCheckTimeoutMs).")
        }
        if (slowRequestThresholdMs <= 0) {
            errors.add("Slow request threshold must be greater than 0 ms (got $slowRequestThresholdMs).")
        }
        if (gracefulShutdownTimeoutMs <= 0) {
            errors.add("Graceful shutdown timeout must be greater than 0 ms (got $gracefulShutdownTimeoutMs).")
        }

        if (environment == BackendEnvironment.PRODUCTION) {
            if (databasePassword.isBlank()) {
                errors.add("DATABASE_PASSWORD is required in PRODUCTION.")
            }
            if (jwtSigningSecret.isBlank() || jwtSigningSecret.length < 32 || jwtSigningSecret.contains("dev") || jwtSigningSecret.contains("development") || jwtSigningSecret.contains("fallback")) {
                errors.add("JWT_SIGNING_SECRET must be at least 32 characters long and not use development/fallback defaults in PRODUCTION.")
            }
            if (databaseUrl.contains("localhost") && !databaseUrl.contains("allow_localhost_prod")) {
                errors.add("DATABASE_URL cannot point to localhost in PRODUCTION unless explicitly allowed via 'allow_localhost_prod'.")
            }
            if (redisEnabled && redisUrl.isNullOrBlank()) {
                errors.add("REDIS_URL must be specified when REDIS_ENABLED=true in PRODUCTION.")
            }
        }

        return errors
    }

    fun toSafeString(): String {
        return "BackendConfig(appName='$appName', version='$appVersion', build='$buildVersion', git='$gitRevision', host='$serverHost', port=$serverPort, env=$environment, dbUrl='$databaseUrl', dbUser='$databaseUser', dbPassword=[REDACTED], poolSize=$databasePoolSize, redisEnabled=$redisEnabled, workerPoolSize=$workerPoolSize, jwtSecret=[REDACTED], jwtIssuer='$jwtIssuer', jwtAudience='$jwtAudience', flywayEnabled=$flywayEnabled, migrationMode=$migrationMode, logLevel='$logLevel', observabilityEnabled=$observabilityEnabled, metricsEnabled=$metricsEnabled, shutdownTimeoutMs=$gracefulShutdownTimeoutMs)"
    }
}
