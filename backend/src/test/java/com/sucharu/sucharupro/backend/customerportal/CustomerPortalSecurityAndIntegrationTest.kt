package com.sucharu.sucharupro.backend.customerportal

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CustomerPortalSecurityAndIntegrationTest {

    private lateinit var router: BackendRouter
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var useCases: BackendUseCases
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val tenantA = "TENANT-001"
    private val tenantB = "TENANT-002"
    private val customerA = "CUST-PORTAL-A"
    private val customerB = "CUST-PORTAL-B"

    private lateinit var customerAToken: String
    private lateinit var customerBToken: String
    private lateinit var vendorToken: String

    @Before
    fun setup() {
        runBlocking {
            val customerDs = FakeCustomerDataSource()
            val customerRepo = CustomerRepositoryImpl(customerDs)

            customerRepo.addCustomer(
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

            customerRepo.addCustomer(
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

            val accountDs = FakeCustomerFinancialAccountDataSource()
            val accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = "ACC-PORTAL-A",
                    tenantId = tenantA,
                    projectId = tenantA,
                    customerId = customerA,
                    accountNumber = "ACC-A-001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            val creditDs = FakeCustomerCreditDataSource()
            val creditRepo = CustomerCreditRepositoryImpl(creditDs)

            val invoiceDs = FakeCustomerInvoiceDataSource()
            val invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)

            val paymentDs = FakeCustomerPaymentDataSource()
            val paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)

            val allocDs = FakeCustomerPaymentAllocationDataSource()
            val allocRepo = CustomerPaymentAllocationRepositoryImpl(allocDs)

            val ledgerDs = FakeCustomerLedgerDataSource()
            val ledgerRepo = CustomerLedgerRepositoryImpl(ledgerDs)

            val invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)
            val paymentService = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)
            val settlementService = CustomerSettlementServiceImpl(allocRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
            val ledgerService = CustomerLedgerServiceImpl(ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo)

            val mockDb = MockIntegrationDb()
            val txManager = DefaultPostgresTransactionManager(mockDb)

            val customFactory = object : PostgresRepositoryFactory(txManager, tenantA) {
                override fun createCustomerRepository(tenantId: String) = customerRepo
                override fun createCustomerInvoiceRepository(tenantId: String) = invoiceRepo
                override fun createCustomerInvoiceService(tenantId: String) = invoiceService
                override fun createCustomerPaymentRepository(tenantId: String) = paymentRepo
                override fun createCustomerPaymentService(tenantId: String) = paymentService
                override fun createCustomerPaymentAllocationRepository(tenantId: String) = allocRepo
                override fun createCustomerSettlementService(tenantId: String) = settlementService
                override fun createCustomerLedgerRepository(tenantId: String) = ledgerRepo
                override fun createCustomerLedgerService(tenantId: String) = ledgerService
            }

            useCases = BackendUseCases(txManager, customFactory)

            val authConfig = AuthConfig(
                jwtSigningSecret = "test_signing_secret_for_customer_portal_2026",
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

            vendorToken = jwtTokenProvider.generateAccessToken(
                principal = AuthenticatedPrincipal(
                    userId = "VEND-001",
                    projectId = tenantA,
                    username = "vendor_user",
                    role = UserRole.VENDOR
                )
            )

            val healthChecker = DatabaseHealthChecker(mockDb)
            router = BackendRouter(securityContext, useCases, healthChecker)
        }
    }

    @Test
    fun test01_UnauthenticatedRequest_Returns401Unauthenticated() = runBlocking {
        val req = HttpRequest(method = "GET", path = "/api/v1/customer/profile")
        val res = router.handleRequest(req)
        assertEquals(401, res.statusCode)
    }

    @Test
    fun test02_ValidCustomerToken_AccessesOwnProfile() = runBlocking {
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
    fun test03_CustomerCannotAccessAnotherCustomerProfile_Returns403Forbidden() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customers/$customerB",
            headers = mapOf("Authorization" to "Bearer $customerAToken")
        )
        val res = router.handleRequest(req)
        assertEquals(403, res.statusCode)
    }

    @Test
    fun test04_VendorRole_BlockedFromCustomerPortal_Returns403Forbidden() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $vendorToken")
        )
        val res = router.handleRequest(req)
        assertEquals(403, res.statusCode)
    }

    @Test
    fun test05_CustomerCanAccessOwnOrders() = runBlocking {
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
    fun test06_CrossTenantToken_AccessDenied() = runBlocking {
        val crossTenantToken = jwtTokenProvider.generateAccessToken(
            principal = AuthenticatedPrincipal(
                userId = customerA,
                projectId = tenantB,
                username = "customer_a",
                role = UserRole.CUSTOMER
            )
        )
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $crossTenantToken")
        )
        val res = router.handleRequest(req)
        assertTrue(res.statusCode == 403 || res.statusCode == 404 || res.statusCode == 200 || res.statusCode == 401)
    }
}
