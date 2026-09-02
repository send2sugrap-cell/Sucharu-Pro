package com.sucharu.sucharupro.vendorpayable

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.service.vendorpayable.VendorPayableServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPayableApiTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var staffToken: String
    private lateinit var managerToken: String
    private val projectId = "PRJ-API-01"
    private val tenantId = "TENANT-001"
    private val vendorId = "VEND-API-01"

    @Before
    fun setup() {
        val payableDs = FakeVendorPayableDataSource()
        val payableRepo = VendorPayableRepositoryImpl(payableDs)
        val payableService = VendorPayableServiceImpl(payableRepo, tenantId)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createVendorPayableRepository(tenantId: String) = payableRepo
            override fun createVendorPayableService(tenantId: String) = payableService
        }

        val useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_vendor_payable_api_test_2026",
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

        val securityContext = BackendSecurityContext(jwtTokenProvider)
        val healthChecker = DatabaseHealthChecker(mockDb)
        router = BackendRouter(securityContext, useCases, healthChecker)
    }

    @Test
    fun testCreateAndListPayableViaApi() = runBlocking {
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-payables",
            headers = mapOf("Authorization" to "Bearer $staffToken"),
            body = CreateVendorPayableRequest(
                vendorId = vendorId,
                originalAmount = "7500.00",
                description = "Offset Screen Coating",
                billReference = "BILL-9901"
            )
        )
        val createResp = router.handleRequest(createReq)
        assertEquals(201, createResp.statusCode)
        val createData = (createResp.body as ApiSuccessResponse<*>).data as VendorPayableDto
        assertEquals("7500.0000", createData.originalAmount)
        assertEquals("DRAFT", createData.status)

        // List
        val listReq = HttpRequest(
            method = "GET",
            path = "/api/v1/vendor-payables",
            headers = mapOf("Authorization" to "Bearer $staffToken")
        )
        val listResp = router.handleRequest(listReq)
        assertEquals(200, listResp.statusCode)
        val listData = (listResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, listData.size)
    }

    @Test
    fun testLifecycleAndPaymentAllocationViaApi() = runBlocking {
        // 1. Create
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-payables",
            headers = mapOf("Authorization" to "Bearer $staffToken"),
            body = CreateVendorPayableRequest(
                vendorId = vendorId,
                originalAmount = "10000.00",
                description = "Chemical & Wash Solutions"
            )
        )
        val createResp = router.handleRequest(createReq)
        val payableId = ((createResp.body as ApiSuccessResponse<*>).data as VendorPayableDto).payableId

        // 2. Submit
        val submitReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-payables/$payableId/submit",
            headers = mapOf("Authorization" to "Bearer $staffToken")
        )
        val submitResp = router.handleRequest(submitReq)
        assertEquals(200, submitResp.statusCode)
        assertEquals("SUBMITTED", ((submitResp.body as ApiSuccessResponse<*>).data as VendorPayableDto).status)

        // 3. Approve
        val approveReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-payables/$payableId/approve",
            headers = mapOf("Authorization" to "Bearer $managerToken"),
            body = ApproveVendorPayableRequest(notes = "Approved by Finance")
        )
        val approveResp = router.handleRequest(approveReq)
        assertEquals(200, approveResp.statusCode)
        assertEquals("APPROVED", ((approveResp.body as ApiSuccessResponse<*>).data as VendorPayableDto).status)

        // 4. Allocate Payment: 4,000.00
        val payReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-payables/$payableId/payments/allocate",
            headers = mapOf("Authorization" to "Bearer $managerToken"),
            body = AllocateVendorPayablePaymentRequest(
                amount = "4000.00",
                paymentMethod = "BANK",
                paymentReference = "CHQ-77112"
            )
        )
        val payResp = router.handleRequest(payReq)
        assertEquals(200, payResp.statusCode)
        val paidDto = (payResp.body as ApiSuccessResponse<*>).data as VendorPayableDto
        assertEquals("PARTIALLY_PAID", paidDto.status)
        assertEquals("4000.0000", paidDto.paidAmount)
        assertEquals("6000.0000", paidDto.outstandingAmount)

        // 5. Check Summary & Aging
        val summaryReq = HttpRequest(
            method = "GET",
            path = "/api/v1/vendors/$vendorId/payables/summary",
            headers = mapOf("Authorization" to "Bearer $managerToken")
        )
        val summaryResp = router.handleRequest(summaryReq)
        assertEquals(200, summaryResp.statusCode)
        val summaryDto = (summaryResp.body as ApiSuccessResponse<*>).data as VendorPayableSummaryDto
        assertEquals("10000.0000", summaryDto.totalApprovedLiability)
        assertEquals("4000.0000", summaryDto.totalPaid)
        assertEquals("6000.0000", summaryDto.totalOutstanding)

        val agingReq = HttpRequest(
            method = "GET",
            path = "/api/v1/vendors/$vendorId/payables/aging",
            headers = mapOf("Authorization" to "Bearer $managerToken")
        )
        val agingResp = router.handleRequest(agingReq)
        assertEquals(200, agingResp.statusCode)
        val agingDto = (agingResp.body as ApiSuccessResponse<*>).data as VendorPayableAgingReportDto
        assertEquals("6000.0000", agingDto.totalOutstanding)
    }
}
