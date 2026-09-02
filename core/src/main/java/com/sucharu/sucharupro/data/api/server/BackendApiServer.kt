package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.auth.service.UserIdentityService
import com.sucharu.sucharupro.data.observability.event.OperationalEventRecorder
import com.sucharu.sucharupro.data.observability.event.SecurityEventRecorder
import com.sucharu.sucharupro.data.observability.health.HealthRegistry
import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production-grade Backend API Server Runtime (INFRA-02 Step 04, INFRA-03 Step 01 & Step 03, INFRA-05 Step 06).
 */
class BackendApiServer(
    val connectionProvider: PostgresConnectionProvider,
    val transactionManager: TransactionManager,
    val repositoryFactory: PostgresRepositoryFactory,
    val securityContext: BackendSecurityContext = BackendSecurityContext(),
    val healthChecker: DatabaseHealthChecker = DatabaseHealthChecker(connectionProvider),
    val authService: AuthenticationService? = null,
    val userIdentityService: UserIdentityService? = null,
    val webhookIngressService: com.sucharu.sucharupro.data.integration.service.WebhookIngressService? = null,
    val metricsRegistry: ObservabilityMetricsRegistry? = null,
    val healthRegistry: HealthRegistry? = null,
    val securityEventRecorder: SecurityEventRecorder? = null,
    val operationalEventRecorder: OperationalEventRecorder? = null,
    val slowRequestThresholdMs: Long = 1000L
) : AutoCloseable {

    private val isRunning = AtomicBoolean(false)
    val useCases = BackendUseCases(transactionManager, repositoryFactory)
    val router = BackendRouter(
        securityContext,
        useCases,
        healthChecker,
        authService = authService,
        userIdentityService = userIdentityService,
        webhookIngressService = webhookIngressService,
        metricsRegistry = metricsRegistry,
        healthRegistry = healthRegistry,
        securityEventRecorder = securityEventRecorder,
        operationalEventRecorder = operationalEventRecorder,
        slowRequestThresholdMs = slowRequestThresholdMs
    )

    fun start() {
        isRunning.set(true)
    }

    fun isServerRunning(): Boolean = isRunning.get()

    suspend fun handle(request: HttpRequest): HttpResponse {
        check(isRunning.get()) { "Backend server is not running or has been shut down." }
        return router.handleRequest(request)
    }

    suspend fun shutdownGracefully(drainTimeoutMs: Long = 5000L) {
        if (isRunning.compareAndSet(true, false)) {
            connectionProvider.shutdownGracefully(drainTimeoutMs)
        }
    }

    override fun close() {
        isRunning.set(false)
        connectionProvider.close()
    }
}
