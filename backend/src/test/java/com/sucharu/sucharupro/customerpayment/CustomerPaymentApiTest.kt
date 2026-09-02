package com.sucharu.sucharupro.customerpayment

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
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 03: REST Routing & API Tests for Customer Payments.
 */
class CustomerPaymentApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var token: String

    private val projectId = "PRJ-PAY-API-01"
    private val customerId = "CUS-PAY-API-01"
    private val accountId = "CFA-PAY-API-01"
    private val invoiceId = "INV-PAY-API-01"

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

        val accountService = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)
        val invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)
        val paymentService = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)

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
        }

        useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_payment_api_test_2026",
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
                    displayName = "Payment API Client",
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
    fun testRecordAndConfirmPaymentViaRouter() = runBlocking {
        val recordReq = RecordCustomerPaymentRequest(
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = invoiceId,
            amount = BigDecimal("2500.00"),
            currency = "BDT",
            paymentMethod = "BKASH",
            referenceNumber = "TRX-API-123"
        )
        val recordHttpReq = HttpRequest(
            path = "/api/v1/customer-payments",
            method = "POST",
            body = recordReq,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val recordResp = router.handleRequest(recordHttpReq)
        assertNotNull(recordResp)
        assertEquals(201, recordResp.statusCode)

        val createdPayment = (recordResp.body as ApiSuccessResponse<*>).data as CustomerPaymentDto
        assertEquals("RECORDED", createdPayment.status)
        assertEquals(0, BigDecimal("2500.0000").compareTo(createdPayment.amount))

        // Get by ID
        val getHttpReq = HttpRequest(
            path = "/api/v1/customer-payments/${createdPayment.paymentId}",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val getResp = router.handleRequest(getHttpReq)
        assertNotNull(getResp)
        assertEquals(200, getResp.statusCode)

        // Confirm payment
        val confirmHttpReq = HttpRequest(
            path = "/api/v1/customer-payments/${createdPayment.paymentId}/confirm",
            method = "POST",
            body = ConfirmCustomerPaymentRequest(expectedVersion = 1L),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val confirmResp = router.handleRequest(confirmHttpReq)
        assertNotNull(confirmResp)
        assertEquals(200, confirmResp.statusCode)

        val confirmed = (confirmResp.body as ApiSuccessResponse<*>).data as CustomerPaymentDto
        assertEquals("CONFIRMED", confirmed.status)

        // Get payments for invoice
        val invoicePaymentsHttpReq = HttpRequest(
            path = "/api/v1/customer-invoices/$invoiceId/payments",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val invoicePaymentsResp = router.handleRequest(invoicePaymentsHttpReq)
        assertNotNull(invoicePaymentsResp)
        assertEquals(200, invoicePaymentsResp.statusCode)
        val paymentList = (invoicePaymentsResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, paymentList.size)
    }
}
