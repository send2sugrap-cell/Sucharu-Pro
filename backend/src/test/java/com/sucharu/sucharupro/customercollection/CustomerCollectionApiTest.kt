package com.sucharu.sucharupro.customercollection

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
import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
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
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
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
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCollectionApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var token: String

    private val projectId = "PRJ-COL-API-01"
    private val customerId = "CUS-COL-API-01"

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
        val collectionDs = FakeCustomerCollectionDataSource()
        val collectionRepo = CustomerCollectionRepositoryImpl(collectionDs)

        val settlementService = CustomerSettlementServiceImpl(allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
        val creditControlService = CustomerCreditControlServiceImpl(creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo)
        val collectionService = CustomerCollectionServiceImpl(collectionRepo, customerRepo, accountRepo, invoiceRepo, settlementService, creditControlService)

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
        }

        useCases = BackendUseCases(txManager, customFactory)

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_signing_secret_for_collection_api_test_2026",
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
                    displayName = "Collection API Customer",
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

            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-API-01",
                    tenantId = projectId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = "CFA-API-01",
                    invoiceNumber = "INV-API-01",
                    grandTotal = BigDecimal("40000.0000"),
                    dueAmount = BigDecimal("40000.0000"),
                    dueDate = System.currentTimeMillis() - 86400000L,
                    status = CustomerInvoiceStatus.ISSUED
                )
            )
        }
    }

    @Test
    fun testCollectionApiLifecycle() = runBlocking {
        // 1. Create collection action
        val createReq = HttpRequest(
            path = "/api/v1/customers/$customerId/collection-actions",
            method = "POST",
            body = CreateCustomerCollectionActionRequest(
                invoiceId = "INV-API-01",
                actionType = "PHONE_FOLLOW_UP",
                notes = "Initial call"
            ),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val createResp = router.handleRequest(createReq)
        assertEquals(200, createResp.statusCode)
        val actionDto = (createResp.body as ApiSuccessResponse<*>).data as CustomerCollectionActionDto
        assertEquals("SCHEDULED", actionDto.status)
        val actionId = actionDto.actionId

        // 2. Complete collection action
        val compReq = HttpRequest(
            path = "/api/v1/customer-collection-actions/$actionId/complete",
            method = "POST",
            body = CompleteCustomerCollectionActionRequest(
                outcome = "PAYMENT_PROMISED",
                outcomeNotes = "Customer will pay next Monday"
            ),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val compResp = router.handleRequest(compReq)
        assertEquals(200, compResp.statusCode)
        val compDto = (compResp.body as ApiSuccessResponse<*>).data as CustomerCollectionActionDto
        assertEquals("COMPLETED", compDto.status)

        // 3. Create payment promise
        val promiseReq = HttpRequest(
            path = "/api/v1/customer-collection-actions/$actionId/payment-promise",
            method = "POST",
            body = CreateCustomerPaymentPromiseRequest(
                invoiceId = "INV-API-01",
                promisedAmount = BigDecimal("40000.0000"),
                promisedDate = System.currentTimeMillis() + 86400000L,
                notes = "Promised full payment"
            ),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val promiseResp = router.handleRequest(promiseReq)
        assertEquals(200, promiseResp.statusCode)
        val promiseDto = (promiseResp.body as ApiSuccessResponse<*>).data as CustomerPaymentPromiseDto
        assertEquals(BigDecimal("40000.0000"), promiseDto.promisedAmount)

        // 4. Get Collection Summary
        val sumReq = HttpRequest(
            path = "/api/v1/customers/$customerId/collection-summary",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val sumResp = router.handleRequest(sumReq)
        assertEquals(200, sumResp.statusCode)
        val sumDto = (sumResp.body as ApiSuccessResponse<*>).data as CustomerReceivableCollectionSummaryDto
        assertEquals(BigDecimal("40000.0000"), sumDto.totalOutstanding)
        assertEquals(1, sumDto.activePromiseCount)
    }
}
