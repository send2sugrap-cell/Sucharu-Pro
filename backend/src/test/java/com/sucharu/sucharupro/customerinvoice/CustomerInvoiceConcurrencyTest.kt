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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 02: Concurrency & Optimistic Locking Tests for Invoices.
 */
class CustomerInvoiceConcurrencyTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var service: CustomerInvoiceServiceImpl

    private val tenantId = "TENANT-CONCURRENT"
    private val projectId = "PRJ-CONCURRENT"
    private val customerId = "CUS-CONCURRENT-01"
    private val accountId = "CFA-CONCURRENT-01"

    private lateinit var draftInvoiceId: String

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
                    customerId = customerId,
                    customerCode = "CUS-CONC-01",
                    displayName = "Concurrent Client",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801733333333",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountId,
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    accountNumber = "ACC-CONC-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            val line = CustomerInvoiceLine(
                lineId = "", invoiceId = "", tenantId = "", projectId = "",
                description = "Printing Items", quantity = BigDecimal("100"), unitPrice = BigDecimal("10")
            )
            val draft = (service.createDraftInvoice(
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                currency = "BDT",
                lines = listOf(line),
                actorId = "staff_1",
                actorRole = "STAFF"
            ) as DomainResult.Success).data
            draftInvoiceId = draft.invoiceId
        }
    }

    @Test
    fun testConcurrentSimultaneousIssuanceHasExactlyOneSuccess() = runBlocking {
        val attempts = 10
        val results = coroutineScope {
            (1..attempts).map { index ->
                async {
                    service.issueInvoice(
                        tenantId = tenantId,
                        projectId = projectId,
                        invoiceId = draftInvoiceId,
                        actorId = "user_$index",
                        actorRole = "STAFF",
                        expectedVersion = 1L // All competing with version 1
                    )
                }
            }.awaitAll()
        }

        val successes = results.filterIsInstance<DomainResult.Success<*>>()
        val errors = results.filterIsInstance<DomainResult.Error>()

        assertEquals("Exactly one concurrent issue request must succeed", 1, successes.size)
        assertEquals("All other concurrent issue requests must fail with version conflict", attempts - 1, errors.size)
    }
}
