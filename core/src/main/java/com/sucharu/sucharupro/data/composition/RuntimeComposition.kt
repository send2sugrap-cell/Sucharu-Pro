package com.sucharu.sucharupro.data.composition

import com.sucharu.sucharupro.data.api.client.DemoBackendApiClient
import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.datasource.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.*
import com.sucharu.sucharupro.data.auth.service.*
import com.sucharu.sucharupro.data.auth.session.AuthenticationSessionManager
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import kotlinx.coroutines.runBlocking
import java.sql.Connection

/**
 * Global Application Runtime Modes (INFRA-01 Step 01).
 */
enum class AppRuntimeMode {
    DEVELOPMENT,
    PRODUCTION
}

/**
 * Unified Composition root for Application Shell (INFRA-01 Step 01).
 *
 * Guarantees that the Android application interacts with the backend
 * exclusively via the secure API boundary in production.
 */
interface AppRuntimeComposition {
    val mode: AppRuntimeMode
    fun createSessionManager(): AuthenticationSessionManager
}

/**
 * PostgreSQL-backed local development and feature validation composition.
 *
 * ONLY for local development, integration tests, and server-side runtimes.
 * MUST NOT be loaded by the Android application in production.
 */
class PostgresRuntimeComposition(
    private val connectionProvider: PostgresConnectionProvider,
    private val devSecret: String = "sucharu_dev_postgres_signing_secret_2026"
) : AppRuntimeComposition {

    override val mode: AppRuntimeMode = AppRuntimeMode.DEVELOPMENT

    override fun createSessionManager(): AuthenticationSessionManager {
        val transactionManager = DefaultPostgresTransactionManager(connectionProvider)
        val repositoryFactory = PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")

        val accountDs = PostgresAuthAccountDataSource(transactionManager)
        val profileDs = PostgresAuthProfileDataSource(transactionManager)
        val verifDs = PostgresAuthVerificationDataSource(transactionManager)
        val pwdHistDs = PostgresAuthPasswordHistoryDataSource(transactionManager)
        val sessionDs = PostgresAuthSessionDataSource(transactionManager)
        val auditDs = PostgresAuthAuditDataSource(transactionManager)
        
        val notifProvider = FakeVerificationNotificationProvider()

        val authConfig = AuthConfig(
            accessTokenTtlSeconds = 900L,
            refreshTokenTtlSeconds = 604800L,
            jwtIssuer = "sucharu-postgres-dev",
            jwtAudience = "sucharu-postgres-audience",
            jwtKeyId = "pg-dev-1",
            jwtSigningSecret = devSecret,
            maxLoginAttempts = 5,
            accountLockDurationSeconds = 900L
        )
        val jwtProvider = JwtTokenProvider(authConfig)

        val authService = AuthenticationService(
            accountDataSource = accountDs,
            sessionDataSource = sessionDs,
            auditDataSource = auditDs,
            profileDataSource = profileDs,
            verificationDataSource = verifDs,
            passwordHistoryDataSource = pwdHistDs,
            notificationProvider = notifProvider,
            jwtProvider = jwtProvider,
            config = authConfig
        )

        val identityService = UserIdentityService(
            accountDataSource = accountDs,
            profileDataSource = profileDs,
            verificationDataSource = verifDs,
            passwordHistoryDataSource = pwdHistDs,
            sessionDataSource = sessionDs,
            auditDataSource = auditDs
        )

        val securityContext = BackendSecurityContext(jwtTokenProvider = jwtProvider)

        val server = BackendApiServer(
            connectionProvider = connectionProvider,
            transactionManager = transactionManager,
            repositoryFactory = repositoryFactory,
            securityContext = securityContext,
            authService = authService,
            userIdentityService = identityService
        )
        server.start()

        val client = DirectBackendApiClient(server = server)
        return AuthenticationSessionManager(client = client)
    }
}

/**
 * Canonical Production Runtime Composition for Android (INFRA-05 Step 01).
 *
 * Enforces strict network isolation. Direct PostgreSQL connectivity or
 * in-process server execution is strictly prohibited in this mode.
 */
class ProductionRuntimeComposition(
    private val apiGatewayUrl: String? = System.getenv("SUCHARU_API_GATEWAY_URL")
        ?: System.getProperty("sucharu.api.gateway.url")
) : AppRuntimeComposition {

    override val mode: AppRuntimeMode = AppRuntimeMode.PRODUCTION

    /**
     * Initializes the authenticated session manager via the secure HTTPS API Gateway.
     *
     * Fails fast if the mandatory gateway URL is missing.
     * No fallback to local databases is permitted.
     */
    override fun createSessionManager(): AuthenticationSessionManager {
        val endpoint = apiGatewayUrl
        if (endpoint.isNullOrBlank()) {
            throw IllegalStateException(
                "Production composition requires a valid SUCHARU_API_GATEWAY_URL. " +
                "Direct database connection from the Android client is prohibited."
            )
        }

        // Implementation of real remote API client (Ktor/Retrofit) is scheduled for INFRA-05 Step 01.
        // This ensures the build fails if attempting to run production without the network boundary.
        throw UnsupportedOperationException(
            "Production remote API client (INFRA-05) not yet implemented. " +
            "Connect to $endpoint via a real HTTPS transport is required."
        )
    }
}

/**
 * Isolated Development Demo Runtime Composition (DEVELOPMENT ONLY).
 *
 * Provides a self-contained, in-memory client runtime for evaluating complete
 * UI/UX workflows on physical Android devices without requiring live PostgreSQL or API Gateway.
 *
 * Absolute Invariants:
 * 1. MUST NOT connect to PostgreSQL or hold DB credentials.
 * 2. MUST NOT invoke production API Gateway or live SMS services.
 * 3. MUST NOT affect or mutate production authentication accounts.
 * 4. Deterministic demo OTP: '123456' accepted ONLY inside this isolated demo runtime.
 */
class DevelopmentDemoRuntimeComposition(
    val initialRole: DemoRole = DemoRole.CUSTOMER,
    val demoTenantId: String = "TENANT-DEMO-001",
    val demoProjectId: String = "PROJECT-DEMO-001",
    val demoOtp: String = "123456"
) : AppRuntimeComposition {

    override val mode: AppRuntimeMode = AppRuntimeMode.DEVELOPMENT

    val demoClient: DemoBackendApiClient by lazy {
        DemoBackendApiClient(
            initialRole = initialRole,
            demoTenantId = demoTenantId,
            demoProjectId = demoProjectId,
            demoOtp = demoOtp
        )
    }

    override fun createSessionManager(): AuthenticationSessionManager {
        return AuthenticationSessionManager(client = demoClient)
    }
}

