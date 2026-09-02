package com.sucharu.sucharupro.customerpayment

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 03: Concurrency & Race Condition Tests for Payments.
 */
class CustomerPaymentConcurrencyTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var service: CustomerPaymentServiceImpl

    private val tenantId = "TENANT-CONCURRENT"
    private val projectId = "PRJ-CONCURRENT"
    private val customerId = "CUS-CONC-01"
    private val accountId = "CFA-CONC-01"

    private lateinit var invoiceId: String

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

        service = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)

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

            val invoice = CustomerInvoice(
                invoiceId = "INV-CONC-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-CONC-1001",
                grandTotal = BigDecimal("5000.0000"),
                paidAmount = BigDecimal.ZERO,
                dueAmount = BigDecimal("5000.0000"),
                status = CustomerInvoiceStatus.ISSUED,
                version = 1L
            )
            invoiceRepo.createInvoice(invoice)
            invoiceId = invoice.invoiceId
        }
    }

    @Test
    fun testConcurrentSimultaneousFullPaymentsPreventOverpayment() = runBlocking {
        val attempts = 8
        val results = coroutineScope {
            (1..attempts).map { index ->
                async {
                    service.recordPayment(
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = customerId,
                        customerFinancialAccountId = accountId,
                        invoiceId = invoiceId,
                        amount = BigDecimal("5000.00"),
                        currency = "BDT",
                        paymentMethod = CustomerPaymentMethod.CASH,
                        actorId = "cashier_$index",
                        actorRole = "STAFF"
                    )
                }
            }.awaitAll()
        }

        val successes = results.filterIsInstance<DomainResult.Success<*>>()
        val errors = results.filterIsInstance<DomainResult.Error>()

        assertEquals("Exactly one concurrent full payment must succeed", 1, successes.size)
        assertEquals("All other concurrent full payments must fail", attempts - 1, errors.size)

        // Verify invoice balance is exactly 5000 paid and 0 due (never overpaid!)
        val finalInv = (invoiceRepo.getInvoiceById(tenantId, projectId, invoiceId) as DomainResult.Success).data
        assertEquals(0, BigDecimal("5000.0000").compareTo(finalInv.paidAmount))
        assertEquals(0, BigDecimal.ZERO.compareTo(finalInv.dueAmount))
        assertEquals(CustomerInvoiceStatus.PAID, finalInv.status)
    }
}
