package com.sucharu.sucharupro.data.composition

import com.sucharu.sucharupro.data.api.client.*
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
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionConfig
import kotlinx.coroutines.runBlocking
import java.sql.Connection

import com.sucharu.sucharupro.data.persistence.postgres.PostgresAffiliateDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresCustomerDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresOrderDataSource
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.HttpAffiliateRepository
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.DashboardRepository
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository

/**
 * Global Application Runtime Modes (INFRA-01 Step 01).
 */
enum class AppRuntimeMode {
    DEVELOPMENT,
    PRODUCTION
}

/**
 * Unified Composition root for Application Shell (INFRA-01 Step 01 & INFRA-05 Step 03).
 *
 * Guarantees that the Android application interacts with the backend
 * exclusively via the secure API boundary in production.
 */
interface AppRuntimeComposition {
    val mode: AppRuntimeMode
    fun createSessionManager(): AuthenticationSessionManager
    fun createAuthenticationProvider(): com.sucharu.sucharupro.data.auth.provider.AuthenticationProvider

    val customerRepository: CustomerRepository
    val orderRepository: OrderRepository
    val affiliateRepository: AffiliateRepository
    val dashboardRepository: DashboardRepository
    val printingCalculatorService: com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorService
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
            config = authConfig,
            firebaseTokenVerifier = FirebaseTokenVerifier()
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

    override fun createAuthenticationProvider(): com.sucharu.sucharupro.data.auth.provider.AuthenticationProvider {
        throw IllegalStateException(
            "PostgresRuntimeComposition does not supply an AuthenticationProvider. " +
            "This composition is for server-side and integration test use only."
        )
    }

    override val customerRepository: CustomerRepository by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        CustomerRepositoryImpl(PostgresCustomerDataSource(tm, "TENANT-001"))
    }

    override val orderRepository: OrderRepository by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        OrderRepositoryImpl(PostgresOrderDataSource(tm, "TENANT-001"))
    }

    override val affiliateRepository: AffiliateRepository by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        AffiliateRepositoryImpl(PostgresAffiliateDataSource(tm))
    }

    override val dashboardRepository: DashboardRepository by lazy {
        FakeDashboardRepository()
    }

    override val printingCalculatorService: com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorServiceImpl(
            com.sucharu.sucharupro.data.repository.printingcalculator.PrintingCalculatorRepositoryImpl(
                com.sucharu.sucharupro.data.persistence.postgres.PostgresPrintingCalculatorDataSource(tm)
            )
        )
    }
}

/**
 * Canonical Production Runtime Composition for Android (INFRA-05 Step 01 & Step 03).
 *
 * Enforces strict network isolation. Direct PostgreSQL connectivity or
 * in-process server execution is strictly prohibited in this mode.
 */
class ProductionRuntimeComposition(
    private val apiGatewayUrl: String? = System.getenv("SUCHARU_API_GATEWAY_URL")
        ?: System.getProperty("sucharu.api.gateway.url")
        ?: "http://192.168.1.102:8080", // Local PC Server Default for Mobile Demo
    private val tokenStorage: AuthTokenStorage = InMemoryAuthTokenStorage(),
    private val authenticationProvider: com.sucharu.sucharupro.data.auth.provider.AuthenticationProvider? = null
) : AppRuntimeComposition {

    override val mode: AppRuntimeMode = AppRuntimeMode.PRODUCTION

    val client: BackendApiClient by lazy {
        val endpoint = apiGatewayUrl
        if (endpoint.isNullOrBlank()) {
            throw IllegalStateException(
                "Production composition requires a valid SUCHARU_API_GATEWAY_URL. " +
                "Direct database connection from the Android client is prohibited."
            )
        }
        HttpBackendApiClient(baseUrl = endpoint, tokenStorage = tokenStorage)
    }

    override fun createSessionManager(): AuthenticationSessionManager {
        return AuthenticationSessionManager(client = client)
    }

    override fun createAuthenticationProvider(): com.sucharu.sucharupro.data.auth.provider.AuthenticationProvider {
        return authenticationProvider
            ?: throw IllegalStateException(
                "ProductionRuntimeComposition requires a concrete AuthenticationProvider."
            )
    }

    override val customerRepository: CustomerRepository by lazy {
        HttpCustomerRepository(client = client)
    }

    override val orderRepository: OrderRepository by lazy {
        HttpOrderRepository(client = client)
    }

    override val affiliateRepository: AffiliateRepository by lazy {
        HttpAffiliateRepository(client = client)
    }

    override val dashboardRepository: DashboardRepository by lazy {
        HttpDashboardRepository(client = client)
    }

    override val printingCalculatorService: com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorService by lazy {
        com.sucharu.sucharupro.data.repository.printingcalculator.HttpPrintingCalculatorService(client = client)
    }
}
