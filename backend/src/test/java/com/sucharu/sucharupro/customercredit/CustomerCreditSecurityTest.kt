package com.sucharu.sucharupro.customercredit

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 04: RBAC & Customer Ownership Authorization Tests for Customer Credits.
 */
class CustomerCreditSecurityTest {

    private lateinit var useCases: BackendUseCases

    private val projectId = "PRJ-CR-SEC-01"
    private val customerId1 = "CUS-CR-SEC-01"
    private val accountId1 = "CFA-CR-SEC-01"
    private val customerId2 = "CUS-CR-SEC-02"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF,
        permissions = setOf(UserPermission.MANAGE_CUSTOMERS, UserPermission.MANAGE_FINANCE)
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "manager_01",
        projectId = projectId,
        username = "manager_user",
        role = UserRole.MANAGER,
        permissions = setOf(UserPermission.MANAGE_CUSTOMERS, UserPermission.MANAGE_FINANCE)
    )

    private val customerPrincipal1 = AuthenticatedPrincipal(
        userId = "client_01",
        projectId = projectId,
        username = "client_user_1",
        role = UserRole.CUSTOMER,
        customerId = customerId1
    )

    private val customerPrincipal2 = AuthenticatedPrincipal(
        userId = "client_02",
        projectId = projectId,
        username = "client_user_2",
        role = UserRole.CUSTOMER,
        customerId = customerId2
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor_01",
        projectId = projectId,
        username = "vendor_user",
        role = UserRole.VENDOR,
        vendorId = "VND-001"
    )

    private lateinit var advance1: CustomerAdvanceDto

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

        val accountService = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)
        val invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)
        val paymentService = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)
        val creditService = CustomerCreditServiceImpl(creditRepo, accountRepo, invoiceRepo, customerRepo, paymentRepo)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createCustomerRepository(tenantId: String) = customerRepo
            override fun createCustomerFinancialAccountRepository(tenantId: String) = accountRepo
            override fun createCustomerFinancialAccountService(tenantId: String) = accountService
            override fun createCustomerInvoiceRepository(tenantId: String) = invoiceRepo
            override fun createCustomerInvoiceService(tenantId: String) = invoiceService
            override fun createCustomerPaymentRepository(tenantId: String) = paymentRepo
            override fun createCustomerPaymentService(tenantId: String) = paymentService
            override fun createCustomerCreditRepository(tenantId: String) = creditRepo
            override fun createCustomerCreditService(tenantId: String) = creditService
        }

        useCases = BackendUseCases(txManager, customFactory)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId1,
                    customerCode = "CUS-CR-01",
                    displayName = "Customer 1",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000001",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountId1,
                    tenantId = projectId,
                    projectId = projectId,
                    customerId = customerId1,
                    accountNumber = "ACC-CR-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            advance1 = useCases.recordCustomerAdvance(
                staffPrincipal,
                RecordCustomerAdvanceRequest(
                    customerId = customerId1,
                    customerFinancialAccountId = accountId1,
                    amount = BigDecimal("2000.00"),
                    paymentMethod = "CASH"
                )
            )
        }
    }

    @Test
    fun testCustomerCanAccessOwnCreditSummary() = runBlocking {
        val summary = useCases.getCustomerCreditSummary(customerPrincipal1, customerId1)
        assertEquals(customerId1, summary.customerId)
        assertEquals(BigDecimal("2000.0000"), summary.totalAvailableCredit)
    }

    @Test
    fun testCustomerCannotAccessAnotherCustomerCreditSummary() = runBlocking {
        try {
            useCases.getCustomerCreditSummary(customerPrincipal2, customerId1)
            fail("Must block customer accessing another customer's credit summary")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testVendorCannotAccessCustomerCredits() = runBlocking {
        try {
            useCases.getCustomerCreditSummary(vendorPrincipal, customerId1)
            fail("Vendor must be blocked from customer credit data")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testCustomerCannotRecordAdvanceDirectly() = runBlocking {
        try {
            useCases.recordCustomerAdvance(
                customerPrincipal1,
                RecordCustomerAdvanceRequest(
                    customerId = customerId1,
                    customerFinancialAccountId = accountId1,
                    amount = BigDecimal("1000.00")
                )
            )
            fail("Customer role cannot record advances directly without staff")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testStaffCannotApproveRefundWithoutManagerRole() = runBlocking {
        // Staff requests refund
        val refund = useCases.requestCustomerRefund(
            staffPrincipal,
            RequestCustomerRefundRequest(
                customerId = customerId1,
                customerFinancialAccountId = accountId1,
                advanceId = advance1.advanceId,
                amount = BigDecimal("500.00"),
                reason = "Refund partial advance"
            )
        )

        // Staff tries to approve refund
        try {
            useCases.approveCustomerRefund(
                staffPrincipal,
                refund.refundId,
                ApproveCustomerRefundRequest(expectedVersion = refund.version)
            )
            fail("Staff cannot approve refunds; requires Manager/Admin")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }

        // Manager approves refund successfully
        val approved = useCases.approveCustomerRefund(
            managerPrincipal,
            refund.refundId,
            ApproveCustomerRefundRequest(expectedVersion = refund.version)
        )
        assertEquals("APPROVED", approved.status)
    }
}
