package com.sucharu.sucharupro.backend.customerportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.persistence.postgres.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.sql.DriverManager

/**
 * Real PostgreSQL Database, Flyway, and Security Integration Test Suite for Customer Portal (Phase 09.1).
 *
 * MANDATORY REQUIREMENT:
 * - NO fake data sources or mock JDBC proxies in this real DB path.
 * - NO silent skips (`if (!postgresAvailable) return` is FORBIDDEN).
 * - NO localhost / DATABASE_* environment variable connection fallbacks.
 * - STRICT assertions on isolation responses: 403 / 404 ONLY (NEVER 200, NEVER 401 for valid tokens).
 */
class CustomerPortalPostgresSecurityIntegrationTest {

    companion object {
        private var containerException: Throwable? = null

        val postgresContainer: PostgreSQLContainer<*>? = try {
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("sucharu_pro_db")
                withUsername("postgres")
                withPassword("postgres")
                start()
            }
        } catch (t: Throwable) {
            containerException = t
            null
        }
    }

    private lateinit var config: PostgresConnectionConfig
    private lateinit var connectionProvider: DefaultPostgresConnectionProvider
    private lateinit var transactionManager: DefaultPostgresTransactionManager
    private lateinit var router: BackendRouter
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var useCases: BackendUseCases
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val tenantA = "TENANT-001"
    private val tenantB = "TENANT-002"
    private val customerA = "CUST-PORTAL-A"
    private val customerB = "CUST-PORTAL-B"
    private val customerC = "CUST-PORTAL-C"

    private lateinit var customerAToken: String
    private lateinit var customerBToken: String
    private lateinit var customerCToken: String
    private lateinit var vendorToken: String

    @Before
    fun setUp() {
        val container = postgresContainer
        if (container == null) {
            fail("MANDATORY REAL POSTGRESQL TESTCONTAINER FAILED TO START: ${containerException?.message ?: "Container unavailable"}")
            return
        }

        val jdbcUrl = container.jdbcUrl
        val user = container.username
        val password = container.password
        val host = container.host
        val port = container.firstMappedPort
        val dbName = container.databaseName

        config = PostgresConnectionConfig(
            host = host,
            port = port,
            database = dbName,
            user = user,
            password = password,
            maxPoolSize = 10
        )

        try {
            Class.forName("org.postgresql.Driver")
            val conn = DriverManager.getConnection(jdbcUrl, user, password)
            conn.close()

            // Run Flyway migrations against clean PostgreSQL container (WITHOUT baselineOnMigrate)
            val flyway = Flyway.configure()
                .dataSource(jdbcUrl, user, password)
                .locations("classpath:db/migration")
                .table("flyway_schema_history")
                .load()
            flyway.migrate()

            connectionProvider = DefaultPostgresConnectionProvider(config, jdbcUrl)
            transactionManager = DefaultPostgresTransactionManager(connectionProvider)

            val repositoryFactory = PostgresRepositoryFactory(transactionManager, tenantA)
            useCases = BackendUseCases(transactionManager, repositoryFactory)

            val authConfig = AuthConfig(
                jwtSigningSecret = "test_signing_secret_for_customer_portal_postgres_2026",
                jwtIssuer = "sucharu-test",
                jwtAudience = "sucharu-api"
            )
            jwtTokenProvider = JwtTokenProvider(authConfig)
            securityContext = BackendSecurityContext(jwtTokenProvider)

            customerAToken = jwtTokenProvider.generateAccessToken(
                principal = AuthenticatedPrincipal(
                    userId = customerA,
                    projectId = tenantA,
                    username = "customer_a",
                    role = UserRole.CUSTOMER
                )
            )

            customerBToken = jwtTokenProvider.generateAccessToken(
                principal = AuthenticatedPrincipal(
                    userId = customerB,
                    projectId = tenantA,
                    username = "customer_b",
                    role = UserRole.CUSTOMER
                )
            )

            customerCToken = jwtTokenProvider.generateAccessToken(
                principal = AuthenticatedPrincipal(
                    userId = customerC,
                    projectId = tenantB,
                    username = "customer_c",
                    role = UserRole.CUSTOMER
                )
            )

            vendorToken = jwtTokenProvider.generateAccessToken(
                principal = AuthenticatedPrincipal(
                    userId = "VEND-001",
                    projectId = tenantA,
                    username = "vendor_user",
                    role = UserRole.VENDOR
                )
            )

            val healthChecker = DatabaseHealthChecker(connectionProvider)
            router = BackendRouter(securityContext, useCases, healthChecker)

            // Seed real PostgreSQL database with Customer A & B (Tenant A) and Customer C (Tenant B)
            runBlocking {
                val customerDsA = PostgresCustomerDataSource(transactionManager, tenantA)
                val accountDsA = PostgresCustomerFinancialAccountDataSource(transactionManager, tenantA)

                customerDsA.insertCustomer(
                    Customer(
                        customerId = customerA,
                        customerCode = "CUS-PORTAL-A",
                        displayName = "Portal Customer Alpha",
                        primaryPhone = "+8801700000001",
                        customerType = CustomerType.BUSINESS,
                        status = CustomerStatusType.ACTIVE,
                        createdAt = "2026-08-29T00:00:00Z",
                        updatedAt = "2026-08-29T00:00:00Z"
                    )
                )

                customerDsA.insertCustomer(
                    Customer(
                        customerId = customerB,
                        customerCode = "CUS-PORTAL-B",
                        displayName = "Portal Customer Beta",
                        primaryPhone = "+8801700000002",
                        customerType = CustomerType.INDIVIDUAL,
                        status = CustomerStatusType.ACTIVE,
                        createdAt = "2026-08-29T00:00:00Z",
                        updatedAt = "2026-08-29T00:00:00Z"
                    )
                )

                accountDsA.insertAccount(
                    CustomerFinancialAccount(
                        financialAccountId = "ACC-PORTAL-A",
                        tenantId = tenantA,
                        projectId = tenantA,
                        customerId = customerA,
                        accountNumber = "ACC-A-001",
                        status = CustomerFinancialAccountStatus.ACTIVE
                    )
                )

                val customerDsB = PostgresCustomerDataSource(transactionManager, tenantB)
                customerDsB.insertCustomer(
                    Customer(
                        customerId = customerC,
                        customerCode = "CUS-PORTAL-C",
                        displayName = "Portal Customer Gamma",
                        primaryPhone = "+8801700000003",
                        customerType = CustomerType.BUSINESS,
                        status = CustomerStatusType.ACTIVE,
                        createdAt = "2026-08-29T00:00:00Z",
                        updatedAt = "2026-08-29T00:00:00Z"
                    )
                )
            }
        } catch (e: Exception) {
            fail("MANDATORY REAL POSTGRESQL TESTCONTAINER INITIALIZATION FAILED at $host:$port/$dbName: ${e.message}")
        }
    }

    @Test
    fun test01_UnauthenticatedRequest_Returns401Unauthenticated() = runBlocking {
        val req = HttpRequest(method = "GET", path = "/api/v1/customer/profile")
        val res = router.handleRequest(req)
        assertEquals("Unauthenticated request must be rejected with 401", 401, res.statusCode)
    }

    @Test
    fun test02_InvalidOrExpiredToken_Returns401Unauthenticated() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer invalid_expired_forged_token_12345")
        )
        val res = router.handleRequest(req)
        assertEquals("Invalid/expired token request must be rejected with 401", 401, res.statusCode)
    }

    @Test
    fun test03_ValidCustomerToken_AccessesOwnProfile() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $customerAToken")
        )
        val res = router.handleRequest(req)
        assertEquals(200, res.statusCode)
        assertTrue(res.body is ApiSuccessResponse<*>)
        val profile = (res.body as ApiSuccessResponse<*>).data as CustomerProfileDto
        assertEquals(customerA, profile.customerId)
        assertEquals("Portal Customer Alpha", profile.name)
    }

    @Test
    fun test04_SameTenantForeignCustomerProfile_Returns403Forbidden() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customers/$customerB",
            headers = mapOf("Authorization" to "Bearer $customerAToken")
        )
        val res = router.handleRequest(req)
        assertTrue(
            "Accessing another customer profile in same tenant must be DENIED with 403 or 404 (was ${res.statusCode})",
            res.statusCode == 403 || res.statusCode == 404
        )
        assertFalse("Cross customer access must NEVER return 200 OK", res.statusCode == 200)
    }

    @Test
    fun test05_CrossTenantCustomerProfile_Returns403ForbiddenOr404NotFound() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customers/$customerC",
            headers = mapOf("Authorization" to "Bearer $customerAToken")
        )
        val res = router.handleRequest(req)
        assertTrue(
            "Cross-tenant customer profile lookup must be DENIED with 403 or 404 (was ${res.statusCode})",
            res.statusCode == 403 || res.statusCode == 404
        )
        assertFalse("Cross-tenant access must NEVER return 200 OK", res.statusCode == 200)
    }

    @Test
    fun test06_VendorRole_BlockedFromCustomerPortal_Returns403Forbidden() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $vendorToken")
        )
        val res = router.handleRequest(req)
        assertEquals("Vendor role attempting to access customer profile must return 403 Forbidden", 403, res.statusCode)
    }

    @Test
    fun test07_CustomerCanAccessOwnOrders() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/orders",
            headers = mapOf("Authorization" to "Bearer $customerAToken")
        )
        val res = router.handleRequest(req)
        assertEquals(200, res.statusCode)
        assertTrue(res.body is ApiSuccessResponse<*>)
    }

    @Test
    fun test08_PostgresRlsCatalogAndPolicyVerification() = runBlocking {
        val conn = connectionProvider.acquireConnection()
        try {
            // 1. Verify Flyway schema history
            val flywayStmt = conn.prepareStatement("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")
            val flywayRs = flywayStmt.executeQuery()
            var migrationCount = 0
            if (flywayRs.next()) {
                migrationCount = flywayRs.getInt(1)
            }
            flywayRs.close()
            flywayStmt.close()
            assertTrue("Flyway migration history must contain executed migrations", migrationCount >= 1)

            // 2. Verify pg_class RLS enabled and forced
            val stmt = conn.prepareStatement(
                """
                SELECT relname, relrowsecurity, relforcerowsecurity 
                FROM pg_class 
                WHERE relname IN ('customer_invoices', 'customer_payments', 'customer_payment_allocations')
                """.trimIndent()
            )
            val rs = stmt.executeQuery()
            var count = 0
            while (rs.next()) {
                count++
                val relName = rs.getString("relname")
                val rlsEnabled = rs.getBoolean("relrowsecurity")
                val rlsForced = rs.getBoolean("relforcerowsecurity")
                assertTrue("RLS must be enabled on $relName", rlsEnabled)
                assertTrue("RLS must be forced on $relName", rlsForced)
            }
            rs.close()
            stmt.close()
            assertTrue("All 3 Phase 08 financial tables must exist in PostgreSQL catalog with RLS enabled", count >= 3)
        } finally {
            connectionProvider.releaseConnection(conn)
        }
    }

    @Test
    fun test09_PostgresRlsCrossTenantReadAndWriteIsolation() = runBlocking {
        val invoiceDsA = PostgresCustomerInvoiceDataSource(transactionManager, tenantA)
        val invoiceDsB = PostgresCustomerInvoiceDataSource(transactionManager, tenantB)

        val invoice = CustomerInvoice(
            invoiceId = "INV-RLS-PORTAL-01",
            tenantId = tenantA,
            projectId = tenantA,
            customerId = customerA,
            customerFinancialAccountId = "ACC-PORTAL-A",
            invoiceNumber = "INV-2026-PORTAL-01",
            subtotal = BigDecimal("1200.0000"),
            grandTotal = BigDecimal("1200.0000"),
            dueAmount = BigDecimal("1200.0000"),
            paidAmount = BigDecimal.ZERO,
            status = CustomerInvoiceStatus.ISSUED,
            lines = listOf(
                CustomerInvoiceLine(
                    lineId = "LINE-RLS-01",
                    invoiceId = "INV-RLS-PORTAL-01",
                    tenantId = tenantA,
                    projectId = tenantA,
                    description = "Custom Portal Printed Cards",
                    quantity = BigDecimal("1000"),
                    unitPrice = BigDecimal("1.2000"),
                    lineTotal = BigDecimal("1200.0000")
                )
            )
        )

        // Tenant A creates invoice
        val insertRes = invoiceDsA.insertInvoice(invoice)
        assertTrue("Tenant A invoice insertion must succeed", insertRes is DomainResult.Success)

        // Tenant B READ ISOLATION
        val readRes = invoiceDsB.findInvoiceById(tenantB, tenantA, invoice.invoiceId)
        assertTrue("Tenant B read attempt on Tenant A invoice must be DENIED by Postgres RLS", readRes is DomainResult.Error)

        // Tenant B WRITE ISOLATION
        val writeRes = invoiceDsB.updatePaymentBalance(
            tenantId = tenantB,
            projectId = tenantA,
            invoiceId = invoice.invoiceId,
            newPaidAmount = BigDecimal("1200.0000"),
            newDueAmount = BigDecimal.ZERO,
            newStatus = CustomerInvoiceStatus.PAID,
            actorId = "ATTACKER-B",
            expectedVersion = 1L
        )
        assertTrue("Tenant B write attempt on Tenant A invoice must be DENIED by Postgres RLS", writeRes is DomainResult.Error)
    }
}
