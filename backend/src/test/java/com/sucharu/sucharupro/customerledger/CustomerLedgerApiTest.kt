package com.sucharu.sucharupro.customerledger

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
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
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
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerLedgerApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var token: String

    private val projectId = "PRJ-LED-API-01"
    private val customerId = "CUS-LED-API-01"
    private val accountId = "CFA-LED-API-01"
    private val invoiceId = "INV-LED-API-01"

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
        val ledgerDs = FakeCustomerLedgerDataSource()
        val ledgerRepo = CustomerLedgerRepositoryImpl(ledgerDs)

        val accountService = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)
        val invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)
        val paymentService = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)
        val creditService = CustomerCreditServiceImpl(creditRepo, accountRepo, invoiceRepo, customerRepo, paymentRepo)
        val ledgerService = CustomerLedgerServiceImpl(ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo)

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
            override fun createCustomerLedgerRepository(tenantId: String) = ledgerRepo
            override fun createCustomerLedgerService(tenantId: String) = ledgerService
        }

        useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_ledger_api_test_2026",
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
                    displayName = "Ledger API Client",
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
    fun testGetStatementAndReconciliationViaRouter() = runBlocking {
        // 1. Get Statement
        val stmtHttpReq = HttpRequest(
            path = "/api/v1/customers/$customerId/statement",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val stmtResp = router.handleRequest(stmtHttpReq)
        assertNotNull(stmtResp)
        assertEquals(200, stmtResp.statusCode)

        val stmt = (stmtResp.body as ApiSuccessResponse<*>).data as CustomerStatementDto
        assertEquals(customerId, stmt.customerId)
        assertEquals(1, stmt.entries.size)
        assertEquals(BigDecimal("5000.0000"), stmt.closingBalance)

        // 2. Get Ledger
        val ledgerHttpReq = HttpRequest(
            path = "/api/v1/customers/$customerId/ledger",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val ledgerResp = router.handleRequest(ledgerHttpReq)
        assertNotNull(ledgerResp)
        assertEquals(200, ledgerResp.statusCode)

        // 3. Trigger Receivable Reconciliation
        val reconHttpReq = HttpRequest(
            path = "/api/v1/customers/$customerId/receivable-reconciliation",
            method = "POST",
            body = ReconcileCustomerReceivableRequest(notes = "Monthly audit test"),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val reconResp = router.handleRequest(reconHttpReq)
        assertNotNull(reconResp)
        assertEquals(200, reconResp.statusCode)

        val recon = (reconResp.body as ApiSuccessResponse<*>).data as CustomerReceivableReconciliationDto
        assertEquals("CONSISTENT", recon.status)
        assertTrue(recon.isConsistent)
    }
}
