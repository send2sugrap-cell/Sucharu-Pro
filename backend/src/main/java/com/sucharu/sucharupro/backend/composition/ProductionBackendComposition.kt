package com.sucharu.sucharupro.backend.composition

import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.health.ServerHealthTracker
import com.sucharu.sucharupro.backend.persistence.DatabasePoolProvider
import com.sucharu.sucharupro.backend.persistence.FlywayMigrationManager
import com.sucharu.sucharupro.backend.workers.BackgroundWorkerManager
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.*
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.auth.service.UserIdentityService
import com.sucharu.sucharupro.data.observability.event.OperationalEventRecorder
import com.sucharu.sucharupro.data.observability.event.SecurityEventRecorder
import com.sucharu.sucharupro.data.observability.health.HealthCheck
import com.sucharu.sucharupro.data.observability.health.HealthRegistry
import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import com.sucharu.sucharupro.data.observability.model.ComponentHealth
import com.sucharu.sucharupro.data.observability.model.HealthStatus
import com.sucharu.sucharupro.data.observability.model.OperationalEventType
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Authoritative Server-Side Production Composition Root for Sucharu Pro (INFRA-05 Step 01-06).
 * Fully owns PostgreSQL connection pooling, Flyway migrations, authentication authority,
 * transaction management, event store, outbox workers, background jobs, external integrations,
 * and observability / telemetry.
 */
class ProductionBackendComposition(
    val config: BackendConfig,
    val healthTracker: ServerHealthTracker = ServerHealthTracker()
) {

    private val logger = LoggerFactory.getLogger(ProductionBackendComposition::class.java)
    private val isStarted = AtomicBoolean(false)

    // 1. Persistence & Transaction Management
    val poolProvider: DatabasePoolProvider by lazy {
        DatabasePoolProvider(config)
    }

    val transactionManager: DefaultPostgresTransactionManager by lazy {
        DefaultPostgresTransactionManager(poolProvider)
    }

    val repositoryFactory: PostgresRepositoryFactory by lazy {
        PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")
    }

    val migrationManager: FlywayMigrationManager by lazy {
        FlywayMigrationManager(poolProvider.dataSource, config)
    }

    // 2. Authentication & Identity Authority
    val authConfig: AuthConfig by lazy {
        AuthConfig(
            jwtSigningSecret = config.jwtSigningSecret.ifBlank { "sucharu_backend_server_secret_fallback_key_2026" },
            jwtIssuer = config.jwtIssuer,
            jwtAudience = config.jwtAudience,
            accessTokenTtlSeconds = 900L,
            refreshTokenTtlSeconds = 604800L
        )
    }

    val jwtTokenProvider: JwtTokenProvider by lazy {
        JwtTokenProvider(authConfig)
    }

    val accountDataSource: PostgresAuthAccountDataSource by lazy {
        PostgresAuthAccountDataSource(transactionManager)
    }

    val profileDataSource: PostgresAuthProfileDataSource by lazy {
        PostgresAuthProfileDataSource(transactionManager)
    }

    val verificationDataSource: PostgresAuthVerificationDataSource by lazy {
        PostgresAuthVerificationDataSource(transactionManager)
    }

    val passwordHistoryDataSource: PostgresAuthPasswordHistoryDataSource by lazy {
        PostgresAuthPasswordHistoryDataSource(transactionManager)
    }

    val sessionDataSource: PostgresAuthSessionDataSource by lazy {
        PostgresAuthSessionDataSource(transactionManager)
    }

    val auditDataSource: PostgresAuthAuditDataSource by lazy {
        PostgresAuthAuditDataSource(transactionManager)
    }

    val notificationProvider: IVerificationNotificationProvider by lazy {
        ProductionSmsVerificationNotificationProvider()
    }

    val authenticationService: AuthenticationService by lazy {
        AuthenticationService(
            accountDataSource = accountDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource,
            jwtProvider = jwtTokenProvider,
            config = authConfig,
            profileDataSource = profileDataSource,
            verificationDataSource = verificationDataSource,
            passwordHistoryDataSource = passwordHistoryDataSource,
            notificationProvider = notificationProvider
        )
    }

    val userIdentityService: UserIdentityService by lazy {
        UserIdentityService(
            accountDataSource = accountDataSource,
            profileDataSource = profileDataSource,
            verificationDataSource = verificationDataSource,
            passwordHistoryDataSource = passwordHistoryDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource
        )
    }

    // 3. Security Context, Metrics & Observability Subsystem (INFRA-05 Step 06)
    val securityContext: BackendSecurityContext by lazy {
        BackendSecurityContext(jwtTokenProvider = jwtTokenProvider)
    }

    val metricsRegistry: ObservabilityMetricsRegistry by lazy {
        ObservabilityMetricsRegistry()
    }

    val securityEventRecorder: SecurityEventRecorder by lazy {
        SecurityEventRecorder(metricsRegistry = metricsRegistry)
    }

    val operationalEventRecorder: OperationalEventRecorder by lazy {
        OperationalEventRecorder(metricsRegistry = metricsRegistry)
    }

    val healthRegistry: HealthRegistry by lazy {
        val registry = HealthRegistry(defaultTimeoutMs = config.healthCheckTimeoutMs)
        // Register Database Health Probe
        registry.register(object : HealthCheck {
            override val name = "database"
            override val isCritical = true
            override suspend fun check(): ComponentHealth {
                val healthy = try { poolProvider.isHealthy() } catch (_: Exception) { false }
                return ComponentHealth(
                    name = "database",
                    status = if (healthy) HealthStatus.UP else HealthStatus.DOWN,
                    message = if (healthy) "PostgreSQL connection pool healthy." else "Database unreachable."
                )
            }
        })
        // Register Worker Health Probe
        registry.register(object : HealthCheck {
            override val name = "worker"
            override val isCritical = false
            override suspend fun check(): ComponentHealth {
                val running = workerManager.isHealthy()
                return ComponentHealth(
                    name = "worker",
                    status = if (running) HealthStatus.UP else HealthStatus.DEGRADED,
                    message = if (running) "Background job worker pool active." else "Workers idle or stopped."
                )
            }
        })
        // Register Redis Health Probe (Optional / Non-critical acceleration)
        registry.register(com.sucharu.sucharupro.data.observability.health.RedisHealthChecker(
            redisEnabled = config.redisEnabled,
            redisUrl = config.redisUrl
        ))
        registry
    }

    // 4. Background Job & Worker Subsystem (INFRA-05 Step 04)
    val jobRepository: com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository(transactionManager)
    }

    val jobExecutionRepository: com.sucharu.sucharupro.data.job.postgres.PostgresJobExecutionRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobExecutionRepository(transactionManager)
    }

    val jobDeadLetterRepository: com.sucharu.sucharupro.data.job.postgres.PostgresJobDeadLetterRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobDeadLetterRepository(transactionManager)
    }

    val jobDependencyRepository: com.sucharu.sucharupro.data.job.postgres.PostgresJobDependencyRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobDependencyRepository(transactionManager)
    }

    val jobScheduleRepository: com.sucharu.sucharupro.data.job.postgres.PostgresJobScheduleRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobScheduleRepository(transactionManager)
    }

    val jobHandlerRegistry: com.sucharu.sucharupro.domain.job.worker.JobHandlerRegistry by lazy {
        com.sucharu.sucharupro.domain.job.worker.JobHandlerRegistry()
    }

    val jobClaimService: com.sucharu.sucharupro.data.job.worker.JobClaimService by lazy {
        com.sucharu.sucharupro.data.job.worker.JobClaimService(jobRepository)
    }

    val jobLeaseRecoveryService: com.sucharu.sucharupro.data.job.lease.JobLeaseRecoveryService by lazy {
        com.sucharu.sucharupro.data.job.lease.JobLeaseRecoveryService(jobRepository)
    }

    val jobRetryEngine: com.sucharu.sucharupro.data.job.retry.JobRetryEngine by lazy {
        com.sucharu.sucharupro.data.job.retry.JobRetryEngine()
    }

    val jobExecutionEngine: com.sucharu.sucharupro.data.job.worker.JobExecutionEngine by lazy {
        com.sucharu.sucharupro.data.job.worker.JobExecutionEngine(
            handlerRegistry = jobHandlerRegistry,
            jobRepository = jobRepository,
            executionRepository = jobExecutionRepository,
            deadLetterRepository = jobDeadLetterRepository,
            dependencyRepository = jobDependencyRepository,
            retryEngine = jobRetryEngine
        )
    }

    val jobWorker: com.sucharu.sucharupro.data.job.worker.BackgroundJobWorker by lazy {
        com.sucharu.sucharupro.data.job.worker.BackgroundJobWorker(
            concurrencyLimit = config.workerPoolSize,
            claimService = jobClaimService,
            executionEngine = jobExecutionEngine,
            leaseRecoveryService = jobLeaseRecoveryService,
            pollIntervalMs = 1000L,
            leaseDurationMs = 30000L
        )
    }

    val workerManager: BackgroundWorkerManager by lazy {
        BackgroundWorkerManager(
            jobWorker = jobWorker,
            leaseRecoveryService = jobLeaseRecoveryService,
            defaultTenants = listOf(com.sucharu.sucharupro.data.persistence.postgres.TenantContext("TENANT-001"))
        )
    }

    // 5. External Integration & Webhook Dispatch Platform (INFRA-05 Step 05)
    val integrationRepository: com.sucharu.sucharupro.data.integration.postgres.PostgresIntegrationRepository by lazy {
        com.sucharu.sucharupro.data.integration.postgres.PostgresIntegrationRepository(transactionManager)
    }

    val webhookRepository: com.sucharu.sucharupro.data.integration.postgres.PostgresWebhookRepository by lazy {
        com.sucharu.sucharupro.data.integration.postgres.PostgresWebhookRepository(transactionManager)
    }

    val integrationAuditRepository: com.sucharu.sucharupro.data.integration.postgres.PostgresIntegrationAuditRepository by lazy {
        com.sucharu.sucharupro.data.integration.postgres.PostgresIntegrationAuditRepository(transactionManager)
    }

    val ssrfValidator: com.sucharu.sucharupro.data.integration.security.SsrfProtectionValidator by lazy {
        com.sucharu.sucharupro.data.integration.security.SsrfProtectionValidator()
    }

    val integrationSecretProvider: com.sucharu.sucharupro.data.integration.security.DefaultIntegrationSecretProvider by lazy {
        com.sucharu.sucharupro.data.integration.security.DefaultIntegrationSecretProvider()
    }

    val webhookSignatureVerifier: com.sucharu.sucharupro.data.integration.security.HmacSha256SignatureVerifier by lazy {
        com.sucharu.sucharupro.data.integration.security.HmacSha256SignatureVerifier()
    }

    val integrationHttpClient: com.sucharu.sucharupro.data.integration.client.DefaultIntegrationHttpClient by lazy {
        com.sucharu.sucharupro.data.integration.client.DefaultIntegrationHttpClient(ssrfValidator = ssrfValidator)
    }

    val circuitBreakerRegistry: com.sucharu.sucharupro.data.integration.resilience.IntegrationCircuitBreakerRegistry by lazy {
        com.sucharu.sucharupro.data.integration.resilience.IntegrationCircuitBreakerRegistry()
    }

    val rateLimiterRegistry: com.sucharu.sucharupro.data.integration.resilience.IntegrationRateLimiterRegistry by lazy {
        com.sucharu.sucharupro.data.integration.resilience.IntegrationRateLimiterRegistry()
    }

    val webhookIngressService: com.sucharu.sucharupro.data.integration.service.WebhookIngressService by lazy {
        com.sucharu.sucharupro.data.integration.service.WebhookIngressService(
            integrationRepository = integrationRepository,
            webhookRepository = webhookRepository,
            auditRepository = integrationAuditRepository,
            secretProvider = integrationSecretProvider,
            signatureVerifier = webhookSignatureVerifier,
            jobRepository = jobRepository
        )
    }

    // 6. API Dispatcher & Server Facade (Wired with Observability)
    val apiServer: BackendApiServer by lazy {
        BackendApiServer(
            connectionProvider = poolProvider,
            transactionManager = transactionManager,
            repositoryFactory = repositoryFactory,
            securityContext = securityContext,
            authService = authenticationService,
            userIdentityService = userIdentityService,
            webhookIngressService = webhookIngressService,
            metricsRegistry = metricsRegistry,
            healthRegistry = healthRegistry,
            securityEventRecorder = securityEventRecorder,
            operationalEventRecorder = operationalEventRecorder,
            slowRequestThresholdMs = config.slowRequestThresholdMs
        )
    }

    fun start() {
        if (isStarted.compareAndSet(false, true)) {
            logger.info("=================================================================")
            logger.info("SUCHARU PRO STANDALONE BACKEND RUNTIME INITIALIZING")
            logger.info("Environment: {}", config.environment)
            logger.info("Configuration: {}", config.toSafeString())
            logger.info("=================================================================")

            // Step 1: Validate configuration
            val validationErrors = config.validate()
            if (validationErrors.isNotEmpty()) {
                val errorMsg = "Fatal configuration error(s):\n" + validationErrors.joinToString("\n - ", prefix = " - ")
                logger.error(errorMsg)
                throw IllegalStateException(errorMsg)
            }
            healthTracker.markApplicationStarted(true)

            // Step 2: Database Connection Pool verification
            try {
                val healthy = poolProvider.isHealthy()
                if (!healthy) {
                    logger.warn("Initial database probe returned false (database might be starting up)")
                }
                healthTracker.markDatabaseReady(true)
            } catch (e: Exception) {
                logger.error("Failed to establish initial database connection", e)
                if (config.environment == com.sucharu.sucharupro.backend.config.BackendEnvironment.PRODUCTION) {
                    throw e
                }
            }

            // Step 3: Flyway schema migrations
            if (config.flywayEnabled && config.migrationMode != com.sucharu.sucharupro.backend.config.MigrationMode.DISABLED) {
                try {
                    val migrated = migrationManager.runMigrations()
                    healthTracker.markMigrationsValid(migrated)
                } catch (e: Exception) {
                    logger.error("Database migration error", e)
                    healthTracker.markMigrationsValid(false)
                    if (config.environment == com.sucharu.sucharupro.backend.config.BackendEnvironment.PRODUCTION) {
                        throw e
                    }
                }
            } else {
                logger.info("Flyway migrations skipped (flywayEnabled={}, mode={})", config.flywayEnabled, config.migrationMode)
                healthTracker.markMigrationsValid(true)
            }

            // Step 4: Core dependencies initialization
            apiServer.start()
            healthTracker.markCoreDependenciesReady(true)

            // Step 5: Background worker startup
            workerManager.start()
            healthTracker.markWorkersReady(true)

            // Emit Operational Event
            operationalEventRecorder.recordEvent(
                eventType = OperationalEventType.SERVER_STARTED,
                correlationId = "boot-0",
                component = "ProductionBackendComposition",
                summary = "Sucharu Pro backend runtime started successfully in ${config.environment} mode."
            )

            logger.info("=================================================================")
            logger.info("SUCHARU PRO BACKEND RUNTIME READY (Health: {})", healthTracker.getHealthReport()["status"])
            logger.info("=================================================================")
        }
    }

    fun stop() {
        if (isStarted.compareAndSet(true, false)) {
            logger.info("Shutting down Sucharu Pro Standalone Backend...")
            try {
                operationalEventRecorder.recordEvent(
                    eventType = OperationalEventType.SERVER_STOPPING,
                    correlationId = "shutdown-0",
                    component = "ProductionBackendComposition",
                    summary = "Initiating graceful shutdown of backend runtime."
                )

                workerManager.stop()
                healthTracker.markWorkersReady(false)

                apiServer.close()
                healthTracker.markCoreDependenciesReady(false)

                poolProvider.close()
                healthTracker.markDatabaseReady(false)
                healthTracker.markApplicationStarted(false)

                logger.info("Sucharu Pro Standalone Backend shutdown complete.")
            } catch (e: Exception) {
                logger.error("Error during backend shutdown", e)
            }
        }
    }
}
