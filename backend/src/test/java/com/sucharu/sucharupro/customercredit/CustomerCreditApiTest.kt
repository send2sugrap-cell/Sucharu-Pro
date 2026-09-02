package com.sucharu.sucharupro.customercredit

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
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 04: REST Routing & API Tests for Customer Advances, Credits, Adjustments, and Refunds.
 */
class CustomerCreditApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var token: String

    private val projectId = "PRJ-CR-API-01"
    private val customerId = "CUS-CR-API-01"
    private val accountId = "CFA-CR-API-01"
    private val invoiceId = "INV-CR-API-01"

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
        val paymentDs = FakeCustomerPaymentDataSource()
        val paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)
        val creditDs = FakeCustomerCreditDataSource()
        val creditRepo = CustomerCreditRepositoryImpl(creditDs)

        val accountService = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)
        val invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)
        val paymentService = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)
        val creditService = CustomerCreditServiceImpl(creditRepo, accountRepo, invoiceRepo, customerRepo, paymentRepo)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createCustomerRepository(tenantId: String) = customerRepo
            override fun createCustomerFinancialAccountRepository(tenantId: String) = accountRepo
            override fun createCustomerFinancialAccountService(tenantId: String) = accountService
            override fun createCustomerInvoiceRepository(tenantId: String) = invoiceRepo
            override fun createCustomerInvoiceService(tenantId: String) = invoiceService
            override fun createCustomerPaymentRepository(tenantId: String) = paymentRepo
            override fun createCustomerPaymentService(tenantId: String) = paymentService
            override fun createCustomerCreditRepository(tenantId: String) = creditRepo
            override fun createCustomerCreditService(tenantId: String) = creditService
        }

        useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_credit_api_test_2026",
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
                    displayName = "Credit API Client",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000088",
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

            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = invoiceId,
                    tenantId = projectId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-API-2026",
                    grandTotal = BigDecimal("5000.0000"),
                    paidAmount = BigDecimal.ZERO,
                    dueAmount = BigDecimal("5000.0000"),
                    status = CustomerInvoiceStatus.ISSUED,
                    version = 1L
                )
            )
        }
    }

    @Test
    fun testRecordAdvanceAndGetCreditSummaryViaRouter() = runBlocking {
        // 1. Record Advance
        val recordReq = RecordCustomerAdvanceRequest(
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("3000.00"),
            currency = "BDT",
            paymentMethod = "BKASH",
            referenceNumber = "ADV-API-123"
        )
        val recordHttpReq = HttpRequest(
            path = "/api/v1/customer-credits/advance",
            method = "POST",
            body = recordReq,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val recordResp = router.handleRequest(recordHttpReq)
        assertNotNull(recordResp)
        assertEquals(201, recordResp.statusCode)

        val createdAdvance = (recordResp.body as ApiSuccessResponse<*>).data as CustomerAdvanceDto
        assertEquals("AVAILABLE", createdAdvance.status)
        assertEquals(0, BigDecimal("3000.0000").compareTo(createdAdvance.amount))

        // 2. Get Credit Summary
        val summaryHttpReq = HttpRequest(
            path = "/api/v1/customers/$customerId/credit-summary",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val summaryResp = router.handleRequest(summaryHttpReq)
        assertNotNull(summaryResp)
        assertEquals(200, summaryResp.statusCode)

        val summary = (summaryResp.body as ApiSuccessResponse<*>).data as CustomerCreditSummaryDto
        assertEquals(0, BigDecimal("3000.0000").compareTo(summary.totalAvailableCredit))

        // 3. Allocate Credit to Invoice
        val allocReq = AllocateCustomerCreditRequest(
            customerId = customerId,
            invoiceId = invoiceId,
            advanceId = createdAdvance.advanceId,
            amount = BigDecimal("1500.00")
        )
        val allocHttpReq = HttpRequest(
            path = "/api/v1/customer-credit-allocations",
            method = "POST",
            body = allocReq,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val allocResp = router.handleRequest(allocHttpReq)
        assertNotNull(allocResp)
        assertEquals(201, allocResp.statusCode)

        val allocation = (allocResp.body as ApiSuccessResponse<*>).data as CustomerCreditAllocationDto
        assertEquals("ALLOCATED", allocation.status)
        assertEquals(0, BigDecimal("1500.0000").compareTo(allocation.allocatedAmount))
    }
}
