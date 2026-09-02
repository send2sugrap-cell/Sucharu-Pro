package com.sucharu.sucharupro.customerfinancialdocumentdelivery

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialDocumentDeliveryServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialDocumentDeliveryApiTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var adminToken: String
    private val projectId = "PRJ-API-01"
    private val customerId = "CUS-API-01"
    private val accountId = "CFA-API-01"

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
        val collectionDs = FakeCustomerCollectionDataSource()
        val collectionRepo = CustomerCollectionRepositoryImpl(collectionDs)
        val ledgerDs = FakeCustomerLedgerDataSource()
        val ledgerRepo = CustomerLedgerRepositoryImpl(ledgerDs)

        val settlementService = CustomerSettlementServiceImpl(allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
        val creditControlService = CustomerCreditControlServiceImpl(creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo)
        val collectionService = CustomerCollectionServiceImpl(collectionRepo, customerRepo, accountRepo, invoiceRepo, settlementService, creditControlService)
        val ledgerService = CustomerLedgerServiceImpl(ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo)
        val dashboardService = CustomerFinancialDashboardServiceImpl(
            customerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, collectionRepo,
            settlementService, creditControlService, collectionService, ledgerService
        )
        val reportingService = CustomerFinancialReportingServiceImpl(
            customerRepository = customerRepo,
            accountRepository = accountRepo,
            invoiceRepository = invoiceRepo,
            paymentRepository = paymentRepo,
            creditRepository = creditRepo,
            ledgerService = ledgerService,
            settlementService = settlementService,
            creditControlService = creditControlService,
            collectionService = collectionService,
            dashboardService = dashboardService
        )

        val deliveryDs = FakeCustomerFinancialDocumentDeliveryDataSource()
        val deliveryRepo = CustomerFinancialDocumentDeliveryRepositoryImpl(deliveryDs)
        val deliveryService = CustomerFinancialDocumentDeliveryServiceImpl(
            deliveryRepository = deliveryRepo,
            reportingService = reportingService,
            customerRepository = customerRepo,
            notificationRepository = null
        )

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
            override fun createCustomerCollectionRepository(tenantId: String) = collectionRepo
            override fun createCustomerCollectionService(tenantId: String) = collectionService
            override fun createCustomerLedgerRepository(tenantId: String) = ledgerRepo
            override fun createCustomerLedgerService(tenantId: String) = ledgerService
            override fun createCustomerFinancialDashboardService(tenantId: String) = dashboardService
            override fun createCustomerFinancialReportingService(tenantId: String) = reportingService
            override fun createCustomerFinancialDocumentDeliveryRepository(tenantId: String) = deliveryRepo
            override fun createCustomerFinancialDocumentDeliveryService(tenantId: String) = deliveryService
        }

        val useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_del_api_test_2026",
            jwtIssuer = "sucharu-test",
            jwtAudience = "sucharu-api"
        )
        jwtTokenProvider = JwtTokenProvider(authConfig)
        val securityContext = BackendSecurityContext(jwtTokenProvider)
        val healthChecker = DatabaseHealthChecker(mockDb)

        router = BackendRouter(
            securityContext = securityContext,
            useCases = useCases,
            healthChecker = healthChecker
        )

        adminToken = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal("usr-admin", projectId, "admin_user", com.sucharu.sucharupro.data.api.model.UserRole.ADMIN)
        )

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-API-01",
                    displayName = "API Customer",
                    primaryPhone = "+8801700000001",
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
        }
    }

    @Test
    fun testCreateListAccessNotifyAndRevokeEndpoints() = runBlocking {
        // 1. Create Document Delivery
        val createReq = HttpRequest(
            path = "/api/v1/customer-financial-documents",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = mapOf(
                "customerId" to customerId,
                "reportType" to "CUSTOMER_STATEMENT",
                "format" to "CSV"
            )
        )
        val createResp = router.handleRequest(createReq)
        assertEquals(201, createResp.statusCode)
        val createdDto = (createResp.body as ApiSuccessResponse<*>).data as CustomerFinancialDocumentDeliveryDto
        assertNotNull(createdDto.deliveryId)

        // 2. List Document Deliveries
        val listReq = HttpRequest(
            path = "/api/v1/customer-financial-documents?customerId=$customerId",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val listResp = router.handleRequest(listReq)
        assertEquals(200, listResp.statusCode)
        val list = (listResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, list.size)

        // 3. Access Document Payload
        val accessReq = HttpRequest(
            path = "/api/v1/customer-financial-documents/${createdDto.deliveryId}/access",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = emptyMap<String, Any>()
        )
        val accessResp = router.handleRequest(accessReq)
        assertEquals(200, accessResp.statusCode)
        val accessDto = (accessResp.body as ApiSuccessResponse<*>).data as CustomerFinancialDocumentAccessResponseDto
        assertEquals(createdDto.deliveryId, accessDto.deliveryId)
        assertNotNull(accessDto.contentBase64)

        // 4. Notify Customer
        val notifyReq = HttpRequest(
            path = "/api/v1/customer-financial-documents/${createdDto.deliveryId}/notify",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = mapOf("customMessage" to "Your statement is ready")
        )
        val notifyResp = router.handleRequest(notifyReq)
        assertEquals(200, notifyResp.statusCode)

        // 5. Revoke Document Delivery
        val revokeReq = HttpRequest(
            path = "/api/v1/customer-financial-documents/${createdDto.deliveryId}/revoke",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = mapOf("reason" to "Administrative cancellation")
        )
        val revokeResp = router.handleRequest(revokeReq)
        assertEquals(200, revokeResp.statusCode)
        val revokedDto = (revokeResp.body as ApiSuccessResponse<*>).data as CustomerFinancialDocumentDeliveryDto
        assertEquals(true, revokedDto.isRevoked)

        // 6. Audit Trail
        val auditReq = HttpRequest(
            path = "/api/v1/customer-financial-documents/${createdDto.deliveryId}/audit",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val auditResp = router.handleRequest(auditReq)
        assertEquals(200, auditResp.statusCode)
        val auditList = (auditResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(4, auditList.size)
    }
}
