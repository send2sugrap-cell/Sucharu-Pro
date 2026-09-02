package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.model.UnauthenticatedException
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.composition.PostgresRuntimeComposition
import com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition
import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.event.integration.n8n.N8nAutomationDispatcher
import com.sucharu.sucharupro.data.event.integration.n8n.N8nConfig
import com.sucharu.sucharupro.data.notification.InMemoryNotificationDeliveryProvider
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import java.math.BigDecimal
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement

/**
 * Production Architecture & Security Integrity Regression Tests.
 *
 * Proves that:
 * 1. Production runtime prohibits direct database connectivity.
 * 2. API Gateway URL is mandatory for production.
 * 3. Silent PostgreSQL fallbacks from the client are prevented.
 * 4. Server secrets are validated.
 * 5. Multi-tenant transaction scoping remains secure.
 */
class SecurityRemediationRegressionTest {

    @Test
    fun test01_productionSecurityContext_rejectsUnregisteredStaticTokens() {
        val securityContext = BackendSecurityContext()

        // Unregistered legacy static tokens must be rejected
        val rejectedTokens = listOf(
            "Bearer token-staff-admin",
            "Bearer token-customer-100",
            "Bearer token-customer-200",
            "Bearer token-affiliate-100",
            "Bearer token-tenant-b-customer"
        )

        for (token in rejectedTokens) {
            try {
                securityContext.authenticate(token)
                fail("Expected UnauthenticatedException for unregistered static token '$token'")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message?.contains("Invalid or expired authentication token") == true)
            }
        }
    }

    @Test
    fun test02_authConfig_validatesBlankOrShortSigningSecret() {
        val invalidConfig = AuthConfig(jwtSigningSecret = "")
        val errors = invalidConfig.validateForProduction()
        assertTrue("Blank secret must fail production validation", errors.isNotEmpty())
        assertTrue(errors.any { it.contains("at least 32 characters") })

        val shortConfig = AuthConfig(jwtSigningSecret = "short_key_12345")
        val shortErrors = shortConfig.validateForProduction()
        assertTrue(shortErrors.any { it.contains("at least 32 characters") })

        val validConfig = AuthConfig(jwtSigningSecret = "a_super_secure_signing_secret_key_that_is_long_enough_2026")
        val validErrors = validConfig.validateForProduction()
        assertTrue("Valid secret must produce no errors", validErrors.isEmpty())
    }

    @Test
    fun test03_productionRuntimeComposition_enforcesApiBoundary() {
        // 1. Verify failure when API Gateway URL is missing
        val prodCompMissingUrl = ProductionRuntimeComposition(apiGatewayUrl = null)
        try {
            prodCompMissingUrl.createSessionManager()
            fail("Expected IllegalStateException when SUCHARU_API_GATEWAY_URL is missing")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("requires a valid SUCHARU_API_GATEWAY_URL") == true)
            assertTrue(e.message?.contains("Direct database connection from the Android client is prohibited") == true)
        }

        // 2. Verify it does NOT fall back to PostgreSQL or embedded server
        val prodCompWithUrl = ProductionRuntimeComposition(apiGatewayUrl = "https://api.sucharu.com")
        try {
            prodCompWithUrl.createSessionManager()
            fail("Expected UnsupportedOperationException to prove network client implementation is required")
        } catch (e: UnsupportedOperationException) {
            assertTrue(e.message?.contains("remote API client") == true)
            assertTrue(e.message?.contains("HTTPS transport is required") == true)
        }
    }

    @Test
    fun test04_postgresRuntimeComposition_isolatedToDev() {
        val mockProvider = object : PostgresConnectionProvider {
            override suspend fun acquireConnection(): Connection = throw UnsupportedOperationException()
            override suspend fun releaseConnection(connection: Connection) {}
            override fun close() {}
        }
        
        val devComp = PostgresRuntimeComposition(
            connectionProvider = mockProvider,
            devSecret = "postgres_secret_key_32_characters_long_12345"
        )
        
        assertEquals(com.sucharu.sucharupro.data.composition.AppRuntimeMode.DEVELOPMENT, devComp.mode)
        assertNotNull(devComp.createSessionManager())
    }

    @Test
    fun test05_n8nDispatcher_withoutTransport_returnsSkipped() = runBlocking {
        val dispatcher = N8nAutomationDispatcher(
            config = N8nConfig(isEnabled = true, signingSecret = "test_signing_secret_32_characters_long"),
            transport = null
        )

        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
            projectId = "PRJ-01",
            actor = EventActor.human("U-1")
        )

        val result = dispatcher.dispatch(envelope)
        assertTrue("Missing transport must yield Skipped, not Success", result is EventConsumerResult.Skipped)
        assertFalse("Missing transport must NOT be treated as success", result.isSuccess)
    }

    @Test
    fun test06_readOnlyTransactionScoping_managesAutoCommitAndSession() = runBlocking {
        var autoCommitSet = false
        var sessionVarSet = false
        var committed = false

        val mockStmt = Proxy.newProxyInstance(
            PreparedStatement::class.java.classLoader,
            arrayOf(PreparedStatement::class.java)
        ) { _, method, args ->
            if (method.name == "setString" && args?.getOrNull(1) == "TENANT-SAFE-01") {
                sessionVarSet = true
            }
            if (method.name == "execute") true
            else null
        } as PreparedStatement

        val mockConn = Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "setAutoCommit" -> {
                    if (args?.getOrNull(0) == false) autoCommitSet = true
                    null
                }
                "prepareStatement" -> mockStmt
                "commit" -> {
                    committed = true
                    null
                }
                "isClosed" -> false
                else -> null
            }
        } as Connection

        val mockProvider = object : PostgresConnectionProvider {
            override suspend fun acquireConnection(): Connection = mockConn
            override suspend fun releaseConnection(connection: Connection) {}
            override fun close() {}
        }

        val txManager = DefaultPostgresTransactionManager(mockProvider)
        val result = txManager.inReadOnly(TenantContext("TENANT-SAFE-01")) {
            "QUERY_RESULT"
        }

        assertEquals("QUERY_RESULT", result)
        assertTrue("inReadOnly must set autoCommit=false", autoCommitSet)
        assertTrue("inReadOnly must set tenant session variable", sessionVarSet)
        assertTrue("inReadOnly must commit transaction to end scope", committed)
    }

    @Test
    fun test07_internalCommunicationRepository_doesNotDeadlockOnThreadAndReply() = runBlocking {
        val dataSource = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifDelivery = NotificationDeliveryServiceImpl(listOf(InMemoryNotificationDeliveryProvider()))
        val notifRepo = NotificationRepositoryImpl(notifDs, notifDelivery)
        val repo = InternalCommunicationRepositoryImpl(dataSource, notifRepo)

        val threadRes = repo.createThread(
            projectId = "PRJ-REG-01",
            subject = "Deadlock Regression Test",
            initialMessage = "Testing non-reentrant mutex fix",
            senderUserId = "USER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(threadRes is DomainResult.Success)
        val thread = (threadRes as DomainResult.Success).data

        val replyRes = repo.replyToThread(
            projectId = "PRJ-REG-01",
            threadId = thread.threadId,
            replyMessage = "Replying without deadlock",
            senderUserId = "USER-02",
            senderRole = UserRole.STAFF,
            actorId = "USER-02",
            callerRole = UserRole.STAFF
        )
        assertTrue(replyRes is DomainResult.Success)
    }
}
