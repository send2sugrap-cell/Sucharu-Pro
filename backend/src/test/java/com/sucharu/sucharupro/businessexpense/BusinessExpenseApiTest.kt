package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BusinessExpenseApiTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var staffToken: String
    private lateinit var managerToken: String
    private lateinit var adminToken: String
    private val projectId = "PRJ-API-01"
    private val tenantId = "TENANT-001"

    @Before
    fun setup() {
        val expenseDs = FakeBusinessExpenseDataSource()
        val expenseRepo = BusinessExpenseRepositoryImpl(expenseDs)
        val expenseService = BusinessExpenseServiceImpl(expenseRepo, tenantId)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createBusinessExpenseRepository(tenantId: String) = expenseRepo
            override fun createBusinessExpenseService(tenantId: String) = expenseService
        }

        val useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_expense_api_test_2026",
            jwtIssuer = "sucharu-test",
            jwtAudience = "sucharu-api"
        )
        jwtTokenProvider = JwtTokenProvider(authConfig)

        staffToken = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal(
                userId = "USER-STAFF-1",
                projectId = projectId,
                username = "staff1",
                role = UserRole.STAFF,
                permissions = emptySet()
            )
        )

        managerToken = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal(
                userId = "USER-MGR-1",
                projectId = projectId,
                username = "manager1",
                role = UserRole.MANAGER,
                permissions = emptySet()
            )
        )

        adminToken = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal(
                userId = "USER-ADMIN-1",
                projectId = projectId,
                username = "admin1",
                role = UserRole.ADMIN,
                permissions = emptySet()
            )
        )

        val securityContext = BackendSecurityContext(jwtTokenProvider)
        val healthChecker = DatabaseHealthChecker(mockDb)
        router = BackendRouter(securityContext, useCases, healthChecker)
    }

    @Test
    fun testCreateAndListExpenseViaApi() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-OFC"
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-expenses",
            headers = mapOf("Authorization" to "Bearer $staffToken"),
            body = CreateBusinessExpenseRequest(
                categoryId = catId,
                amount = "4200.00",
                description = "Office Chairs",
                notes = "Set of 2 Ergonomic Chairs"
            )
        )
        val createResp = router.handleRequest(createReq)
        assertEquals(201, createResp.statusCode)
        val createData = (createResp.body as ApiSuccessResponse<*>).data as BusinessExpenseDto
        assertEquals("4200.00", createData.amount)
        assertEquals("DRAFT", createData.status)

        // List
        val listReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-expenses",
            headers = mapOf("Authorization" to "Bearer $staffToken")
        )
        val listResp = router.handleRequest(listReq)
        assertEquals(200, listResp.statusCode)
        val listData = (listResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, listData.size)
    }

    @Test
    fun testLifecycleEndToEndViaApi() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-TRN"
        // 1. Create
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-expenses",
            headers = mapOf("Authorization" to "Bearer $staffToken"),
            body = CreateBusinessExpenseRequest(
                categoryId = catId,
                amount = "300.00",
                description = "Client Delivery Transport"
            )
        )
        val createResp = router.handleRequest(createReq)
        val expenseId = ((createResp.body as ApiSuccessResponse<*>).data as BusinessExpenseDto).expenseId

        // 2. Submit
        val submitReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-expenses/$expenseId/submit",
            headers = mapOf("Authorization" to "Bearer $staffToken")
        )
        val submitResp = router.handleRequest(submitReq)
        assertEquals(200, submitResp.statusCode)
        val submittedDto = (submitResp.body as ApiSuccessResponse<*>).data as BusinessExpenseDto
        assertEquals("SUBMITTED", submittedDto.status)

        // 3. Approve (as Manager)
        val approveReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-expenses/$expenseId/approve",
            headers = mapOf("Authorization" to "Bearer $managerToken"),
            body = ApproveExpenseRequest(notes = "Verified transport receipt")
        )
        val approveResp = router.handleRequest(approveReq)
        assertEquals(200, approveResp.statusCode)
        val approvedDto = (approveResp.body as ApiSuccessResponse<*>).data as BusinessExpenseDto
        assertEquals("APPROVED", approvedDto.status)

        // 4. Audit
        val auditReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-expenses/$expenseId/audit",
            headers = mapOf("Authorization" to "Bearer $managerToken")
        )
        val auditResp = router.handleRequest(auditReq)
        assertEquals(200, auditResp.statusCode)
        val auditList = (auditResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(3, auditList.size)
    }

    @Test
    fun testCategoriesApi() = runBlocking {
        // List Categories
        val listCatReq = HttpRequest(
            method = "GET",
            path = "/api/v1/expense-categories",
            headers = mapOf("Authorization" to "Bearer $staffToken")
        )
        val listCatResp = router.handleRequest(listCatReq)
        assertEquals(200, listCatResp.statusCode)

        // Create Custom Category (as Admin)
        val createCatReq = HttpRequest(
            method = "POST",
            path = "/api/v1/expense-categories",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = CreateBusinessExpenseCategoryRequest(
                name = "Software Licenses",
                code = "CAT-SFT",
                description = "Cloud & SaaS subscriptions"
            )
        )
        val createCatResp = router.handleRequest(createCatReq)
        assertEquals(201, createCatResp.statusCode)
        val createdCat = (createCatResp.body as ApiSuccessResponse<*>).data as BusinessExpenseCategoryDto
        assertEquals("CAT-SFT", createdCat.code)
    }
}
