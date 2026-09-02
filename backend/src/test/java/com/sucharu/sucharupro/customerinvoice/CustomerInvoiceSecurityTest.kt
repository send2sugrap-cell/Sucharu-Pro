package com.sucharu.sucharupro.customerinvoice

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 02: RBAC & Customer Ownership Authorization Tests for Invoices.
 */
class CustomerInvoiceSecurityTest {

    private lateinit var useCases: BackendUseCases

    private val projectId = "PRJ-SEC-01"
    private val customerId1 = "CUS-SEC-01"
    private val accountId1 = "CFA-SEC-01"
    private val customerId2 = "CUS-SEC-02"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF,
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

    private lateinit var invoice1: CustomerInvoiceDto

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        val customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        val accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        val invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)

        val accountService = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)
        val invoiceService = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createCustomerRepository(tenantId: String) = customerRepo
            override fun createCustomerFinancialAccountRepository(tenantId: String) = accountRepo
            override fun createCustomerFinancialAccountService(tenantId: String) = accountService
            override fun createCustomerInvoiceRepository(tenantId: String) = invoiceRepo
            override fun createCustomerInvoiceService(tenantId: String) = invoiceService
        }

        useCases = BackendUseCases(txManager, customFactory)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId1,
                    customerCode = "CUS-01",
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
                    accountNumber = "ACC-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            val lineReq = CustomerInvoiceLineRequest(
                description = "Printing Service",
                quantity = BigDecimal("500"),
                unitPrice = BigDecimal("2.00")
            )
            invoice1 = useCases.createCustomerInvoice(
                staffPrincipal,
                CreateCustomerInvoiceRequest(
                    customerId = customerId1,
                    customerFinancialAccountId = accountId1,
                    lines = listOf(lineReq)
                )
            )
        }
    }

    @Test
    fun testCustomerCanAccessOwnInvoice() = runBlocking {
        val invoice = useCases.getCustomerInvoice(customerPrincipal1, invoice1.invoiceId)
        assertEquals(invoice1.invoiceId, invoice.invoiceId)

        val customerInvoices = useCases.getCustomerInvoicesForCustomer(customerPrincipal1, customerId1)
        assertEquals(1, customerInvoices.size)
    }

    @Test
    fun testCustomerCannotAccessAnotherCustomerInvoice() = runBlocking {
        try {
            useCases.getCustomerInvoice(customerPrincipal2, invoice1.invoiceId)
            fail("Must block customer accessing another customer's invoice")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }

        try {
            useCases.getCustomerInvoicesForCustomer(customerPrincipal2, customerId1)
            fail("Must block querying another customer's invoices")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testVendorCannotAccessCustomerInvoices() = runBlocking {
        try {
            useCases.getCustomerInvoice(vendorPrincipal, invoice1.invoiceId)
            fail("Vendor must be blocked from customer financial documents")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testCustomerCannotIssueInvoice() = runBlocking {
        try {
            useCases.issueCustomerInvoice(
                customerPrincipal1,
                invoice1.invoiceId,
                IssueCustomerInvoiceRequest(expectedVersion = 1L)
            )
            fail("Customer role cannot issue invoices")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }
}
