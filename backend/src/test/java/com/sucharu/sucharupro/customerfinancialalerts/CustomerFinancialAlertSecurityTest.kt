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
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlert
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertSeverity
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertType
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
import org.junit.Before
import org.junit.Test

class CustomerFinancialAlertSecurityTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private val projectId = "PRJ-SEC-01"
    private val customerId1 = "CUS-SEC-01"
    private val customerId2 = "CUS-SEC-02"

    private lateinit var customer1Token: String
    private lateinit var customer2Token: String
    private lateinit var vendorToken: String
    private lateinit var affiliateToken: String

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
            jwtSigningSecret = "test_signing_secret_for_sec_alert_api_test_2026",
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

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId1,
                    customerCode = "CUS-SEC-01",
                    displayName = "Customer 1",
                    primaryPhone = "+8801700000001",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId2,
                    customerCode = "CUS-SEC-02",
                    displayName = "Customer 2",
                    primaryPhone = "+8801700000002",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            // Add alert for Customer 1
            alertRepo.saveAlert(
                CustomerFinancialAlert(
                    alertId = "ALT-CUS1",
                    tenantId = projectId,
                    projectId = projectId,
                    customerId = customerId1,
                    alertType = CustomerFinancialAlertType.INVOICE_OVERDUE,
                    severity = CustomerFinancialAlertSeverity.HIGH,
                    title = "Overdue",
                    safeMessage = "Your invoice is overdue",
                    sourceType = "INVOICE",
                    sourceId = "INV-1",
                    deduplicationKey = "dedup_1"
                )
            )
        }

        customer1Token = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal(customerId1, projectId, "cust1_user", com.sucharu.sucharupro.data.api.model.UserRole.CUSTOMER, customerId = customerId1)
        )
        customer2Token = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal(customerId2, projectId, "cust2_user", com.sucharu.sucharupro.data.api.model.UserRole.CUSTOMER, customerId = customerId2)
        )
        vendorToken = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal("usr-ven", projectId, "vendor_user", com.sucharu.sucharupro.data.api.model.UserRole.VENDOR, vendorId = "VEN-001")
        )
        affiliateToken = jwtTokenProvider.generateAccessToken(
            AuthenticatedPrincipal("usr-aff", projectId, "aff_user", com.sucharu.sucharupro.data.api.model.UserRole.AFFILIATE, affiliateId = "AFF-001")
        )
    }

    @Test
    fun testCustomerCanAccessOwnAlertsAndIsDeniedOtherCustomerAlerts() = runBlocking {
        // Customer 1 gets own alert -> 200 OK
        val req1 = HttpRequest(
            path = "/api/v1/customer-financial-alerts/ALT-CUS1",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $customer1Token")
        )
        val resp1 = router.handleRequest(req1)
        assertEquals(200, resp1.statusCode)

        // Customer 2 attempts to get Customer 1's alert -> 403 Forbidden
        val req2 = HttpRequest(
            path = "/api/v1/customer-financial-alerts/ALT-CUS1",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $customer2Token")
        )
        val resp2 = router.handleRequest(req2)
        assertEquals(403, resp2.statusCode)
    }

    @Test
    fun testVendorAndAffiliateRolesAreDenied() = runBlocking {
        val reqVendor = HttpRequest(
            path = "/api/v1/customer-financial-alerts",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $vendorToken")
        )
        val respVendor = router.handleRequest(reqVendor)
        assertEquals(403, respVendor.statusCode)

        val reqAffiliate = HttpRequest(
            path = "/api/v1/customer-financial-alerts",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $affiliateToken")
        )
        val respAffiliate = router.handleRequest(reqAffiliate)
        assertEquals(403, respAffiliate.statusCode)
    }
}
