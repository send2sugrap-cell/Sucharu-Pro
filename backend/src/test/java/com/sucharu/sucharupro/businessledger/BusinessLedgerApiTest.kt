package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerApiTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var managerToken: String
    private lateinit var customerToken: String
    private lateinit var expenseRepo: BusinessExpenseRepositoryImpl
    private val projectId = "PRJ-API-01"
    private val tenantId = "TENANT-001"

    @Before
    fun setup() {
        val ledgerDs = FakeBusinessLedgerDataSource()
        val ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)
        val expenseDs = FakeBusinessExpenseDataSource()
        expenseRepo = BusinessExpenseRepositoryImpl(expenseDs)
        val payableDs = FakeVendorPayableDataSource()
        val payableRepo = VendorPayableRepositoryImpl(payableDs)

        val ledgerService = BusinessLedgerServiceImpl(
            repository = ledgerRepo,
            expenseRepository = expenseRepo,
            payableRepository = payableRepo,
            defaultTenantId = tenantId
        )

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createBusinessLedgerRepository(tenantId: String) = ledgerRepo
            override fun createBusinessLedgerService(tenantId: String) = ledgerService
            override fun createBusinessExpenseRepository(tenantId: String) = expenseRepo
            override fun createVendorPayableRepository(tenantId: String) = payableRepo
        }

        val useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_business_ledger_api_test_2026",
            jwtIssuer = "sucharu-test",
            jwtAudience = "sucharu-api"
        )
        jwtTokenProvider = JwtTokenProvider(authConfig)

        managerToken = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal(
                userId = "USER-MGR-1",
                projectId = projectId,
                username = "manager1",
                role = UserRole.MANAGER,
                permissions = emptySet()
            )
        )

        customerToken = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal(
                userId = "USER-CUS-1",
                projectId = projectId,
                username = "customer1",
                role = UserRole.CUSTOMER,
                permissions = emptySet()
            )
        )

        val securityContext = BackendSecurityContext(jwtTokenProvider)
        val healthChecker = DatabaseHealthChecker(mockDb)
        router = BackendRouter(securityContext, useCases, healthChecker)
    }

    @Test
    fun testPostApprovedExpenseAndQueryViaApi() = runBlocking {
        // Prepopulate an approved expense
        val expense = BusinessExpense(
            expenseId = "EXP-API-101",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-API",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("12500.0000"),
            status = BusinessExpenseStatus.APPROVED,
            description = "High Speed Binding Glue",
            createdBy = "USER-1"
        )
        expenseRepo.createExpense(expense)

        // 1. Post Approved Expense
        val postReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-ledger/post-expense",
            headers = mapOf("Authorization" to "Bearer $managerToken"),
            body = PostApprovedExpenseRequest(
                expenseId = "EXP-API-101",
                accountCategory = "PRODUCTION_COST",
                jobId = "JOB-API-1"
            )
        )
        val postResp = router.handleRequest(postReq)
        assertEquals(201, postResp.statusCode)
        val postData = (postResp.body as ApiSuccessResponse<*>).data as BusinessLedgerPostingDto
        assertEquals("12500.0000", postData.debitAmount)
        assertEquals("PRODUCTION_COST", postData.accountCategory)

        // 2. Query Balance
        val balReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-ledger/balance",
            headers = mapOf("Authorization" to "Bearer $managerToken")
        )
        val balResp = router.handleRequest(balReq)
        assertEquals(200, balResp.statusCode)
        val balData = (balResp.body as ApiSuccessResponse<*>).data as BusinessLedgerBalanceSummaryDto
        assertEquals("12500.0000", balData.totalDebit)

        // 3. List Postings
        val listReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-ledger",
            headers = mapOf("Authorization" to "Bearer $managerToken")
        )
        val listResp = router.handleRequest(listReq)
        assertEquals(200, listResp.statusCode)
        val listData = (listResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, listData.size)
    }

    @Test
    fun testCustomerRoleDeniedAccess() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/business-ledger/balance",
            headers = mapOf("Authorization" to "Bearer $customerToken")
        )
        val resp = router.handleRequest(req)
        assertTrue(resp.statusCode == 403 || resp.statusCode == 400)
    }
}
