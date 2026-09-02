package com.sucharu.sucharupro.customersettlement

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
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerSettlementApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var token: String

    private val projectId = "PRJ-SET-API-01"
    private val customerId = "CUS-SET-API-01"
    private val accountId = "CFA-SET-API-01"
    private val invoiceId = "INV-SET-API-01"
    private val paymentId = "PAY-SET-API-01"

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
        val allocationDs = FakeCustomerPaymentAllocationDataSource()
        val allocationRepo = CustomerPaymentAllocationRepositoryImpl(allocationDs)

        val accountService = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)
        val invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)
        val paymentService = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)
        val creditService = CustomerCreditServiceImpl(creditRepo, accountRepo, invoiceRepo, customerRepo, paymentRepo)
        val settlementService = CustomerSettlementServiceImpl(allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)

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
            override fun createCustomerPaymentAllocationRepository(tenantId: String) = allocationRepo
            override fun createCustomerSettlementService(tenantId: String) = settlementService
        }

        useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_settlement_api_test_2026",
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
                    displayName = "Settlement API Customer",
                    primaryPhone = "+8801700000088",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
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
                    grandTotal = BigDecimal("10000.0000"),
                    dueAmount = BigDecimal("10000.0000"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )

            paymentRepo.createPayment(
                CustomerPayment(
                    paymentId = paymentId,
                    tenantId = projectId,
                    projectId = projectId,
                    paymentNumber = "PAY-API-2026",
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    amount = BigDecimal("6000.0000"),
                    status = CustomerPaymentStatus.CONFIRMED
                )
            )
        }
    }

    @Test
    fun testPaymentAllocationAndSettlementSummaryFlow() = runBlocking {
        // 1. Allocate payment via API
        val allocReq = HttpRequest(
            path = "/api/v1/customer-payments/$paymentId/allocations",
            method = "POST",
            body = AllocateCustomerPaymentRequest(
                invoiceId = invoiceId,
                amount = BigDecimal("6000.0000"),
                idempotencyKey = "IDEMP-SET-01"
            ),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val allocResp = router.handleRequest(allocReq)
        assertNotNull(allocResp)
        assertEquals(201, allocResp.statusCode)
        val allocationDto = (allocResp.body as ApiSuccessResponse<*>).data as CustomerPaymentAllocationDto
        assertEquals(BigDecimal("6000.0000"), allocationDto.allocatedAmount)

        // 2. List allocations for invoice
        val listInvAllocReq = HttpRequest(
            path = "/api/v1/customer-invoices/$invoiceId/allocations",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val listInvResp = router.handleRequest(listInvAllocReq)
        assertEquals(200, listInvResp.statusCode)
        val invAllocList = (listInvResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, invAllocList.size)

        // 3. Get settlement summary
        val summaryReq = HttpRequest(
            path = "/api/v1/customers/$customerId/settlement-summary",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val summaryResp = router.handleRequest(summaryReq)
        assertEquals(200, summaryResp.statusCode)
        val summary = (summaryResp.body as ApiSuccessResponse<*>).data as CustomerSettlementSummaryDto
        assertEquals(BigDecimal("10000.0000"), summary.totalInvoiced)
        assertEquals(BigDecimal("6000.0000"), summary.totalPaid)
        assertEquals(BigDecimal("6000.0000"), summary.totalAllocated)
        assertEquals(BigDecimal("0.0000"), summary.totalUnallocated)
        assertEquals(BigDecimal("4000.0000"), summary.totalOutstanding)

        // 4. Reverse allocation
        val revReq = HttpRequest(
            path = "/api/v1/customer-payment-allocations/${allocationDto.allocationId}/reverse",
            method = "POST",
            body = ReverseCustomerPaymentAllocationRequest(reason = "Client requested reallocation"),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val revResp = router.handleRequest(revReq)
        assertEquals(200, revResp.statusCode)
        val reversedDto = (revResp.body as ApiSuccessResponse<*>).data as CustomerPaymentAllocationDto
        assertEquals("REVERSED", reversedDto.status)
    }
}
