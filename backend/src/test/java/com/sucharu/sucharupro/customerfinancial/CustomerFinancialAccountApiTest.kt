package com.sucharu.sucharupro.customerfinancial

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MODULE 14 STEP 01: API & REST Routing Tests.
 */
class CustomerFinancialAccountApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var token: String

    private val projectId = "PRJ-API-01"
    private val customerId = "CUS-API-01"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF,
        permissions = setOf(UserPermission.MANAGE_CUSTOMERS, UserPermission.MANAGE_FINANCE)
    )

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        val customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        val accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val service = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createCustomerRepository(tenantId: String) = customerRepo
            override fun createCustomerFinancialAccountRepository(tenantId: String) = accountRepo
            override fun createCustomerFinancialAccountService(tenantId: String) = service
        }

        useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_cfa_api_test_2026",
            jwtIssuer = "sucharu-test",
            jwtAudience = "sucharu-api"
        )
        jwtTokenProvider = JwtTokenProvider(authConfig)
        token = jwtTokenProvider.generateAccessToken(staffPrincipal)

        val securityContext = BackendSecurityContext(jwtTokenProvider)
        val healthChecker = DatabaseHealthChecker(mockDb)

        router = BackendRouter(
            securityContext = securityContext,
            useCases = useCases,
            healthChecker = healthChecker
        )

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-API-01",
                    displayName = "API Test Client",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000099",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
        }
    }

    @Test
    fun testCreateAndGetAccountViaRouter() = runBlocking {
        val createReq = CreateCustomerFinancialAccountRequest(
            customerId = customerId,
            currency = "BDT",
            notes = "Created via REST API"
        )
        val createHttpReq = HttpRequest(
            path = "/api/v1/customer-financial-accounts",
            method = "POST",
            body = createReq,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val createResponse = router.handleRequest(createHttpReq)
        assertNotNull(createResponse)
        assertEquals(201, createResponse.statusCode)

        // Get by Customer ID
        val getByCustReq = HttpRequest(
            path = "/api/v1/customers/$customerId/financial-account",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val getByCustResponse = router.handleRequest(getByCustReq)
        assertNotNull(getByCustResponse)
        assertEquals(200, getByCustResponse.statusCode)
    }

    @Test
    fun testStatusUpdateViaRouter() = runBlocking {
        val created = useCases.createCustomerFinancialAccount(
            staffPrincipal,
            CreateCustomerFinancialAccountRequest(customerId = customerId, currency = "BDT")
        )

        val adminPrincipal = AuthenticatedPrincipal(
            userId = "admin_01",
            projectId = projectId,
            username = "admin_user",
            role = UserRole.ADMIN,
            permissions = setOf(UserPermission.MANAGE_FINANCE)
        )
        val adminToken = jwtTokenProvider.generateAccessToken(adminPrincipal)

        val statusReq = UpdateCustomerFinancialAccountStatusRequest(
            status = "SUSPENDED",
            reason = "Auditing financial records",
            expectedVersion = 1L
        )
        val statusHttpReq = HttpRequest(
            path = "/api/v1/customer-financial-accounts/${created.financialAccountId}/status",
            method = "POST",
            body = statusReq,
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val statusResponse = router.handleRequest(statusHttpReq)
        assertNotNull(statusResponse)
        assertEquals(200, statusResponse.statusCode)
    }
}
