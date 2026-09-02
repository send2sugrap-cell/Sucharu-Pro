package com.sucharu.sucharupro.customerinvoice

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 02: Multi-Tenant, Multi-Project & Customer Isolation Tests for Invoices.
 */
class CustomerInvoiceIsolationTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var service: CustomerInvoiceServiceImpl

    private val tenantA = "TENANT-ALPHA"
    private val tenantB = "TENANT-BETA"

    private val projectA = "PRJ-ALPHA"
    private val projectB = "PRJ-BETA"

    private val customerA = "CUS-ALPHA-1"
    private val accountA = "CFA-ALPHA-1"

    private val customerB = "CUS-BETA-1"
    private val accountB = "CFA-BETA-1"

    private lateinit var invoiceAId: String

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)

        service = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerA,
                    customerCode = "CUS-A",
                    displayName = "Customer Alpha",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000001",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            customerRepo.addCustomer(
                Customer(
                    customerId = customerB,
                    customerCode = "CUS-B",
                    displayName = "Customer Beta",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000002",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountA,
                    tenantId = tenantA,
                    projectId = projectA,
                    customerId = customerA,
                    accountNumber = "ACC-A-1",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountB,
                    tenantId = tenantB,
                    projectId = projectB,
                    customerId = customerB,
                    accountNumber = "ACC-B-1",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            val line = CustomerInvoiceLine(
                lineId = "", invoiceId = "", tenantId = "", projectId = "",
                description = "Printing Job A", quantity = BigDecimal("10"), unitPrice = BigDecimal("100")
            )
            val draft = (service.createDraftInvoice(
                tenantId = tenantA,
                projectId = projectA,
                customerId = customerA,
                customerFinancialAccountId = accountA,
                currency = "BDT",
                lines = listOf(line),
                actorId = "staff_a",
                actorRole = "STAFF"
            ) as DomainResult.Success).data
            invoiceAId = draft.invoiceId
        }
    }

    @Test
    fun testTenantIsolationOnInvoiceLookup() = runBlocking {
        // Tenant B attempting to read Tenant A's invoice must fail
        val crossTenantLookup = service.getInvoiceById(tenantB, projectA, invoiceAId)
        assertTrue("Cross-tenant invoice lookup must fail", crossTenantLookup is DomainResult.Error)

        val tenantBList = service.listInvoices(tenantB, projectA)
        assertTrue(tenantBList is DomainResult.Success)
        assertTrue((tenantBList as DomainResult.Success).data.isEmpty())
    }

    @Test
    fun testProjectIsolationOnInvoiceLookup() = runBlocking {
        // Project B attempting to read Project A's invoice in Tenant A must fail
        val crossProjectLookup = service.getInvoiceById(tenantA, projectB, invoiceAId)
        assertTrue("Cross-project invoice lookup must fail", crossProjectLookup is DomainResult.Error)
    }
}
