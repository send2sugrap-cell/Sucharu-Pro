package com.sucharu.sucharupro.customerinvoice

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
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 02: API & REST Routing Tests for Customer Invoices.
 */
class CustomerInvoiceApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var token: String

    private val projectId = "PRJ-API-01"
    private val customerId = "CUS-API-01"
    private val accountId = "CFA-API-01"

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
        val invoiceDs = FakeCustomerInvoiceDataSource()
        val invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)

        val accountService = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)
        val invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createCustomerRepository(tenantId: String) = customerRepo
            override fun createCustomerFinancialAccountRepository(tenantId: String) = accountRepo
            override fun createCustomerFinancialAccountService(tenantId: String) = accountService
            override fun createCustomerInvoiceRepository(tenantId: String) = invoiceRepo
            override fun createCustomerInvoiceService(tenantId: String) = invoiceService
        }

        useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_invoice_api_test_2026",
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

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountId,
                    tenantId = projectId,
                    projectId = projectId,
                    customerId = customerId,
                    accountNumber = "ACC-API-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testCreateAndIssueInvoiceViaRouter() = runBlocking {
        val lineReq = CustomerInvoiceLineRequest(
            description = "Packaging Boxes",
            quantity = BigDecimal("500"),
            unitPrice = BigDecimal("10.00")
        )
        val createReq = CreateCustomerInvoiceRequest(
            customerId = customerId,
            customerFinancialAccountId = accountId,
            currency = "BDT",
            lines = listOf(lineReq),
            discount = BigDecimal("100.00")
        )
        val createHttpReq = HttpRequest(
            path = "/api/v1/customer-invoices",
            method = "POST",
            body = createReq,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val createResponse = router.handleRequest(createHttpReq)
        assertNotNull(createResponse)
        assertEquals(201, createResponse.statusCode)

        val createdInvoice = (createResponse.body as ApiSuccessResponse<*>).data as CustomerInvoiceDto
        assertEquals("DRAFT", createdInvoice.status)
        assertEquals(0, BigDecimal("4900.0000").compareTo(createdInvoice.grandTotal))

        // Issue Invoice
        val issueHttpReq = HttpRequest(
            path = "/api/v1/customer-invoices/${createdInvoice.invoiceId}/issue",
            method = "POST",
            body = IssueCustomerInvoiceRequest(expectedVersion = 1L),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val issueResponse = router.handleRequest(issueHttpReq)
        assertNotNull(issueResponse)
        assertEquals(200, issueResponse.statusCode)

        // Get by ID
        val getHttpReq = HttpRequest(
            path = "/api/v1/customer-invoices/${createdInvoice.invoiceId}",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val getResponse = router.handleRequest(getHttpReq)
        assertNotNull(getResponse)
        assertEquals(200, getResponse.statusCode)
        val fetched = (getResponse.body as ApiSuccessResponse<*>).data as CustomerInvoiceDto
        assertEquals("ISSUED", fetched.status)
    }
}
