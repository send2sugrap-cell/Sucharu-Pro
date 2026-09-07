package com.sucharu.sucharupro.backend.customerfinancial

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
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerInvoicePaymentEndToEndIntegrationTest {

    private lateinit var router: BackendRouter
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var useCases: BackendUseCases
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var adminToken: String

    private lateinit var invoiceService: CustomerInvoiceServiceImpl
    private lateinit var paymentService: CustomerPaymentServiceImpl
    private lateinit var settlementService: CustomerSettlementServiceImpl
    private lateinit var ledgerService: CustomerLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUST-E2E-01"
    private val financialAccountId = "ACC-E2E-01"

    @Before
    fun setup() {
        runBlocking {
            val customerDs = FakeCustomerDataSource()
            val customerRepo = CustomerRepositoryImpl(customerDs)
            val addCustRes = customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-E2E",
                    displayName = "Acme E2E Printing Ltd.",
                    primaryPhone = "+8801700000000",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            if (addCustRes is DomainResult.Error) {
                fail("Customer setup failed: " + addCustRes.message)
            }

            val accountDs = FakeCustomerFinancialAccountDataSource()
            val accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
            val addAccRes = accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = financialAccountId,
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    accountNumber = "ACC-2026-001",
                    status = CustomerFinancialAccountStatus.ACTIVE,
                    createdAt = 1000L,
                    createdBy = "system",
                    updatedAt = 1000L,
                    updatedBy = "system"
                )
            )
            if (addAccRes is DomainResult.Error) {
                fail("Account setup failed: " + addAccRes.message)
            }

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

            invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)
            paymentService = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)
            settlementService = CustomerSettlementServiceImpl(allocRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
            ledgerService = CustomerLedgerServiceImpl(ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo)

            val mockDb = MockIntegrationDb()
            val txManager = DefaultPostgresTransactionManager(mockDb)

            val customFactory = object : PostgresRepositoryFactory(txManager, tenantId) {
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
                jwtSigningSecret = "test_signing_secret_for_e2e_financial_test_2026",
                jwtIssuer = "sucharu-test",
                jwtAudience = "sucharu-api"
            )
            jwtTokenProvider = JwtTokenProvider(authConfig)
            securityContext = BackendSecurityContext(jwtTokenProvider)

            adminToken = jwtTokenProvider.generateAccessToken(
                principal = AuthenticatedPrincipal(
                    userId = "USR-ADMIN-01",
                    projectId = tenantId,
                    username = "admin_user",
                    role = UserRole.ADMIN
                )
            )

            val healthChecker = DatabaseHealthChecker(mockDb)
            router = BackendRouter(securityContext, useCases, healthChecker)
        }
    }

    @Test
    fun testCompleteCommercialFinancialCycle_EndToEnd() = runBlocking {
        // 1. Create Draft Invoice ($1,000)
        val line = CustomerInvoiceLine(
            lineId = "LINE-E2E-01",
            invoiceId = "",
            tenantId = tenantId,
            projectId = projectId,
            description = "1000 Commercial Catalog Printing",
            quantity = BigDecimal("1000"),
            unitPrice = BigDecimal("1.00"),
            lineTotal = BigDecimal("1000.00")
        )

        val draftRes = invoiceService.createDraftInvoice(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = financialAccountId,
            sourceOrderId = "ORD-E2E-100",
            sourceJobId = "JOB-E2E-100",
            dueDate = System.currentTimeMillis() + 864000000L,
            currency = "BDT",
            lines = listOf(line),
            notes = "E2E Invoice Notes",
            actorId = "USR-ADMIN-01",
            actorRole = "ADMIN"
        )
        if (draftRes is DomainResult.Error) {
            fail("Draft creation failed: ${draftRes.message}")
        }
        val draft = (draftRes as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.DRAFT, draft.status)
        assertEquals(BigDecimal("1000.0000"), draft.grandTotal)

        // 2. Issue Invoice
        val issueRes = invoiceService.issueInvoice(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = draft.invoiceId,
            actorId = "USR-ADMIN-01",
            actorRole = "ADMIN",
            expectedVersion = 1L
        )
        if (issueRes is DomainResult.Error) {
            fail("Invoice issuance failed: ${issueRes.message}")
        }
        val issued = (issueRes as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.ISSUED, issued.status)
        assertEquals(BigDecimal("1000.0000"), issued.dueAmount)

        // 3. Record & Confirm Partial Payment ($600, unallocated)
        val payRes = paymentService.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = financialAccountId,
            invoiceId = null,
            amount = BigDecimal("600.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BKASH,
            paymentDate = System.currentTimeMillis(),
            referenceNumber = "BKASH-REF-001",
            externalReference = "EXT-REF-001",
            notes = "First partial payment",
            idempotencyKey = "idemp-pay-e2e-01",
            actorId = "USR-ADMIN-01",
            actorRole = "ADMIN"
        )
        if (payRes is DomainResult.Error) {
            fail("Payment recording failed: ${payRes.message}")
        }
        val payment1 = (payRes as DomainResult.Success).data

        val confirmPayRes = paymentService.confirmPayment(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = payment1.paymentId,
            actorId = "USR-ADMIN-01",
            actorRole = "ADMIN",
            expectedVersion = 1L
        )
        if (confirmPayRes is DomainResult.Error) {
            fail("Payment confirmation failed: ${confirmPayRes.message}")
        }

        // 4. Allocate Payment ($600) -> Invoice becomes PARTIALLY_PAID ($400 due)
        val allocRes = settlementService.allocatePayment(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = payment1.paymentId,
            invoiceId = issued.invoiceId,
            amount = BigDecimal("600.00"),
            idempotencyKey = "idemp-alloc-e2e-01",
            actorId = "USR-ADMIN-01",
            actorRole = "ADMIN"
        )
        if (allocRes is DomainResult.Error) {
            fail("Payment allocation failed: ${allocRes.message}")
        }

        val updatedInvoice1Res = invoiceService.getInvoiceById(tenantId, projectId, issued.invoiceId)
        assertTrue(updatedInvoice1Res is DomainResult.Success)
        val updatedInvoice1 = (updatedInvoice1Res as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.PARTIALLY_PAID, updatedInvoice1.status)
        assertEquals(BigDecimal("600.0000"), updatedInvoice1.paidAmount)
        assertEquals(BigDecimal("400.0000"), updatedInvoice1.dueAmount)

        // 5. Record & Confirm Second Payment ($400, unallocated) -> Invoice becomes PAID ($0 due)
        val payRes2 = paymentService.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = financialAccountId,
            invoiceId = null,
            amount = BigDecimal("400.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK,
            paymentDate = System.currentTimeMillis(),
            referenceNumber = "BANK-REF-002",
            externalReference = "EXT-REF-002",
            notes = "Final settlement payment",
            idempotencyKey = "idemp-pay-e2e-02",
            actorId = "USR-ADMIN-01",
            actorRole = "ADMIN"
        )
        assertTrue("Second payment recording must succeed", payRes2 is DomainResult.Success)
        val payment2 = (payRes2 as DomainResult.Success).data

        paymentService.confirmPayment(tenantId, projectId, payment2.paymentId, "USR-ADMIN-01", "ADMIN", 1L)

        val allocRes2 = settlementService.allocatePayment(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = payment2.paymentId,
            invoiceId = issued.invoiceId,
            amount = BigDecimal("400.00"),
            idempotencyKey = "idemp-alloc-e2e-02",
            actorId = "USR-ADMIN-01",
            actorRole = "ADMIN"
        )
        assertTrue("Second payment allocation must succeed", allocRes2 is DomainResult.Success)

        val finalInvoiceRes = invoiceService.getInvoiceById(tenantId, projectId, issued.invoiceId)
        assertTrue(finalInvoiceRes is DomainResult.Success)
        val finalInvoice = (finalInvoiceRes as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.PAID, finalInvoice.status)
        assertEquals(BigDecimal("1000.0000"), finalInvoice.paidAmount)
        assertEquals(BigDecimal("0.0000"), finalInvoice.dueAmount)

        // 6. Replay Check: Repeated allocation with same idempotency key returns original allocation
        val replayAllocRes = settlementService.allocatePayment(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = payment2.paymentId,
            invoiceId = issued.invoiceId,
            amount = BigDecimal("400.00"),
            idempotencyKey = "idemp-alloc-e2e-02",
            actorId = "USR-ADMIN-01",
            actorRole = "ADMIN"
        )
        assertTrue("Replay allocation must succeed safely", replayAllocRes is DomainResult.Success)
        val replayAlloc = (replayAllocRes as DomainResult.Success).data
        assertEquals((allocRes2 as DomainResult.Success).data.allocationId, replayAlloc.allocationId)
    }
}
