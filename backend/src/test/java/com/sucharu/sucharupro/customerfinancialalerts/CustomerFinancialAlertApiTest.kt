package com.sucharu.sucharupro.customerfinancialalerts

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
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialAlertDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialReportScheduleDataSource
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
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialAlertRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialReportScheduleRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialAlertServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialDocumentDeliveryServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialScheduleServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialAlertApiTest {

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

        val alertDs = FakeCustomerFinancialAlertDataSource()
        val alertRepo = CustomerFinancialAlertRepositoryImpl(alertDs)
        val alertService = CustomerFinancialAlertServiceImpl(
            alertRepository = alertRepo,
            customerRepository = customerRepo,
            accountRepository = accountRepo,
            invoiceRepository = invoiceRepo,
            paymentRepository = paymentRepo,
            creditControlService = creditControlService,
            collectionService = collectionService,
            dashboardService = dashboardService,
            notificationRepository = null
        )

        val scheduleDs = FakeCustomerFinancialReportScheduleDataSource()
        val scheduleRepo = CustomerFinancialReportScheduleRepositoryImpl(scheduleDs)
        val scheduleService = CustomerFinancialScheduleServiceImpl(
            scheduleRepository = scheduleRepo,
            customerRepository = customerRepo,
            documentDeliveryService = deliveryService,
            alertRepository = alertRepo
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
            override fun createCustomerFinancialAlertRepository(tenantId: String) = alertRepo
            override fun createCustomerFinancialAlertService(tenantId: String) = alertService
            override fun createCustomerFinancialReportScheduleRepository(tenantId: String) = scheduleRepo
            override fun createCustomerFinancialScheduleService(tenantId: String) = scheduleService
        }

        val useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_alert_api_test_2026",
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
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-API-OVERDUE",
                    tenantId = projectId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-001",
                    dueDate = System.currentTimeMillis() - 86400000L * 3,
                    grandTotal = BigDecimal("1500.00"),
                    paidAmount = BigDecimal("0.00"),
                    dueAmount = BigDecimal("1500.00"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )
        }
    }

    @Test
    fun testAlertAndScheduleFullApiLifecycle() = runBlocking {
        // 1. Evaluate Alerts
        val evalReq = HttpRequest(
            path = "/api/v1/customers/$customerId/financial-alerts/evaluate",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = emptyMap<String, Any>()
        )
        val evalResp = router.handleRequest(evalReq)
        assertEquals(200, evalResp.statusCode)
        val alertList = (evalResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, alertList.size)
        val alertDto = alertList.first() as CustomerFinancialAlertDto

        // 2. Alert Summary
        val summaryReq = HttpRequest(
            path = "/api/v1/customers/$customerId/financial-alerts/summary",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val summaryResp = router.handleRequest(summaryReq)
        assertEquals(200, summaryResp.statusCode)
        val summaryDto = (summaryResp.body as ApiSuccessResponse<*>).data as CustomerFinancialAlertSummaryDto
        assertEquals(1, summaryDto.totalOpen)

        // 3. Acknowledge Alert
        val ackReq = HttpRequest(
            path = "/api/v1/customer-financial-alerts/${alertDto.alertId}/acknowledge",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = emptyMap<String, Any>()
        )
        val ackResp = router.handleRequest(ackReq)
        assertEquals(200, ackResp.statusCode)
        val ackDto = (ackResp.body as ApiSuccessResponse<*>).data as CustomerFinancialAlertDto
        assertEquals("ACKNOWLEDGED", ackDto.status)

        // 4. Resolve Alert
        val resReq = HttpRequest(
            path = "/api/v1/customer-financial-alerts/${alertDto.alertId}/resolve",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = mapOf("reason" to "Paid in full")
        )
        val resResp = router.handleRequest(resReq)
        assertEquals(200, resResp.statusCode)
        val resDto = (resResp.body as ApiSuccessResponse<*>).data as CustomerFinancialAlertDto
        assertEquals("RESOLVED", resDto.status)

        // 5. Create Report Schedule
        val schedReq = HttpRequest(
            path = "/api/v1/customers/$customerId/financial-report-schedules",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = mapOf(
                "reportType" to "CUSTOMER_STATEMENT",
                "format" to "PDF",
                "frequency" to "MONTHLY",
                "timezone" to "Asia/Dhaka"
            )
        )
        val schedResp = router.handleRequest(schedReq)
        assertEquals(201, schedResp.statusCode)
        val schedDto = (schedResp.body as ApiSuccessResponse<*>).data as CustomerFinancialReportScheduleDto
        assertNotNull(schedDto.scheduleId)
        assertEquals("ACTIVE", schedDto.status)

        // 6. Pause Schedule
        val pauseReq = HttpRequest(
            path = "/api/v1/customer-financial-report-schedules/${schedDto.scheduleId}/pause",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = emptyMap<String, Any>()
        )
        val pauseResp = router.handleRequest(pauseReq)
        assertEquals(200, pauseResp.statusCode)
        val pauseDto = (pauseResp.body as ApiSuccessResponse<*>).data as CustomerFinancialReportScheduleDto
        assertEquals("PAUSED", pauseDto.status)

        // 7. Resume Schedule
        val resumeReq = HttpRequest(
            path = "/api/v1/customer-financial-report-schedules/${schedDto.scheduleId}/resume",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = emptyMap<String, Any>()
        )
        val resumeResp = router.handleRequest(resumeReq)
        assertEquals(200, resumeResp.statusCode)
        val resumeDto = (resumeResp.body as ApiSuccessResponse<*>).data as CustomerFinancialReportScheduleDto
        assertEquals("ACTIVE", resumeDto.status)

        // 8. Cancel Schedule
        val cancelReq = HttpRequest(
            path = "/api/v1/customer-financial-report-schedules/${schedDto.scheduleId}/cancel",
            method = "POST",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = emptyMap<String, Any>()
        )
        val cancelResp = router.handleRequest(cancelReq)
        assertEquals(200, cancelResp.statusCode)
        val cancelDto = (cancelResp.body as ApiSuccessResponse<*>).data as CustomerFinancialReportScheduleDto
        assertEquals("CANCELLED", cancelDto.status)
    }
}
