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
    val machineRegistryRepository: com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
    val machineRegistryService: com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
    val machineTelemetryIngestionService: com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionService
    val machineStatusMonitoringService: com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringService
    val machineMaintenanceService: com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceService
    val machineFaultEventService: com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventService
    val machineDowntimeService: com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeService
    val machineAlertService: com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertService
    val machineOeeService: com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeService
    val preflightEngine: com.sucharu.sucharupro.domain.engine.preflight.PreflightEngine
    val preflightService: com.sucharu.sucharupro.domain.service.preflight.PreflightService
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

    override val machineRegistryRepository: com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl(
            com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineRegistryDataSource(tm)
        )
    }

    override val machineRegistryService: com.sucharu.sucharupro.domain.service.machine.MachineRegistryService by lazy {
        com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl(
            machineRegistryRepository
        )
    }

    override val machineTelemetryIngestionService: com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionServiceImpl(
            telemetryRepository = com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineTelemetryDataSource(tm)
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val machineStatusMonitoringService: com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringServiceImpl(
            machineRegistryRepository = machineRegistryRepository,
            machineTelemetryRepository = com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineTelemetryDataSource(tm)
            )
        )
    }

    override val machineMaintenanceService: com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceServiceImpl(
            maintenanceRepository = com.sucharu.sucharupro.data.repository.machine.maintenance.MachineMaintenanceRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineMaintenanceDataSource(tm)
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val machineFaultEventService: com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventServiceImpl(
            eventRepository = com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineEventDataSource(tm)
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val machineDowntimeService: com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeServiceImpl(
            eventRepository = com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineEventDataSource(tm)
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val machineAlertService: com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertServiceImpl(
            alertRepository = com.sucharu.sucharupro.data.repository.machine.alerts.MachineAlertRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineAlertDataSource(tm)
            ),
            machineRegistryRepository = machineRegistryRepository,
            notificationRepository = null
        )
    }

    override val machineOeeService: com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeServiceImpl(
            oeeRepository = com.sucharu.sucharupro.data.repository.machine.oee.MachineOeeRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineOeeDataSource(tm)
            ),
            machineRegistryRepository = machineRegistryRepository,
            eventRepository = com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresMachineEventDataSource(tm)
            ),
            productionExecutionRepository = null
        )
    }

    override val preflightEngine: com.sucharu.sucharupro.domain.engine.preflight.PreflightEngine by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        val factory = PostgresRepositoryFactory(tm)
        com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl(
            ruleRegistry = factory.createPreflightRuleRegistry(),
            preflightRepository = com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresPreflightDataSource(tm)
            )
        )
    }

    override val preflightService: com.sucharu.sucharupro.domain.service.preflight.PreflightService by lazy {
        val tm = DefaultPostgresTransactionManager(connectionProvider)
        com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl(
            preflightEngine = preflightEngine,
            preflightRepository = com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.persistence.postgres.PostgresPreflightDataSource(tm)
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
        ?: "http://192.168.1.100:8080", // Local PC Server Default for Mobile Demo
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

    override val machineRegistryRepository: com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository by lazy {
        com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl(
            com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource()
        )
    }

    override val machineRegistryService: com.sucharu.sucharupro.domain.service.machine.MachineRegistryService by lazy {
        com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl(
            machineRegistryRepository
        )
    }

    override val machineTelemetryIngestionService: com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionService by lazy {
        com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionServiceImpl(
            telemetryRepository = com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource()
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val machineStatusMonitoringService: com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringService by lazy {
        com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringServiceImpl(
            machineRegistryRepository = machineRegistryRepository,
            machineTelemetryRepository = com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource()
            )
        )
    }

    override val machineMaintenanceService: com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceService by lazy {
        com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceServiceImpl(
            maintenanceRepository = com.sucharu.sucharupro.data.repository.machine.maintenance.MachineMaintenanceRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.machine.maintenance.FakeMachineMaintenanceDataSource()
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val machineFaultEventService: com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventService by lazy {
        com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventServiceImpl(
            eventRepository = com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource()
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val machineDowntimeService: com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeService by lazy {
        com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeServiceImpl(
            eventRepository = com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource()
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val machineAlertService: com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertService by lazy {
        com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertServiceImpl(
            alertRepository = com.sucharu.sucharupro.data.repository.machine.alerts.MachineAlertRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.machine.alerts.FakeMachineAlertDataSource()
            ),
            machineRegistryRepository = machineRegistryRepository,
            notificationRepository = com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(
                notificationDataSource = com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource()
            )
        )
    }

    override val machineOeeService: com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeService by lazy {
        com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeServiceImpl(
            oeeRepository = com.sucharu.sucharupro.data.repository.machine.oee.MachineOeeRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.machine.oee.FakeMachineOeeDataSource()
            ),
            machineRegistryRepository = machineRegistryRepository
        )
    }

    override val preflightEngine: com.sucharu.sucharupro.domain.engine.preflight.PreflightEngine by lazy {
        val registry = com.sucharu.sucharupro.domain.preflight.PreflightRuleRegistry()
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FileExistenceRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FileNonEmptyRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FileFormatSupportedRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FileFormatMatchRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FileSignatureValidRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.DocumentParseableRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.DocumentPageCountRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.ImageStructureReadableRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.SpecPageCountMatchRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.SpecDocumentSizeMatchRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.SpecOrientationMatchRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.SpecFormatMatchRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.SpecDocumentTypeMatchRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.AssetDpiResolutionRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.AssetColorSpaceRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.AssetColorProfileRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.AssetIntegrityMissingRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.AssetIntegrityCorruptRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FontPresenceAndMissingRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FontEmbeddingStatusRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FontTypeCompatibilityRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FontSubstitutionRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.FontRequirementComplianceRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.BleedBoxReadinessRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.TrimBoxPresenceAndSizeRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.PageBoxRelationshipRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.PageGeometryConsistencyRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.SafeAreaGeometryRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.ImpositionReadinessRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.ProofReferenceIntegrityRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.ProofPageCountComparisonRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.ProofGeometryComparisonRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.ProofContentFingerprintRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.ProofVersionComparisonRule())
        registry.registerRule(com.sucharu.sucharupro.domain.preflight.rules.ProofVisualComparisonRule())
        com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl(
            ruleRegistry = registry,
            preflightRepository = com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource()
            )
        )
    }

    override val preflightService: com.sucharu.sucharupro.domain.service.preflight.PreflightService by lazy {
        com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl(
            preflightEngine = preflightEngine,
            preflightRepository = com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource()
            )
        )
    }
}
