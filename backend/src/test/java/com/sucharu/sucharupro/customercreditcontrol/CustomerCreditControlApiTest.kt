package com.sucharu.sucharupro.customercreditcontrol

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
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditControlApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var token: String

    private val projectId = "PRJ-CC-API-01"
    private val customerId = "CUS-CC-API-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin_01",
        projectId = projectId,
        username = "admin_user",
        role = UserRole.ADMIN,
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
        val creditControlDs = FakeCustomerCreditControlDataSource()
        val creditControlRepo = CustomerCreditControlRepositoryImpl(creditControlDs)

        val settlementService = CustomerSettlementServiceImpl(allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
        val creditControlService = CustomerCreditControlServiceImpl(creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createCustomerRepository(tenantId: String) = customerRepo
            override fun createCustomerFinancialAccountRepository(tenantId: String) = accountRepo
            override fun createCustomerInvoiceRepository(tenantId: String) = invoiceRepo
            override fun createCustomerPaymentRepository(tenantId: String) = paymentRepo
            override fun createCustomerCreditRepository(tenantId: String) = creditRepo
            override fun createCustomerPaymentAllocationRepository(tenantId: String) = allocationRepo
            override fun createCustomerSettlementService(tenantId: String) = settlementService
            override fun createCustomerCreditControlRepository(tenantId: String) = creditControlRepo
            override fun createCustomerCreditControlService(tenantId: String) = creditControlService
        }

        useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_credit_control_api_test_2026",
            jwtIssuer = "sucharu-test",
            jwtAudience = "sucharu-api"
        )
        jwtTokenProvider = JwtTokenProvider(authConfig)
        token = jwtTokenProvider.generateAccessToken(adminPrincipal)

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
                    displayName = "Credit Control API Customer",
                    primaryPhone = "+8801700000001",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = "CFA-API-01",
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
    fun testCreditProfileAndRiskControlApiLifecycle() = runBlocking {
        // 1. Update credit profile
        val updateReq = HttpRequest(
            path = "/api/v1/customers/$customerId/credit-profile",
            method = "PUT",
            body = UpdateCustomerCreditProfileRequest(
                creditLimit = BigDecimal("75000.0000"),
                currency = "BDT",
                paymentTermsType = "NET_30",
                creditDays = 30,
                requiresAdvance = false,
                reason = "Approved 75k credit"
            ),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val updateResp = router.handleRequest(updateReq)
        assertEquals(200, updateResp.statusCode)
        val profileDto = (updateResp.body as ApiSuccessResponse<*>).data as CustomerCreditProfileDto
        assertEquals(BigDecimal("75000.0000"), profileDto.creditLimit)

        // 2. Perform credit check
        val checkReq = HttpRequest(
            path = "/api/v1/customers/$customerId/credit-check",
            method = "POST",
            body = CustomerCreditCheckApiRequest(
                requestedExposure = BigDecimal("25000.0000")
            ),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val checkResp = router.handleRequest(checkReq)
        assertEquals(200, checkResp.statusCode)
        val checkDto = (checkResp.body as ApiSuccessResponse<*>).data as CustomerCreditCheckResultDto
        assertTrue(checkDto.approved)

        // 3. Place financial hold
        val holdReq = HttpRequest(
            path = "/api/v1/customers/$customerId/financial-hold",
            method = "POST",
            body = CustomerFinancialHoldRequest(reason = "Audit review required"),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val holdResp = router.handleRequest(holdReq)
        assertEquals(200, holdResp.statusCode)
        val holdProfile = (holdResp.body as ApiSuccessResponse<*>).data as CustomerCreditProfileDto
        assertTrue(holdProfile.financialHold)

        // 4. Get receivable risk summary
        val riskReq = HttpRequest(
            path = "/api/v1/customers/$customerId/receivable-risk",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val riskResp = router.handleRequest(riskReq)
        assertEquals(200, riskResp.statusCode)
        val riskSummary = (riskResp.body as ApiSuccessResponse<*>).data as CustomerReceivableRiskSummaryDto
        assertEquals("FINANCIAL_HOLD", riskSummary.riskStatus)

        // 5. Release financial hold
        val relReq = HttpRequest(
            path = "/api/v1/customers/$customerId/financial-hold/release",
            method = "POST",
            body = CustomerFinancialHoldRequest(reason = "Audit review completed"),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val relResp = router.handleRequest(relReq)
        assertEquals(200, relResp.statusCode)
        val releasedProfile = (relResp.body as ApiSuccessResponse<*>).data as CustomerCreditProfileDto
        assertEquals(false, releasedProfile.financialHold)
    }
}
