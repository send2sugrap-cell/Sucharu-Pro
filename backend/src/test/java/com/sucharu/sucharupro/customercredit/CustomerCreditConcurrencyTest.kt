package com.sucharu.sucharupro.customercredit

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditServiceImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditConcurrencyTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var service: CustomerCreditServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-CONCURRENCY-001"
    private val accountId = "CFA-CONCURRENCY-001"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)
        val paymentDs = FakeCustomerPaymentDataSource()
        paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)
        val creditDs = FakeCustomerCreditDataSource()
        creditRepo = CustomerCreditRepositoryImpl(creditDs)

        service = CustomerCreditServiceImpl(creditRepo, accountRepo, invoiceRepo, customerRepo, paymentRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-C-01",
                    displayName = "Concurrency Customer",
                    primaryPhone = "01700000000",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
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
                    accountNumber = "CFA-C-1001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            val line = CustomerInvoiceLine(
                lineId = "LINE-C1",
                invoiceId = "INV-C-001",
                tenantId = tenantId,
                projectId = projectId,
                description = "Printing Job",
                quantity = BigDecimal("100"),
                unitPrice = BigDecimal("10.0000"),
                lineTotal = BigDecimal("1000.0000")
            )
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-C-001",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-2026-C01",
                    lines = listOf(line),
                    subtotal = BigDecimal("1000.0000"),
                    grandTotal = BigDecimal("1000.0000"),
                    paidAmount = BigDecimal("0.0000"),
                    dueAmount = BigDecimal("1000.0000"),
                    status = CustomerInvoiceStatus.ISSUED,
                    version = 1L
                )
            )
        }
    }

    @Test
    fun testConcurrentAllocations_DoNotExceedAvailableAdvance() = runBlocking {
        // Record Advance of 1000
        val adv = (service.recordAdvance(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("1000.0000"),
            actorId = "staff1",
            actorRole = "STAFF"
        ) as DomainResult.Success).data

        // Launch 5 concurrent allocations of 300 each (Total attempted 1500 > 1000)
        val deferred = (1..5).map { i ->
            async {
                service.allocateCreditToInvoice(
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    invoiceId = "INV-C-001",
                    advanceId = adv.advanceId,
                    amount = BigDecimal("300.0000"),
                    actorId = "staff_$i",
                    actorRole = "STAFF"
                )
            }
        }

        val results = deferred.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertTrue("At least one allocation succeeded", successCount >= 1)
        assertTrue("At least one allocation was rejected to prevent overdraft", errorCount >= 1)

        // Verify total allocated <= 1000
        val advAfter = (creditRepo.getAdvanceById(tenantId, projectId, adv.advanceId) as DomainResult.Success).data
        assertTrue(advAfter.allocatedAmount <= BigDecimal("1000.0000"))
        assertTrue(advAfter.availableAmount >= BigDecimal.ZERO)
    }
}
